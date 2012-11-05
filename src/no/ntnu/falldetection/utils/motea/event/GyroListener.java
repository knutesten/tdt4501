package no.ntnu.falldetection.utils.motea.event;

import java.util.EventListener;

public interface GyroListener extends EventListener{
	public void gyroChanged(GyroEvent evt);
}
