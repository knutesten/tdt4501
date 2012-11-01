package no.ntnu.falldetection.models;

import java.util.ArrayList;


public class OrientationModel {
	private float[] angles = new float[]{0f, 0f, 0f};
	private ArrayList<OrientationListener> listeners = new ArrayList<OrientationListener>();
	
	public void setAngles(float[] angles){
		this.angles = angles;
		
		OrientationEvent evt = new OrientationEvent(angles);
		for(OrientationListener listener : listeners){
			listener.orientationChanged(evt);
		}
	}
	
	public void addOrientationListener(OrientationListener listener){
		listeners.add(listener);
	}
	
	public void removeOrientationListener(OrientationListener listener){
		listeners.remove(listener);
	}
	
	public float[] getAngles(){
		return angles;
	}
	
	public float getPitch(){
		return angles[0];
	}
	
	public float getRoll(){
		return angles[1];
	}
	
	public float getYaw(){
		return angles[2];
	}
}
