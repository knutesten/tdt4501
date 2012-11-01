package no.ntnu.falldetection.models;

public class OrientationEvent {
	private float[] angles;
	
	public OrientationEvent(float[] angles){
		this.angles = angles;
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
