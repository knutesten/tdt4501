package no.ntnu.falldetection.models;

import java.util.ArrayList;


public class OrientationModel {
	private final float NOISE_THRESHOLD = 1f;
	
	private float pitch = 0;
	private float roll = 0;
	private float yaw = 0;
	
	private float lastPitch =0;
	private float lastRoll =0;
	private float lastYaw = 0;
	
	private ArrayList<OrientationListener> listeners = new ArrayList<OrientationListener>();
	
	public void setAngles(float[] angles){
		pitch = removeNoise(lastPitch, angles[0]);
		roll = removeNoise(lastRoll, angles[1]);
		yaw = removeNoise(lastYaw, angles[2]);
		
		OrientationEvent evt = new OrientationEvent(angles);
		for(OrientationListener listener : listeners){
			listener.orientationChanged(evt);
		}
	}
	
	private float removeNoise(float oldValue, float newValue){
		if(Math.abs(newValue - oldValue) < NOISE_THRESHOLD){
			return oldValue;
		}else{
			return newValue;
		}
	}
	
	public void addOrientationListener(OrientationListener listener){
		listeners.add(listener);
	}
	
	public void removeOrientationListener(OrientationListener listener){
		listeners.remove(listener);
	}
	
	public float[] getAngles(){
		return new float[]{pitch, roll, yaw};
	}
	
	public float getPitch(){
		return pitch;
	}
	
	public float getRoll(){
		return roll;
	}
	
	public float getYaw(){
		return yaw;
	}
}
