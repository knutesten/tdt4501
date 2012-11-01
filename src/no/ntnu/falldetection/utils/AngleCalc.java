package no.ntnu.falldetection.utils;

import java.util.Currency;

public class AngleCalc {

	private MadgwickAHRS alg;
	private float[] quat;
	private float pitchAngle = Integer.MAX_VALUE;
	private float rollAngle = Integer.MAX_VALUE;
	private float yawAngle = Integer.MAX_VALUE;

	public AngleCalc() {
		alg = new MadgwickAHRS(1f / 100f, 0.5f);
	}

	public float[] convertTo(float wiiPitch, float wiiRoll, float wiiYaw,
			float accelX, float accelY, float accelZ) {

		alg.update(degToRad(wiiPitch), degToRad(wiiRoll), degToRad(wiiYaw),
				accelX, accelY, accelZ);

		quat = alg.getQuaternion();

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
}
