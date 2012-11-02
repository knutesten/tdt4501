package no.ntnu.falldetection.controllers;

import java.util.ArrayList;

import no.ntnu.falldetection.utils.SensorEvent;
import no.ntnu.falldetection.utils.SensorListener;
import no.ntnu.falldetection.utils.motej.android.Mote;
import no.ntnu.falldetection.utils.motej.android.StatusInformationReport;
import no.ntnu.falldetection.utils.motej.android.event.AccelerometerEvent;
import no.ntnu.falldetection.utils.motej.android.event.AccelerometerListener;
import no.ntnu.falldetection.utils.motej.android.event.ExtensionEvent;
import no.ntnu.falldetection.utils.motej.android.event.ExtensionListener;
import no.ntnu.falldetection.utils.motej.android.event.MoteDisconnectedEvent;
import no.ntnu.falldetection.utils.motej.android.event.MoteDisconnectedListener;
import no.ntnu.falldetection.utils.motej.android.event.StatusInformationListener;
import no.ntnu.falldetection.utils.motejx.extensions.motionplus.GyroEvent;
import no.ntnu.falldetection.utils.motejx.extensions.motionplus.GyroListener;
import no.ntnu.falldetection.utils.motejx.extensions.motionplus.MotionPlus;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

public class WiiMoteHandler implements AccelerometerListener<Mote>,
		GyroListener, MoteDisconnectedListener<Mote>, ExtensionListener,
		StatusInformationListener {
	private Mote mote;
	private MotionPlus extension;
	private GyroEvent newGyroEvent = null;
	private AccelerometerEvent<Mote> newAccelEvent = null;
	private ArrayList<SensorListener> listeners = new ArrayList<SensorListener>();
	private boolean calibrated = false;
	private int batteryLevel = -1;

	public WiiMoteHandler(BluetoothDevice device) {
		this.mote = new Mote(device);
		mote.addAccelerometerListener(this);
		mote.addMoteDisconnectedListener(this);
		mote.addExtensionListener(this);
		mote.addStatusInformationListener(this);
	}

	public void addSensorListener(SensorListener listener) {
		listeners.add(listener);
	}

	public void removeSensorListener(SensorListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void gyroChanged(GyroEvent evt) {
		// Log.i("gyro", evt.getYaw() + " " + evt.getRoll() + " " +
		// evt.getPitch());
		fireSensorEvent(evt);
	}

	@Override
	public void accelerometerChanged(AccelerometerEvent<Mote> evt) {
		// Log.i("accel", evt.getX() + " : " + evt.getY() + " : " + evt.getZ());
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
		extension = null;
	}

	@Override
	public void moteDisconnected(MoteDisconnectedEvent<Mote> evt) {
		// TODO notify activity
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
			if (calibrated) {
				calibrated = false;
			}
			for (SensorListener listener : listeners) {
				listener.newSensorData(orientationEvent);
			}

			newAccelEvent = null;
			newGyroEvent = null;
		}
	}

	public void disconnect() {
		if (mote != null) {
			mote.disconnect();
		}
	}

	public boolean isConnected() {
		return mote != null;
	}

	public void rumble() {
		new Thread() {
			public void run() {
				while (true) {
					try {
						mote.rumble(100);
						Thread.sleep(200);
					} catch (Exception e) {

					}
				}
			}
		}.start();
	}

	@Override
	public void statusInformationReceived(StatusInformationReport report) {
		float battery = (float)(report.getBatteryLevel() & 0xff) / 0xc0;
		battery = battery/0.25f;
		
		int newBatteryLevel = Math.round(battery);
		
		if (batteryLevel != newBatteryLevel) {
			batteryLevel = newBatteryLevel;
			boolean[] leds = new boolean[4];
			for (int i = 0; i < leds.length; i++) {
				if (i < batteryLevel) {
					leds[i] = true;
				} else {
					leds[i] = false;
				}
			}
			mote.setPlayerLeds(leds);
		}
	}
}
