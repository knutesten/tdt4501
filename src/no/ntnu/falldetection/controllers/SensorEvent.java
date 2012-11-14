package no.ntnu.falldetection.controllers;

public class SensorEvent {
	private float yaw;
	private float pitch;
	private float roll;
	
	private float accelX;
	private float accelY;
	private float accelZ;
	
	public SensorEvent(float yaw, float pitch, float roll, float accelX, float accelY, float accelZ){
		this.accelX = accelX;
		this.accelY = accelY;
		this.accelZ = accelZ;
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
	}

	public float getYaw() {
		return yaw;
	}
	public float getPitch() {
		return pitch;
	}
	public float getRoll() {
		return roll;
	}
	public float getAccelX() {
		return accelX;
	}
	public float getAccelY() {
		return accelY;
	}
	public float getAccelZ() {
		return accelZ;
	}
}
