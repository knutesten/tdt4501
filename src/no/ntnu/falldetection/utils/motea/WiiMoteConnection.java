package no.ntnu.falldetection.utils.motea;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import no.ntnu.falldetection.utils.motea.event.GyroEvent;
import no.ntnu.falldetection.utils.motea.extension.MotionPlus;
import no.ntnu.falldetection.utils.motea.request.MoteRequest;
import no.ntnu.falldetection.utils.motea.request.ReportModeRequest;
import no.ntnu.falldetection.utils.motea.request.StatusInformationRequest;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class WiiMoteConnection extends Thread{
	private InputStream incoming;
	private OutputStream outgoing;
	private Mote source;
	private boolean active;
	private boolean statusRequested = false;
	
	public WiiMoteConnection(BluetoothSocket socket, Mote source) throws IOException{
		this.source = source;
		active = true;
		incoming = socket.getInputStream();
		outgoing = socket.getOutputStream();
	}
	
	public void sendRequest(MoteRequest request){
		try {
			String s = "";
			for(byte b : request.getBytes()){
				if((b & 0xff) < 0x10){
					s+="0";
				}
				s+=Integer.toHexString(b & 0xff);
			}
			Log.d("motea", "Writing: "+s);

			if(request instanceof StatusInformationRequest){
				statusRequested = true;
			}
			outgoing.write(request.getBytes());
		} catch (IOException e) {
			Log.e("motea", "Connection lost.");
			active = false;
		} catch (NullPointerException e) {
			Log.e("motea", "Connection lost.");
			active = false;
		}
	}
	
	public void run() {
		byte[] buf = new byte[23];
		while (active) {
			try {
				incoming.read(buf);
								
				switch (buf[1]) {
				case ReportModeRequest.DATA_REPORT_0x20:
					if(!statusRequested){
						source.setReportMode();
					}
					statusRequested = false;
					parseStatusInformation(buf);
					break;

				case ReportModeRequest.DATA_REPORT_0x21:
					parseCoreButtonData(buf);
					parseMemoryData(buf);
					break;

				case ReportModeRequest.DATA_REPORT_0x22:
					break;

				case ReportModeRequest.DATA_REPORT_0x37:
					parseCoreButtonData(buf);
					parseAccelerometerData(buf);
					// parseBasicIrCameraData(buf, 7);
					parseExtensionData(buf, 17, 6);
					break;

				default:
					String hex = Integer.toHexString(buf[1] & 0xff);
					Log.d("motej.android",
							"Unknown or not yet implemented data report: "
									+ (hex.length() == 1 ? "0x0" + hex
											: "0x" + hex));
				}
			} catch (IOException ex) {
				Log.e("motej.android", "Connection lost.");
				active = false;
			} catch (NullPointerException ex) {
				Log.e("motej.android", "Connection lost.");
				active = false;
			}
		}

		try {
			Log.d("motej.android", "Connection closing.");
			if (incoming != null) {
				incoming.close();
			}
		} catch (IOException ex) {
			Log.e("motej.android",
					ex.getMessage() + ": " + ex.getStackTrace());
		}

		source.fireMoteDisconnectedEvent();
	}
	
	public void disconnect(){
		active = false;
	}
	
	protected void parseStatusInformation(byte[] bytes) {
		boolean[] leds = new boolean[] { (bytes[4] & 0x10) == 0x10,
				(bytes[4] & 0x20) == 0x20, (bytes[4] & 0x40) == 0x40,
				(bytes[4] & 0x80) == 0x80 };
		boolean extensionControllerConnected = (bytes[4] & 0x02) == 0x02;
		boolean speakerEnabled = (bytes[4] & 0x04) == 0x04;
		boolean continuousReportingEnabled = (bytes[4] & 0x08) == 0x08;
		byte batteryLevel = bytes[7];

		StatusInformationReport info = new StatusInformationReport(leds,
				speakerEnabled, continuousReportingEnabled,
				extensionControllerConnected, batteryLevel);
		source.fireStatusInformationChangedEvent(info);
	}

	protected void parseMemoryData(byte[] bytes) {
		int size = ((bytes[4] >> 4) & 0x0f) + 1;
		int error = bytes[4] & 0x0f;
		byte[] address = new byte[] { bytes[5], bytes[6] };
		byte[] payload = new byte[size];

		System.arraycopy(bytes, 7, payload, 0, size);

		source.fireReadDataEvent(address, payload, error);
	}

	protected void parseAccelerometerData(byte[] bytes) {
		// Get raw data
		float x = ((bytes[4] & 0xff) << 2) | ((bytes[2] & 0xff) >> 5 & 0x03);
		float y = ((bytes[5] & 0xff) << 2) | ((bytes[3] & 0xff) >> 5 & 0x02);
		float z = ((bytes[6] & 0xff) << 2) | ((bytes[3] & 0xff) >> 5 & 0x02);

		CalibrationDataReport c = source.getCalibrationDataReport();
		if (c == null) {
			return;
		}

		// Calculate calibrated accelerometer data
		x = (float) ((x - c.getZeroX()) / (c.getGravityX() - c.getZeroX()));
		y = (float) ((y - c.getZeroY()) / (c.getGravityY() - c.getZeroY()));
		z = (float) ((z - c.getZeroZ()) / (c.getGravityZ() - c.getZeroZ()));

		source.fireAccelerometerEvent(x, y, z);
	}

	protected void parseCoreButtonData(byte[] bytes) {
		int modifiers = bytes[2] & 0xff ^ (bytes[3] & 0xff) << 8;
		source.fireCoreButtonEvent(modifiers);
	}

	protected void parseExtensionData(byte[] bytes, int offset, int length) {
		Extension extension = source.getExtension();
		if (extension == null) {
			return;
		}

		byte[] extensionData = new byte[length];
		System.arraycopy(bytes, offset, extensionData, 0, length);
		Object evt = extension.parseExtensionData(extensionData);

		if (evt == null) {
			return;
		}

		if (extension instanceof MotionPlus) {
			source.fireGyroEvent((GyroEvent) evt);
		}
	}
	
}
	