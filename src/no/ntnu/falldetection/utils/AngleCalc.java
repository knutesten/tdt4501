package no.ntnu.falldetection.utils;

import no.ntnu.falldetection.models.OrientationModel;


public class AngleCalc implements SensorListener{

	private MadgwickAHRS alg;
	private OrientationModel model;

	public AngleCalc(OrientationModel model) {
		this.model = model;
		alg = new MadgwickAHRS(1f / 100f, 0.5f);
	}

	public float[] calculateAngle(float wiiPitch, float wiiRoll, float wiiYaw,
			float accelX, float accelY, float accelZ) {

		alg.update(degToRad(wiiPitch), degToRad(wiiRoll), degToRad(wiiYaw),
				accelX, accelY, accelZ);

		float[] q = alg.getQuaternion();

		double pitch = Math.atan2(2 * (q[0] * q[1] + q[2] * q[3]),
				1 - 2 * (q[1] * q[1] + q[2] * q[2]));
		double roll = Math.asin(2 * (q[0] * q[2] - q[3] * q[1]));
		double yaw = Math.atan2(2 * (q[0] * q[3] + q[1] * q[2]),
				1 - 2 * (q[2] * q[2] + q[3] * q[3]));

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
		if(evt.wasCalibrated()){
			alg.reset();
		}
		float[] angles = calculateAngle(evt.getPitch(), evt.getRoll(), evt.getYaw(), evt.getAccelX(), evt.getAccelY(), evt.getAccelZ());
		model.setAngles(angles);
	}
}
