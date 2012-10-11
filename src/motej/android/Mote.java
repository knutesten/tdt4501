/*
 * Copyright 2007-2008 Volker Fritzsch
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package motej.android;

import motej.android.event.AccelerometerEvent;
import motej.android.event.AccelerometerListener;
import motej.android.event.CoreButtonEvent;
import motej.android.event.CoreButtonListener;
import motej.android.event.DataEvent;
import motej.android.event.DataListener;
import motej.android.event.ExtensionEvent;
import motej.android.event.ExtensionListener;
import motej.android.event.IrCameraEvent;
import motej.android.event.IrCameraListener;
import motej.android.event.MoteDisconnectedEvent;
import motej.android.event.MoteDisconnectedListener;
import motej.android.event.StatusInformationListener;
import motej.android.request.CalibrationDataRequest;
import motej.android.request.PlayerLedRequest;
import motej.android.request.RawByteRequest;
import motej.android.request.ReadRegisterRequest;
import motej.android.request.ReportModeRequest;
import motej.android.request.RumbleRequest;
import motej.android.request.StatusInformationRequest;
import motej.android.request.WriteRegisterRequest;
import test.EventListenerList;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * 
 * <p>
 * 
 * @author <a href="mailto:vfritzsch@users.sourceforge.net">Volker Fritzsch</a>
 */
public class Mote {

	private OutgoingThread outgoing;

	private IncomingThread incoming;

	private ExtensionProvider extensionProvider = new ExtensionProvider();

	private Extension currentExtension;

	private StatusInformationReport statusInformationReport;

	private CalibrationDataReport calibrationDataReport;

	private EventListenerList listenerList = new EventListenerList();

	private BluetoothDevice device;

	public Mote(BluetoothDevice device) {
		try {
			this.device = device;
			// I'm interested if one of the Thread is disconnected
			addMoteDisconnectedListener(new MoteDisconnectedListener<Mote>() {
				public void moteDisconnected(MoteDisconnectedEvent<Mote> evt) {
					// Something goes wrong with one of my Thread
					// Try to properly cleanup everything
					disconnect();
				} 
			});
			
			outgoing = new OutgoingThread(this, device);
			incoming = new IncomingThread(this, device);

			incoming.start();
			outgoing.start();

			outgoing.sendRequest(new StatusInformationRequest());
			outgoing.sendRequest(new CalibrationDataRequest());
		} catch (Exception ex) {
			throw new RuntimeException(ex.fillInStackTrace());
		}
	}

	public void addAccelerometerListener(AccelerometerListener<Mote> listener) {
		listenerList.add(AccelerometerListener.class, listener);
	}

	public void addCoreButtonListener(CoreButtonListener listener) {
		listenerList.add(CoreButtonListener.class, listener);
	}

	public void addDataListener(DataListener listener) {
		listenerList.add(DataListener.class, listener);
	}

	public void addExtensionListener(ExtensionListener listener) {
		listenerList.add(ExtensionListener.class, listener);
	}

	public void addIrCameraListener(IrCameraListener listener) {
		listenerList.add(IrCameraListener.class, listener);
	}

	public void addMoteDisconnectedListener(
			MoteDisconnectedListener<Mote> listener) {
		listenerList.add(MoteDisconnectedListener.class, listener);
	}

	public void addStatusInformationListener(StatusInformationListener listener) {
		listenerList.add(StatusInformationListener.class, listener);
	}

	public void disableIrCamera() {
		// 1. Disable IR Camera
		outgoing.sendRequest(new RawByteRequest(new byte[] { 82, 19, 0 }));

		// 2. Disable IR Camera 2
		outgoing.sendRequest(new RawByteRequest(new byte[] { 82, 26, 0 }));
	}

	public void disconnect() {
		Log.i("motej.android", "Disconnecting mote " + device);

		if (outgoing != null) {
			outgoing.disconnect();
			try {
				outgoing.join(5000l);
			} catch (InterruptedException ex) {
				Log.e("motej.android", ex.getMessage() + ": " + ex.getStackTrace());
			}
		}
		if (incoming != null) {
			incoming.disconnect();
			try {
				incoming.join(5000l);
			} catch (InterruptedException ex) {
				Log.e("motej.android",
						ex.getMessage() + ": " + ex.getStackTrace());
			}
		}
	}

	/**
	 * Enables the IR Camera in basic mode with Marcan sensitivity.
	 */
	public void enableIrCamera() {
		enableIrCamera(IrCameraMode.BASIC, IrCameraSensitivity.WII_LEVEL_3);
	}

	// public void enableIrCamera(IrCameraMode mode, IrCameraSensitivity
	// sensitivity) {
	// // 1. Enable IR Camera (Send 0x04 to Output Report 0x13)
	// outgoing.sendRequest(new RawByteRequest(new byte[] { 82, 19, 4 }));
	//
	// // 2. Enable IR Camera 2 (Send 0x04 to Output Report 0x1a)
	// outgoing.sendRequest(new RawByteRequest(new byte[] { 82, 26, 4 }));
	//
	// // 3. Write 0x08 to register 0xb00030
	// outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
	// 0x00, 0x30 }, new byte[] { 0x08 }));
	//
	// // 4. Write Sensitivity Block 1 to registers at 0xb00000
	// outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
	// 0x00, 0x00 }, sensitivity.block1()));
	//
	// // 5. Write Sensitivity Block 2 to registers at 0xb0001a
	// outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
	// 0x00, 0x1a }, sensitivity.block2()));
	//
	// // 6. Write Mode Number to register 0xb00033
	// outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
	// 0x00, 0x33 }, new byte[] { mode.modeAsByte() }));
	// }

	public void enableIrCamera(IrCameraMode mode,
			IrCameraSensitivity sensitivity) {
		// 1. Enable IR Pixel Clock (Send 0x06 to Output Report 0x13)
		outgoing.sendRequest(new RawByteRequest(new byte[] { 82, 0x13, 0x06 }));

		// 2. Enable IR Logic (Send 0x06 to Output Report 0x1a)
		outgoing.sendRequest(new RawByteRequest(new byte[] { 82, 0x1a, 0x06 }));

		// 3. Write 0x01 to register 0xb00030
		outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
				0x00, 0x30 }, new byte[] { 0x01 }));

		// 4. Write Sensitivity Block 1 to registers at 0xb00000
		outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
				0x00, 0x00 }, sensitivity.block1()));

		// 5. Write Sensitivity Block 2 to registers at 0xb0001a
		outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
				0x00, 0x1a }, sensitivity.block2()));

		// 6. Write Mode Number to register 0xb00033
		outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
				0x00, 0x33 }, new byte[] { mode.modeAsByte() }));

		// 7. Write 0x08 to register 0xb00030
		outgoing.sendRequest(new WriteRegisterRequest(new byte[] { (byte) 0xb0,
				0x00, 0x30 }, new byte[] { 0x08 }));
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Mote))
			return false;

		return hashCode() == obj.hashCode();
	}

	@SuppressWarnings("unchecked")
	protected void fireMoteDisconnectedEvent() {
		MoteDisconnectedListener<Mote>[] listeners = listenerList
				.getListeners(MoteDisconnectedListener.class);
		MoteDisconnectedEvent<Mote> evt = new MoteDisconnectedEvent<Mote>(this);
		for (MoteDisconnectedListener<Mote> l : listeners) {
			l.moteDisconnected(evt);
		}
	}

	@SuppressWarnings("unchecked")
	protected void fireAccelerometerEvent(int x, int y, int z) {
		AccelerometerListener<Mote>[] listeners = listenerList
				.getListeners(AccelerometerListener.class);
		AccelerometerEvent<Mote> evt = new AccelerometerEvent<Mote>(this, x, y,
				z);
		for (AccelerometerListener<Mote> l : listeners) {
			l.accelerometerChanged(evt);
		}
	}

	protected void fireCoreButtonEvent(int modifiers) {
		CoreButtonListener[] listeners = listenerList
				.getListeners(CoreButtonListener.class);
		CoreButtonEvent evt = new CoreButtonEvent(this, modifiers);
		for (CoreButtonListener l : listeners) {
			l.buttonPressed(evt);
		}
	}

	protected void fireExtensionConnectedEvent() {
		ExtensionListener[] listeners = listenerList
				.getListeners(ExtensionListener.class);
		ExtensionEvent evt = new ExtensionEvent(this, currentExtension);
		for (ExtensionListener l : listeners) {
			l.extensionConnected(evt);
		}
	}

	protected void fireExtensionDisconnectedEvent() {
		ExtensionListener[] listeners = listenerList
				.getListeners(ExtensionListener.class);
		ExtensionEvent evt = new ExtensionEvent(this);
		for (ExtensionListener l : listeners) {
			l.extensionDisconnected(evt);
		}
	}

	protected void fireIrCameraEvent(IrCameraMode mode, IrPoint p0, IrPoint p1,
			IrPoint p2, IrPoint p3) {
		IrCameraListener[] listeners = listenerList
				.getListeners(IrCameraListener.class);
		IrCameraEvent evt = new IrCameraEvent(this, mode, p0, p1, p2, p3);
		for (IrCameraListener l : listeners) {
			l.irImageChanged(evt);
		}
	}

	protected void fireReadDataEvent(byte[] address, byte[] payload, int error) {
		String debug = "Address: 0x";
		for(byte a : address){
			debug += String.format("%02X", a);
		}
		debug += " Payload: 0x";
		for(byte p : payload)
			debug += String.format("%02X", p);

		Log.d("HYPERDEBUG", debug);
		byte[] buff = payload;
		
		if (calibrationDataReport == null && error == 0 && address[0] == 0x00
				&& address[1] == 0x20) {
			// calibration data (most probably)
			Log.d("motej.android", "Received Calibration Data Report.");
			CalibrationDataReport report = new CalibrationDataReport(
					payload[0] & 0xff, payload[1] & 0xff, payload[2] & 0xff,
					payload[4] & 0xff, payload[5] & 0xff, payload[6] & 0xff);
			calibrationDataReport = report;
		}
		
		int motionplus = 0;
		if(error == 0 && address[0]== 0 && (address[1] & 0xff) == 0xfa){
			motionplus = ((int)buff[0] << 40) | ((int)buff[1] << 32) | ((int)buff[2]) << 24 | ((int)buff[3]) << 16 | ((int)buff[4]) << 8 | buff[5];
		}
		
		if (currentExtension == null && ((error == 0 && address[0] == 0x00
				&& (address[1] & 0xff) == 0xfe && payload.length == 2) || (motionplus & 0xffffffff)==0xa6200005)) {
			// extension ID (most probably)
			String id0 = Integer.toHexString(payload[0] & 0xff);
			String id1 = Integer.toHexString(payload[1] & 0xff);
			Log.d("motej.android", "Received Extension ID: "
					+ (id0.length() == 1 ? "0x0" + id0 : "0x" + id0) + " "
					+ (id1.length() == 1 ? "0x0" + id1 : "0x" + id1));

			if ((payload[0] & 0xff) == 0xff && (payload[1] & 0xff) == 0xff) {
				Log.d("motej.android", "Connection not completed, re-requesting extension id.");
				outgoing.sendRequest(new ReadRegisterRequest(new byte[] {
						(byte) 0xa4, 0x00, (byte) 0xfe }, new byte[] { 0x00,
						0x02 }));
			} else {

				currentExtension = extensionProvider.getExtension(payload);
					Log.i("motej.android", "Found extension: " + currentExtension);
				if (currentExtension != null) {
					currentExtension.setMote(this);
					currentExtension.initialize();
					incoming.setExtension(currentExtension);
					fireExtensionConnectedEvent();
				}
			}
		}

		DataListener[] listeners = listenerList
				.getListeners(DataListener.class);
		DataEvent evt = new DataEvent(address, payload, error);
		for (DataListener l : listeners) {
			l.dataRead(evt);
		}
	}

	protected void fireStatusInformationChangedEvent(
			StatusInformationReport report) {
		// decide if we should query the extension port
		boolean extensionChanged;
		if (statusInformationReport == null) {
			extensionChanged = report.isExtensionControllerConnected();
		} else {
			extensionChanged = statusInformationReport
					.isExtensionControllerConnected() != report
					.isExtensionControllerConnected();
		}

		statusInformationReport = report;
		StatusInformationListener[] listeners = listenerList
				.getListeners(StatusInformationListener.class);
		for (StatusInformationListener l : listeners) {
			l.statusInformationReceived(report);
		}

		if (extensionChanged) {
			if (!report.isExtensionControllerConnected()) {
				currentExtension = null;
				fireExtensionDisconnectedEvent();
			} else {
				// 1. initialize peripheral (writing zero to 0xa40040)
				outgoing.sendRequest(new WriteRegisterRequest(new byte[] {
						(byte) 0xa4, 0x00, 0x40 }, new byte[] { 0x00 }));

				// 2. read extension ID bytes from wii register (0xa400fe)
				outgoing.sendRequest(new ReadRegisterRequest(new byte[] {
						(byte) 0xa4, 0x00, (byte) 0xfe }, new byte[] { 0x00,
						0x02 }));
			}
		}
	}

	public BluetoothDevice getDevice() {
		return device;
	}

	public CalibrationDataReport getCalibrationDataReport() {
		return calibrationDataReport;
	}

	public StatusInformationReport getStatusInformationReport() {
		return statusInformationReport;
	}

	@Override
	public int hashCode() {
		return device.hashCode();
	}

	public void removeAccelerometerListener(AccelerometerListener<Mote> listener) {
		listenerList.remove(AccelerometerListener.class, listener);
	}

	public void removeCoreButtonListener(CoreButtonListener listener) {
		listenerList.remove(CoreButtonListener.class, listener);
	}

	public void removeDataListener(DataListener listener) {
		listenerList.remove(DataListener.class, listener);
	}

	public void removeExtensionListener(ExtensionListener listener) {
		listenerList.remove(ExtensionListener.class, listener);
	}

	public void removeIrCameraListener(IrCameraListener listener) {
		listenerList.remove(IrCameraListener.class, listener);
	}

	public void remoteMoteDisconnectedListener(
			MoteDisconnectedListener<Mote> listener) {
		listenerList.remove(MoteDisconnectedListener.class, listener);
	}

	public void removeStatusInformationListener(
			StatusInformationListener listener) {
		listenerList.remove(StatusInformationListener.class, listener);
	}

	public void requestStatusInformation() {
		outgoing.sendRequest(new StatusInformationRequest());
	}

	public void rumble(long millis) {
		outgoing.sendRequest(new RumbleRequest(millis));
	}

	public void setPlayerLeds(boolean[] leds) {
		outgoing.sendRequest(new PlayerLedRequest(leds));
	}

	public void setReportMode(byte mode) {
		outgoing.sendRequest(new ReportModeRequest(mode));
	}

	public void setReportMode(byte mode, boolean continuous) {
		outgoing.sendRequest(new ReportModeRequest(mode, continuous));
	}

	public void readRegisters(byte[] offset, byte[] size) {
		outgoing.sendRequest(new ReadRegisterRequest(offset, size));
	}

	public void writeRegisters(byte[] offset, byte[] payload) {
		outgoing.sendRequest(new WriteRegisterRequest(offset, payload));
	}

	@SuppressWarnings("unchecked")
	public <T extends Extension> T getExtension() {
		return (T) currentExtension;
	}

	@Override
	public String toString() {
		return "Mote[" + device + "]";
	}
}
