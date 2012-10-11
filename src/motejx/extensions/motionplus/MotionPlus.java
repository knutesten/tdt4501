package motejx.extensions.motionplus;

import android.util.Log;
import motej.android.AbstractExtension;
import motej.android.Mote;
import motej.android.event.DataEvent;
import motej.android.event.DataListener;
import motej.android.request.ReportModeRequest;

public class MotionPlus extends AbstractExtension implements DataListener {
	private Mote mote;

	private MotionPlusCalibrationData calibrationData;

	// MotionPlus speed scaling
	private final double MOTIONPLUS_LOWSPEED_SCALING = 20.0; // RawValues / 20 =
																// x degree
	private final double MOTIONPLUS_HIGHSPEED_SCALING = 4.0; // RawValues / 4 =
																// x degree

	/**
	 * Initializes the Motion plus extension
	 */
	@Override
	public void initialize() {

//		 Initialize
		mote.writeRegisters(new byte[] { (byte) 0xa6, 0x00, (byte) 0xf0 },
				new byte[] { 0x55 });

		mote.writeRegisters(new byte[] { (byte)0xA6, 0x00, (byte)0xFE}, new byte[]{ 0x04});
	}

	@Override
	public void parseExtensionData(byte[] extensionData) {
//		decrypt(extensionData);
		fireGyroEvent(extensionData);
	}
	
	private void fireGyroEvent(byte[] extensionData){
		
		String debug = "";
		for(byte b : extensionData){
			debug += String.format("%02X", b);
		}
		Log.d("extensionData", debug);
		
		
		boolean yawFast = ((extensionData[3] & 0x02) >> 1) == 0;
		boolean rollFast = ((extensionData[4] & 0x02) >> 1) == 0;
		boolean pitchFast = ((extensionData[3] & 0x01) >> 0) == 0;
		
		int yaw = (extensionData[0] | (extensionData[3] & 0xfc) << 6);
		int roll = (extensionData[1] | (extensionData[4] & 0xfc) << 6);
		int pitch = (extensionData[2] | (extensionData[5] & 0xfc) << 6);
		
		Log.d("gyro", yaw + " " + roll + " " + pitch + " " + yawFast + " " + rollFast + " " + pitchFast);
	}

	@Override
	public void setMote(Mote mote) {
		this.mote = mote;
	}

	@Override
	public void dataRead(DataEvent evt) {
		Log.d("calibration", "event received");
		if (calibrationData == null && evt.getError() == 0
				&& evt.getAddress()[0] == 0x00
				&& (evt.getAddress()[1] & 0xff) == 0x30
				&& evt.getPayload().length == 0x0f) {
			
			byte[] payload = evt.getPayload();
			
			// gyro calibration - seems to be OK but not very accurate
			calibrationData.setYaw0((int)((payload[0] << 6) | (payload[1] >> 2)));
			calibrationData.setRoll0((int)((payload[2] << 6) | (payload[3] >> 2)));
			calibrationData.setPitch0((int)((payload[4] << 6) | (payload[5] >> 2)));
			
			// this doesn't seem right...
            //YawG = (int)((buff[16] << 6) | buff[17] >> 2);
            //RollG = (int)((buff[18] << 6) | buff[19] >> 2);
            //PitchG = (int)((buff[20] << 6) | buff[21] >> 2);
		}

	}
	
	@Override
	public String toString(){
		return "Motion plus";
	}
}
