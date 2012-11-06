package no.ntnu.falldetection.models;

public class AlarmEvent {
	private float severity;
	
	public AlarmEvent(float severity){
		this.severity = severity;
	}
	
	public float getSeverity(){
		return severity;
	}
}
