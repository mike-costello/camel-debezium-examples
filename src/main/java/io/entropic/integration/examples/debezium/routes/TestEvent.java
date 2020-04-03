package io.entropic.integration.examples.debezium.routes;

public class TestEvent implements java.io.Serializable{
	
	private static final long serialVersionUID = -5227324642752828525L;
	private String id; 
	private String eventDescription; 
	private String eventName;
	
	public TestEvent(String id, String eventDescription, String eventName) {
		super();
		this.id = id;
		this.eventDescription = eventDescription;
		this.eventName = eventName;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getEventDescription() {
		return eventDescription;
	}
	public void setEventDescription(String eventDescription) {
		this.eventDescription = eventDescription;
	}
	public String getEventName() {
		return eventName;
	}
	public void setEventName(String eventName) {
		this.eventName = eventName;
	} 

}
