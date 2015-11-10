## Money: Distributed Tracing Made Simple
### Money makes it simple to trace across threads and systems
Money is a lightweight, modular distributed tracing platform that can be seamlessly incorporated into modern applications.

Money is built on Scala and Akka, to be non-blocking from the core. It is purposefully un-opinionated, keeping undesired pull-through dependencies to a minimum.

Money modules build on the core, so implementing tracing is a snap. From Apache Http Components to Spring 4, from thread pools to Scala Futures, Money has modules to support a wide range of architectures.

## I don't need no docs, gimme Money!

Add a dependency as follows for maven:

```xml
    <dependency>
        <groupId>com.comcast.money</groupId>
        <artifactId>money-core</artifactId>
        <version>0.8.8</version>
    </dependency>
``` 

## User Guide
[Money Documentation](https://github.com/Comcast/money/wiki)
