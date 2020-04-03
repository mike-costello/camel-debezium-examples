package io.entropic.integration.examples.debezium;

import org.apache.camel.main.Main;
import org.apache.qpid.jms.JmsConnectionFactory;

import io.entropic.integration.examples.debezium.routes.DebeziumAMQPRoute;

public class Application {

	private static final Main main = new Main();

	public static void main(String[] args) throws Exception {
		JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory();
		try {
			jmsConnectionFactory.setRemoteURI("amqp://localhost:61616");
			jmsConnectionFactory.setUsername("admin");
			jmsConnectionFactory.setPassword("admin");
		} catch (Exception e) {
			throw new Exception(e);
		}
		main.setPropertyPlaceholderLocations("application.properties");
		main.addRouteBuilder(DebeziumAMQPRoute.class);
		main.bind("connFact", jmsConnectionFactory);
		
		main.run();
	}
}
