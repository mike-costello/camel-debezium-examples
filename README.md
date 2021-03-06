# Emitting CDC Events as AMQP 1.0 Events 
Using Apache Camel, we will run Debezium embedded to perform a CDC operation against a PostGresQL DB and create an AMQP1.0 based event. 

The intent of this repo is to unwind CDC usage (and ultimately Debezium) from Kafka Connect as a platform. We assume the user has a: 
* PostGresQL DB Available on localhost
* An AMQP 1.0 Listener available on localhost 

Instead of using Debezium along with Kafka Connect, we use Debezium embedded as: 
* Kafka Connect is a monolithic platform that is heavyweight, and requires substantial tuning
* While Kafka has developed a fairly large Kafka Connect ecosystem: 
  * Camel has a far more robust set of adapters, component, and means to transform messages, etc. 
  * Much of Kafka Connect's ecosystem is developed under closed source (and proprietary) licesnse by Confluence (bad sauce)
  * Camel 3 and Camel-k advance capabilities for cloud native deployments (Kafka Connect is not capable of these cloud native capabilities) 

Our implementation creates a Debezium consumer of a PostGresQL DB commit logs. This consumer simply uses the PG Output capability of PostGres to poll the underlying table changes, and creates a Struct describing the table that has changed (whether it be an update, delete, etc. operation). This struct is then transformed using the Apache Camel type converter, persisted to an Infinispan store (as we likely need a persistent place to keep the message), and produces an AMQP event out of this data change that originated in PGSQL. 

If the AMQP 1.0 receiver has credit, and can auth/auz our Camel producer, we send the AMQP event off, and have a route that consumes that AMQP event to demonstrate the consumption of messages. 

## Why AMQP 1.0 as opposed to Kafka? Debezium typically uses Kafka, right? 
We've mentioned a few advantages of our approach above, but, perhaps there are some other advantages to decoupling Kafka from our event mesh: 

### Peer level Producer Flow Control  
Tthe greatest advantage of decoupling Kafka as a store from our event mesh, is that the Kafka wire protocol is immature in its capabilities. While we do have flow control provided us to by our Kafka brokers, this approach is ad hoc and happens after the fact. It is not until we have exhausted our in memory backing queue in the broker that we will have tripped thresholds for flow controlling producers. 

As a result, while emmitting our events directly to Kafka neatly couples our events to our store, and we leverage Kafka Connect to tail the head of commit logs in a DB that allows us to create a single topic producer, we have no way of decoupling a particular message producer from all message producers in the way that they are being flow controlled. As our number of producers grow, Kafka presents us little capability to dissagregate these stream producers from the overall broker capacity. 
 
AMQP 1.0 makes this capability a Layer 7 wire protocol capability between peers. For instance, if the AMQP event receiver that receives our AMQP event produced from this code sample is not capable of taking in messages, it will offer us 0 credit (AMQP 1.0 employs a credit cased flow control mechanism) and block our producers. This implies that an individual CDC emitter may be flow controlled by our AMQP event sink. 

### Event Mesh Multi-Tenancy
Additionally, Kafka has no real notion of multi-tenancy making it imperfect for use as an event bus. While we can certainly use keys for our message payloads that imply multi-tenancy in our broker, by leveraging an event mesh such as ***Qpid Dispatch Router*** [QPID Dispatch Router](https://qpid.apache.org/components/dispatch-router/index.html), we can create a truly multi-tenany event mesh. For more information on how to leverage a vhost so that policy may be applied in a multi-tenant fashion to our event mesh, please check out: [Configuring Authorization and Vhosts in QDR](https://qpid.apache.org/releases/qpid-dispatch-1.11.0/user-guide/index.html#configuring-authorization-qdr)

### Event Routing 
While there are certainly a number of ways to route Kafka producers/consumers using proxies, load balancers, LTM/GTM approaches, etc. and other machinations, event producers and consumers are coupled to the underlying Kafka wire protocol and inevitably Kafka broker. This coupling implies a request/response contract with the underlying broker, and bakes in Kafka specific needs to event producers. 

### Monolithic broker behaviour implies a useful persistence store but potentially inefficient event bus
Additionally, while Kafka clients can certainly be written to avail themselves of various QoS by employing various techniques, emitting events to Kafka as a central event bus implies that a client is bound to Kafka's In Sync Replica conditions. While this might work well in a QoS 0 type of situation where a client does not block and continues processing while spinning out a thread to handle the interaction with Kafka, disposition of the event by the event receiver is still bound by Kafka's broker level ISR configuration. In the case of many event types, this monolithic behaviour to determine disposition of the event emitted, is inappropriate for the event producers use case. 

## Extending this Code Impl 
Moving forward Camel is advantageous as it provides capabilities of extending this code implementation out to: 
* Leverage Quarkus as an AOT compiler for our Camel code. This implies lightning fast deployment times and runtime capabiltiies. For more information about Camel and Quarkus, please check out: [Camel and Quarkus](https://github.com/apache/camel-quarkus)
  * Leveraging Serverless Capabilities via Knative [Knative](https://knative.dev/)
  * Camel-K is a cloud native means of using Knative and Camel for serverless Camel capabilities: [Camel-K](https://github.com/apache/camel-k) 

## Running this code 

***Please note: the maven compile plugin implies use of a JDK 11 JVM***

This particular pom is compiled with and for Jdk 11. To downgrade this need simply change the maven compiler plugin in this projects pom. 

This application also assumes: 
* There is a postgresql db running and available 
* There is an amqp receiver running and available 

The code example uses a JVM as a runtime, and simply invokes Camel's main method via use of a java main. 

As a result, any number of means may be used as a runtime threshold for this code sample. For instance, the following may be run from the root of the project 
```
mvn exec:java
```
This will run the application as a java process in a traditional sense while packaging application dependencies, etc..  

For use of a single executable jar, the maven assembly or shade plugins may be used to run this application in a similar fashion.

## Quick Note on Production Readiness of this code sample 
***PLEASE NOTE: THIS IS NOT PRODUCTION READY*** 

This code snippet needs a good deal to be production ready. For instance:  
* Debezium should not use the memory backing store for offsets 
* We likely want a difference structure for our messages than a pgoutput struct or a POJO. It is more likley that we would to use json, protobuff or avro for binary payloads that we persist to our inevitable underlying store
* Infinispan is configured without persistence
* We would likely configure a more robust way to handle AMQP 1.0 event producers 
* The AMQP event consumer should not live in the same runtime as the producer 
* This route is not transactional nor does it define compensation behaviour
