package no.ntnu.falldetection.utils.motejx.extensions.motionplus;

import no.ntnu.falldetection.libs.EventListenerList;
import no.ntnu.falldetection.utils.motej.android.AbstractExtension;
import no.ntnu.falldetection.utils.motej.android.Mote;
import no.ntnu.falldetection.utils.motej.android.event.DataEvent;
import no.ntnu.falldetection.utils.motej.android.event.DataListener;
import no.ntnu.falldetection.utils.motej.android.request.ReportModeRequest;

public class MotionPlus extends AbstractExtension implements DataListener {
	private Mote mote;

	private MotionPlusCalibrationData calibrationData;
	private MotionPlusCalibrate calibration = new MotionPlusCalibrate();
	
	private EventListenerList listenerList = new EventListenerList();

	// MotionPlus speed scaling
	private final double LOWSPEED_SCALING = 20.0; // RawValues / 20 =
													// x degree
	private final double HIGHSPEED_SCALING = 4.0; // RawValues / 4 =
													// x degree

	/**
	 * Initializes the Motion plus extension
	 */

	public void initialize() {
		// Add as listener for calibration data
		mote.addDataListener(this);

		// Initialize
		mote.writeRegisters(new byte[] { (byte) 0xa6, 0x00, (byte) 0xf0 },
				new byte[] { 0x55 });

		// Start motion plus without pass through extensions
		mote.writeRegisters(new byte[] { (byte) 0xA6, 0x00, (byte) 0xFE },
				new byte[] { 0x04 });

		// Request calibration data
		mote.readRegisters(new byte[] { (byte) 0xa4, 0x00, 0x20 }, new byte[] {
				0x00, (byte) 0x32 });
	}


	public void parseExtensionData(byte[] extensionData) {
		// decrypt(extensionData);
		fireGyroEvent(extensionData);
	}

	private void fireGyroEvent(byte[] extensionData) {
		if (calibrationData == null) {
			return;
		}

		GyroListener[] listeners = listenerList.getListeners(GyroListener.class);
		if (listeners.length == 0) {
			return;
		}

		boolean yawFast = ((extensionData[3] & 0x02) >> 1) == 0;
		boolean rollFast = ((extensionData[4] & 0x02) >> 1) == 0;
		boolean pitchFast = ((extensionData[3] & 0x01) >> 0) == 0;

		float yaw = ((extensionData[0] & 0xff) | ((extensionData[3] & 0xfc) << 6));
		float roll = ((extensionData[1] & 0xff) | ((extensionData[4] & 0xfc) << 6));
		float pitch = ((extensionData[2] & 0xff) | ((extensionData[5] & 0xfc) << 6));

		if (!calibration.isFinished()) {
			calibration.addCalibrationData(yaw, roll, pitch);
			if (calibration.isFinished()) {
				calibrationData = calibration.getCalibratedData();
			}
		}

		yaw -= calibrationData.getYaw0();
		roll -= calibrationData.getRoll0();
		pitch -= calibrationData.getPitch0();

		if (yawFast) {
			yaw /= HIGHSPEED_SCALING;
		} else {
			yaw /= LOWSPEED_SCALING;
		}

		if (rollFast) {
			roll /= HIGHSPEED_SCALING;
		} else {
			roll /= LOWSPEED_SCALING;
		}

		if (pitchFast) {
			pitch /= HIGHSPEED_SCALING;
		} else {
			pitch /= LOWSPEED_SCALING;
		}

		GyroEvent evt = new GyroEvent(this, yaw, roll, pitch);
		for (GyroListener l : listeners) {
			l.gyroChanged(evt);
		}
	}
	
	public void addGyroListener(GyroListener gyroListener){
		listenerList.add(GyroListener.class, gyroListener);
	}


	public void setMote(Mote mote) {
		this.mote = mote;
	}


	public void dataRead(DataEvent evt) {
		if (calibrationData == null && evt.getError() == 0
				&& evt.getAddress()[0] == 0x00
				&& (evt.getAddress()[1] & 0xff) == 0x20
				&& evt.getPayload().length == 16) {
			byte[] payload = evt.getPayload();

			calibrationData = new MotionPlusCalibrationData();

			// gyro calibration - seems to be OK but not very accurate
			calibrationData
					.setYaw0((int) (((payload[0] & 0xff) << 6) | ((payload[1] & 0xff) >> 2)));
			calibrationData
					.setRoll0((int) (((payload[2] & 0xff) << 6) | ((payload[3] & 0xff) >> 2)));
			calibrationData
					.setPitch0((int) (((payload[4] & 0xff) << 6) | ((payload[5] & 0xff) >> 2)));

			// this doesn't seem right...
			// YawG = (int)((buff[16] << 6) | buff[17] >> 2);
			// RollG = (int)((buff[18] << 6) | buff[19] >> 2);
			// PitchG = (int)((buff[20] << 6) | buff[21] >> 2);
			
		}

	}

	public void calibrate() {
		calibration.startCalibrating();
	}

	@Override
	public String toString() {
		return "Motion plus";
	}
}