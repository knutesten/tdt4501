package motejx.extensions.motionplus;

public class GyroEvent {
	protected Object source;
	
	protected float yaw;
	
	protected float roll;
	
	protected float pitch;
	
	public GyroEvent(Object source, float yaw, float roll, float pitch){
		this.source = source;
		this.yaw = yaw;
		this.roll = roll;
		this.pitch = pitch;
	}

	public Object getSource() {
		return source;
	}

	public float getYaw() {
		return yaw;
	}

	public float getRoll() {
		return roll;
	}

	public float getPitch() {
		return pitch;
	}
}
