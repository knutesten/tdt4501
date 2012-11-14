package no.ntnu.falldetection.models;

import java.util.ArrayList;

public class ThresholdAlarm implements OrientationListener{
	private final int ALARM_THRESHOLD = 20;
	private ArrayList<AlarmListener> listenerList = new ArrayList<AlarmListener>();
	private boolean on = false;
	private float pitch0 = 0;
	private float roll0 = 0;
	private boolean calibrate = false;
	
	public ThresholdAlarm(OrientationModel model){
		model.addOrientationListener(this);
	}
	
	@Override
	public void orientationChanged(OrientationEvent evt) {
		if(calibrate){
			pitch0 = evt.getPitch();
			roll0 = evt.getRoll();
			calibrate = false;
		}
		
		double a = (evt.getPitch());
		double b = (evt.getRoll());
		a-= pitch0;
		b-= roll0;
		
		double angle = Math.pow(a*a + b*b, 0.5);
		
		if(brakesThreshold(angle)){
			on = true;
			fireAlarmOnEvent((float) angle);
		}else if(on){
			fireAlarmOffEvent();
		}
	}
	
	private void fireAlarmOffEvent() {
		for(AlarmListener l : listenerList){
			l.alarmOff();
		}
	}

	private boolean brakesThreshold(double angle){
		return angle > ALARM_THRESHOLD;
	}
	
	private void fireAlarmOnEvent(float angle){
		if(listenerList.isEmpty()){
			return;
		}
		float severity = Math.min(angle/90f, 1);
		AlarmEvent evt = new AlarmEvent(severity);
		
		for(AlarmListener l : listenerList){
			l.alarmOn(evt);
		}
	}
	
	public void addAlarmListener(AlarmListener listener){
		listenerList.add(listener);
	}
	
	public void removeAlarmListener(AlarmListener listener){
		listenerList.remove(listener);
	}
	
	public void calibrate(){
		calibrate = true;
	}
}
