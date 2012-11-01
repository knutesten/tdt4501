package no.ntnu.falldetection.utils;

import java.util.EventListener;

public interface SensorListener extends EventListener{
	public void newSensorData(SensorEvent evt);
}
