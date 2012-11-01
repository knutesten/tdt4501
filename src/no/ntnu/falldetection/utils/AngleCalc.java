package no.ntnu.falldetection.utils;

import android.util.Log;


public class AngleCalc implements SensorListener{

	private MadgwickAHRS alg;

	public AngleCalc() {
		alg = new MadgwickAHRS(1f / 100f, 0.5f);
	}

	public float[] calculateAngle(float wiiPitch, float wiiRoll, float wiiYaw,
			float accelX, float accelY, float accelZ) {

		alg.update(degToRad(wiiPitch), degToRad(wiiRoll), degToRad(wiiYaw),
				accelX, accelY, accelZ);

		float[] quat = alg.getQuaternion();

		double pitch = Math.atan2(2 * (quat[0] * quat[1] + quat[2] * quat[3]),
				1 - 2 * (quat[1] * quat[1] + quat[2] * quat[2]));
		double roll = Math.asin(2 * (quat[0] * quat[2] - quat[3] * quat[1]));
		double yaw = Math.atan2(2 * (quat[0] * quat[3] + quat[1] * quat[2]),
				1 - 2 * (quat[2] * quat[2] + quat[3] * quat[3]));

		float pitchAngle = (float) (pitch * (180 / Math.PI));
		float rollAngle = (float) (roll * (180 / Math.PI));
		float yawAngle = (float) (yaw * (180 / Math.PI));

		float[] angles = new float[] { pitchAngle, rollAngle, yawAngle };

		return angles;
	}

	private float degToRad(float degrees) {
		return (float) (Math.PI / 180) * degrees;
	}

	@Override
	public void newSensorData(SensorEvent evt) {
		float[] angles = calculateAngle(evt.getPitch(), evt.getRoll(), evt.getYaw(), evt.getAccelX(), evt.getAccelY(), evt.getAccelZ());
		
		Log.i("hestemann", angles[0] + " " + angles[1] + " " + angles[2]);
	}
}
