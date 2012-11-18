package no.ntnu.falldetection.models;

import java.util.ArrayList;


public class OrientationModel {
	private ArrayList<OrientationListener> listeners = new ArrayList<OrientationListener>();
	
	public void setAngles(float[] angles){
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
}
