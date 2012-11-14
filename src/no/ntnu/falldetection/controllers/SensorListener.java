package no.ntnu.falldetection.controllers;

import java.util.EventListener;

public interface SensorListener extends EventListener{
	public void newSensorData(SensorEvent evt);
}
