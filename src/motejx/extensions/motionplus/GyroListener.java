package motejx.extensions.motionplus;

import java.util.EventListener;

public interface GyroListener extends EventListener{
	public void gyroChanged(GyroEvent evt);
}
