package no.ntnu.falldetection.models;

public interface AlarmListener {
	public void alarmOn(AlarmEvent evt);
	
	public void alarmOff();
}
