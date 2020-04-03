package io.entropic.integration.examples.debezium.routes;

import org.apache.camel.Converter;
import org.apache.kafka.connect.data.Struct;

@Converter
public class EventConverter {

	@Converter
	public static TestEvent testEventFromStruct(Struct struct) throws Exception {
		return new TestEvent(struct.getInt32("id").toString(), struct.getString("eventname"), struct.getString("eventdescription"));
		
	}
}
