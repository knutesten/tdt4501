package no.ntnu.falldetection.utils.motejx.extensions.motionplus;

public class MotionPlusCalibrationData {
	private float yaw0 = 0;
	
	private float Roll0 = 0;
	
	private float Pitch0 = 0;

	public float getPitch0() {
		return Pitch0;
	}

	public void setPitch0(float pitch0) {
		Pitch0 = pitch0;
	}

	public float getRoll0() {
		return Roll0;
	}

	public void setRoll0(float roll0) {
		Roll0 = roll0;
	}

	public float getYaw0() {
		return yaw0;
	}

	public void setYaw0(float yaw0) {
		this.yaw0 = yaw0;
	}
	
}
