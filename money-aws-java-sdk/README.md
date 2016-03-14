## Money: Amazon Web Service Client. 
### Amazon Web Service Client support for Money trace data.

Are you already using money to trace calls in your application? Would you like to do the same for your AWS calls so you can deep-link a user-request to a aws platform request in your logs? Then this money module is for you!

This module adds money trace data as a header to Amazon Web Service Client. The Amazon Web Service Client is used internally in the Amazon Web Service Java SDK to allow clients to talk to AWS platform.

## Show me how to do it, please!

Add a dependency as follows for maven:

```xml
   <dependency>
      <groupId>com.comcast.money</groupId>
      <artifactId>money-aws-java-sdk</artifactId>
      <version>0.8.14</version>
   </dependency>
   <dependency>
      <groupId>com.amazonaws</groupId>
      <artifactId>aws-java-sdk</artifactId>
      <version>1.10.57</version>
   </dependency>
```

And here is a dependency for SBT:

```scala
    resolvers += "CIM Nexus Repository" at "http://repo.dev.cim.comcast.net/nexus/content/repositories/releases"

    libraryDependencies += "com.comcast.money" %% "money-aws-java-sdk" % "0.8.14"
```

Then in your application, when you instantiate the AmazonDynamoDBClient, add one line of code:

```java
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setProtocol(Protocol.HTTPS);
        clientConfiguration.setMaxErrorRetry(1);
        this.client = new AmazonDynamoDBClient(new ClasspathPropertiesFileCredentialsProvider("PulsarCredentials.properties"),
                clientConfiguration);
        client.setEndpoint(pulsarUrl);
        client.addRequestHandler(new AmazonWebServiceClientMoneyTraceRequestHandler("myapp-pulsar"));
```

Done.

