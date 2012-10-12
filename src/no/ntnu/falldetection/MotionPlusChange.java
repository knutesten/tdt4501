package no.ntnu.falldetection;


public class MotionPlusChange {

	//These values are just temporary and will be subject to change
	private  MadgwickAHRS alg = new MadgwickAHRS(1f/100f, 0.5f);
	
	private float wiiYaw;
	private float wiiRoll;
	private float wiiPitch;
	private float accelX;
	private float accelY;
	private float accelZ;
	private float[] quat;
	private double pitch;
	private double roll;
	private double yaw;
	private double angle;
	
	//Should be excectuted when a change in Wii mote occurs
	
//	wiiYaw = calibratedYaw from mote
//	wiiRoll = calibratedRoll from mote
//	wiiPitch = clibratedPitch from mote
	
//	alg.update(degToRad(wiiPitch), degToRad(wiiRoll), degToRad(wiiYaw), accelX, accelY, accelZ);
	

	quat = alg.getQuaternion();
	
	pitch = Math.Atan2(2 * (quat[1] * quat[2] * quat[3] * quat[4]), 1 - 2 *  (quat[1] * quiat[1] + quat[2] * quat[2]));
	roll = Math.Asin(2 * (quat[0] * quat[2] - quat[3] * quat[1]));
	yaw = Math.Atan2(2 * (quat[0] * quat[3] + quat[1] * quat[2]), 1 - 2 * (quat[2] * quat[2] + quat[3] * quat[3]));
	
	angle = pitch * (180 / Math.PI);
	
	private float degToRad(float degrees){
	return (float) (Math.PI/189) * degrees;
	}
}
