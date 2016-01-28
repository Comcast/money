## Development Status
We are actively working on version 0.9 that will offer several improvements over both the API and module 
implementations.  0.8.x is being used in several projects currently, and we will continue to enhance
0.8.x for the foreseeable future while 0.9 work takes place.

If you want to work on 0.9, feel free to reach out!

### Availability
We have requests in to deploy to a public repo, that should be available shortly

## Distributed Tracing Reads
[Distributed Trace For Video Systems](http://www.nctatechnicalpapers.com/Paper/2015/2015-distributed-trace-for-video-systems/download)
written by Michael Bevilacqua-Linn discusses our experiences at Comcast implementing distributed traces.

## Money: Distributed Tracing Made Simple
### Money makes it simple to trace across threads and systems
Money is a modular distributed tracing platform that can be seamlessly incorporated into modern applications.  *It's 
purpose is to provide a foundation for operational analytics through distributed tracing.*

Money is built on Scala and Akka, to be non-blocking from the core. It is purposefully un-opinionated, keeping undesired pull-through dependencies to a minimum.

Money modules build on the core, so implementing tracing is a snap. From Apache Http Components to Spring 4, from thread pools to Scala Futures, Money has modules to support a wide range of architectures.

## Why is it different?
Money was inspired by inspired by [Google Dapper](http://research.google.com/pubs/pub36356.html) 
and [Twitter Zipkin](http://twitter.github.io/zipkin/); however there are some subtle yet fundamental differences
between those systems and Money, the biggest one being...

### Spans start and end within a single process
In Dapper, a Span can encompass the communication between a client and a server.  Let's use an example of an 
*Order System* calling an *Inventory Management System*.  With Dapper, you could have the following:

- *span-name* - GetInventory
- *start-time*
- *client-send* - the time that the client sent the request
- *server-recv* - the time that the server received the request
- *server-send* - the time that the server responded to the request
- *client-recv* - the time that the client received the request
- *foo* - any arbitrary annotation (note) made by either the client or the server

The idea being that everything can be calculated when the data is at rest.

In Money, it is theoretically possible to do the same, but by default we always extend a span when the server 
starts processing.  We do this because we like to record important notes by default, namely the *span-duration* and 
the *span-success*.  Using the example above, with Money we would get:

- *app-name* - OrderSystem
- *span-name* - GetInventory
- *start-time* - time that the get inventory request started
- *span-duration* - duration in Microseconds the entire request took
- *span-success* - a boolean indicating success or failure of the operation from the Order System
- *host* - the machine name generating the data
- *foo* - any arbitrary bit of data

... and on the server we would get...

- *app-name* - InventoryManagement
- *span-name* - GetInventory
- *start-time* - time that the server began processing the request
- *span-duration* - how long it took for the server to process the request
- *span-success* - a success or failure indicator of the operation
- *host* - the machine name generating the data
- *bar* - any arbitrary bit of data

#### Why do that?
There are a tradeoffs with any decision.  

Here are some disadvantages:

- More data, we get twice as much data for the same RPC, this can be a problem for extremely large volume systems as much
more data will be generated
- Memory pressure - as we calculate duration and success, we need to maintain some state in process, which will add some memory overhead

Here are some advantages:

- You can use Money as a basis for operational analytics.  Money generates 3 important stats: latency, throughput and error rate.
We find that these are the most important stats in analyzing system performance and capacity.  You could calculate 
these at rest in Dapper; but we believe that these can be understood more quickly to support real-time monitoring
- You can very quickly integrate with monitoring tools.  As opposed to having to calculate metrics at rest in Dapper and Zipkin,
you can publish span data directly to Graphite (for instance), and create charts immediately.  This is a very strong advantage
to calculating metrics at the source.  Money actually comes with a *Graphite Emtitter* out-of-the box.  Given that 
money is pluggable, it is quite trivial to integrate with many other systems.
- In distributed system calls, it is important to know that the server "succeeded" or "failed", allowing you to more quickly 
focus your troubleshooting efforts when things go wrong.  For example, if the server succeeded, then we can direct our 
efforts toward the client or the network
- Less calculation is needed on the data - we have basic measures that we can use for calculations, allowing us
to process data and perform analytics faster

### We don't sample...yet
I can see rocks starting to fly here, and I understand.  Money was not built in its present incarnation to support
systems which generate 10s of millions of events per second.  Money was built to use distributed tracing as a foundation 
for operational analytics.  
  
We were much more interested in creating a standard set of metrics that we could use as a basis to perform operational analytics.
As such, for us, every event does matter as we can build aggregates very easy (even success).

We have been able to instrument systems that do generate many millions of events per hour with success, but Money did 
not have the same considerations that went into the Dapper design.  Being able to process our base metrics gets us 
closer to real-time understanding of processing; distributing the calculations to the origin systems gave us a lot of 
flexibility and speed in processing the data at rest.

> We are committed to support sampling and are evaluating designs...ideas are welcome.  Look for basic sampling to be added shortly

### We do not provide a backend like Zipkin
Zipkin comes with an entire infrastructure built around Scribe and Cassandra that actually allows you to see data.  This
is super cool, and something we aspire to complete.  We have looked at Spark Streaming and Akka Cluster Sharding as 
implementation mechanisms (and have some prototype / experimental code to that end), but we have not yet gotten our act
together.

One of the main advantages of Money is that it provides usable operational analytics out-of-the-box; whether 
reporting to Graphite, exposing data via JMX, and/or aggregating logs in Logstash.  As such, we have been able to gain
key insight into traces and performance using standard open source tools; here are some examples:

- Splunk / LogStash - we use log aggregators to perform metrics across systems at rest.  The nice thing about log 
aggregators is that you can not only see your metrics, but you can also look at all of the log entries that happend 
for a single Span
- Graphite / Prometheus - we use common analytic systems for monitoring and altering on Money data

## Should I use Money?
This depends on the scale of your implementation.  Money _tries_ to serve a wide range of implementations.

Certainly, if you want to implement an application that is serving 1000s or 10000s of request per second per JVM, Money 
should work for you.  You can easily funnel data into your log aggregator or other reporting system and start
getting the benefits immediately.

If your implementation is in the order of 50000+ RPS with lots of spans, then things will get difficult 
as you will have to manage a lot of data.  Spooling span events to disk and sending them as you can is one approach.
You can use FluentD, Heka, PipeD or something else to eventually get the data off of disk.  Theoretically it is possible,
but without sampling, Money is generating a ton of data.  If you are not using that data for analytics, you can 
filter it out (or contribute back a sampling feature); either way, it becomes a challenge.

## I don't need no docs, gimme Money!

Add a dependency as follows for maven:

```xml
    <dependency>
        <groupId>com.comcast.money</groupId>
        <artifactId>money-core</artifactId>
        <version>0.8.12</version>
    </dependency>
``` 

## User Guide
[Money Documentation](https://github.com/Comcast/money/wiki)
