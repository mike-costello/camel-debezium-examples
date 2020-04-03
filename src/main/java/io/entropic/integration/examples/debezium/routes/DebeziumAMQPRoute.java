package io.entropic.integration.examples.debezium.routes;

import java.io.IOException;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanOperation;


public class DebeziumAMQPRoute extends RouteBuilder{

	@Override
	public void configure() throws Exception {
		
		from("debezium-postgres:localhost?"
                + "databaseHostname={{database.hostname}}"
                + "&databasePort={{database.port}}"
                + "&databaseUser={{database.user}}"
                + "&databasePassword={{database.password}}"
                + "&databaseDbname=postgres"
                + "&databaseServerName=localhost"
                + "&pluginName=pgoutput"
                + "&schemaWhitelist={{database.schema}}"
                + "&tableWhitelist={{database.schema}}.making_tests"
                + "&offsetStorage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore")
                .routeId("debeziumPGRoute")
                .routeDescription("This route  consumes from a PostGres DB and persists an AMQP Event")
                .log(LoggingLevel.INFO, "Incoming message ${body} with headers ${headers}")
                .convertBodyTo(TestEvent.class)
                .setHeader("CamelInfinispanKey", simple("${body.id}"))
                .setHeader("CamelInfinispanValue", simple("${body}"))
                .setHeader("CamelInfinispanOperation", simple(InfinispanOperation.PUTIFABSENT.toString()))
                .setHeader("eventPayload", simple("${body}"))
                .to("infinispan:pg.event")
                .setBody(simple("${header.eventPayload}"))
                .doTry()
                	.to("amqp:{{database.schema}}.dbevents?connectionFactory=connFact")
                .doCatch(IOException.class)
                	.log("** Exception thrown: ${exception.message} **")
                	.log("Body of the message: ${body}")
                	.throwException(new Exception("${exception.message}"))
                .endDoTry();
		
				from("amqp:{{database.schema}}.dbevents?connectionFactory=connFact")
					.routeId("amqpConsumerRoute")
					.routeDescription("Very simple route that consumes the amqp event messsage and simply logs it out")
					.log("** Received Message **: ${body}");
    }
}
