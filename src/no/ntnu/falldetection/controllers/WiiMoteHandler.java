package no.ntnu.falldetection.controllers;

import java.util.ArrayList;

import no.ntnu.falldetection.utils.SensorEvent;
import no.ntnu.falldetection.utils.SensorListener;
import no.ntnu.falldetection.utils.motej.android.Mote;
import no.ntnu.falldetection.utils.motej.android.event.AccelerometerEvent;
import no.ntnu.falldetection.utils.motej.android.event.AccelerometerListener;
import no.ntnu.falldetection.utils.motej.android.event.ExtensionEvent;
import no.ntnu.falldetection.utils.motej.android.event.ExtensionListener;
import no.ntnu.falldetection.utils.motej.android.event.MoteDisconnectedEvent;
import no.ntnu.falldetection.utils.motej.android.event.MoteDisconnectedListener;
import no.ntnu.falldetection.utils.motej.android.request.ReportModeRequest;
import no.ntnu.falldetection.utils.motejx.extensions.motionplus.GyroEvent;
import no.ntnu.falldetection.utils.motejx.extensions.motionplus.GyroListener;
import no.ntnu.falldetection.utils.motejx.extensions.motionplus.MotionPlus;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class WiiMoteHandler implements AccelerometerListener<Mote>,
		GyroListener, MoteDisconnectedListener<Mote>, ExtensionListener {
	private Mote mote;
	private MotionPlus extension;
	private GyroEvent newGyroEvent = null;
	private AccelerometerEvent<Mote> newAccelEvent = null;
	private ArrayList<SensorListener> listeners = new ArrayList<SensorListener>();
	private boolean calibrated = false;

	public WiiMoteHandler(BluetoothDevice device) {
		this.mote = new Mote(device);
		mote.addAccelerometerListener(this);
		mote.addMoteDisconnectedListener(this);
		mote.addExtensionListener(this);
	}

	public void addSensorListener(SensorListener listener) {
		listeners.add(listener);
	}

	public void removeSensorListener(SensorListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void gyroChanged(GyroEvent evt) {
//		Log.i("gyro", evt.getYaw() + " " + evt.getRoll() + " " + evt.getPitch());
		fireSensorEvent(evt);
	}

	@Override
	public void accelerometerChanged(AccelerometerEvent<Mote> evt) {
//		Log.i("accel", evt.getX() + " : " + evt.getY() + " : " + evt.getZ());
		fireSensorEvent(evt);
	}

	@Override
	public void extensionConnected(ExtensionEvent evt) {
		if (evt.isExtensionConnected()) {
			if (evt.getExtension() instanceof MotionPlus) {
				this.extension = (MotionPlus) evt.getExtension();
				extension.addGyroListener(this);

				mote.setPlayerLeds(new boolean[] { true, true, false, false });
			}
		}
	}

	@Override
	public void extensionDisconnected(ExtensionEvent evt) {
		// TODO fire error?
	}

	@Override
	public void moteDisconnected(MoteDisconnectedEvent<Mote> evt) {
		// TODO fire error?
	}

	public void calibrateMotionPlus() {
		if (extension != null) {
			extension.calibrate();
			calibrated = true;
		}
	}

	public void fireSensorEvent(Object evt) {
		if (evt instanceof GyroEvent) {
			newGyroEvent = (GyroEvent) evt;
		} else if (evt instanceof AccelerometerEvent) {
			newAccelEvent = (AccelerometerEvent<Mote>) evt;
		}

		if (newGyroEvent != null && newAccelEvent != null) {
			SensorEvent orientationEvent = new SensorEvent(
					newGyroEvent.getYaw(), newGyroEvent.getPitch(),
					newGyroEvent.getRoll(), newAccelEvent.getX(),
					newAccelEvent.getY(), newAccelEvent.getZ(), calibrated);
			if(calibrated){
				calibrated = false;
			}
			for (SensorListener listener : listeners) {
				listener.newSensorData(orientationEvent);
			}

			newAccelEvent = null;
			newGyroEvent = null;
		}
	}

	public void searchForMotionPlus() {
		mote.readRegisters(new byte[] { (byte) 0xa6, 0x00, (byte) 0xfa },
				new byte[] { 0x00, 0x06 });
	}

	public void disconnect() {
		if (mote != null) {
			mote.disconnect();
		}
	}
	
	public boolean isConnected(){
		return mote!=null;
	}
}
