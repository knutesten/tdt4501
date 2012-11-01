package no.ntnu.falldetection.models;

import java.util.EventListener;

public interface OrientationListener extends EventListener{
	public void orientationChanged(OrientationEvent evt);
}
