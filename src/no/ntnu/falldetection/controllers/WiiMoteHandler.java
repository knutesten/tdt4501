package no.ntnu.falldetection.controllers;

import java.util.ArrayList;

import no.ntnu.falldetection.utils.SensorEvent;
import no.ntnu.falldetection.utils.SensorListener;
import no.ntnu.falldetection.utils.motea.Mote;
import no.ntnu.falldetection.utils.motea.StatusInformationReport;
import no.ntnu.falldetection.utils.motea.event.AccelerometerEvent;
import no.ntnu.falldetection.utils.motea.event.AccelerometerListener;
import no.ntnu.falldetection.utils.motea.event.GyroEvent;
import no.ntnu.falldetection.utils.motea.event.GyroListener;
import no.ntnu.falldetection.utils.motea.event.MoteDisconnectedEvent;
import no.ntnu.falldetection.utils.motea.event.MoteDisconnectedListener;
import no.ntnu.falldetection.utils.motea.event.StatusInformationListener;
import android.bluetooth.BluetoothDevice;

public class WiiMoteHandler implements AccelerometerListener<Mote>,
		GyroListener, MoteDisconnectedListener<Mote>, 
		StatusInformationListener {
	private Mote mote;
	private GyroEvent newGyroEvent = null;
	private AccelerometerEvent<Mote> newAccelEvent = null;
	private ArrayList<SensorListener> listeners = new ArrayList<SensorListener>();
	private boolean calibrated = false;
	private int batteryLevel = -1;

	public WiiMoteHandler(BluetoothDevice device) {
		this.mote = new Mote(device);
		mote.addAccelerometerListener(this);
		mote.addMoteDisconnectedListener(this);
		mote.addStatusInformationListener(this);
		mote.addGyroListener(this);
	}

	public void addSensorListener(SensorListener listener) {
		listeners.add(listener);
	}

	public void removeSensorListener(SensorListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void gyroChanged(GyroEvent evt) {
		fireSensorEvent(evt);
	}

	@Override
	public void accelerometerChanged(AccelerometerEvent<Mote> evt) {
		fireSensorEvent(evt);
	}

	@Override
	public void moteDisconnected(MoteDisconnectedEvent<Mote> evt) {
		// TODO notify activity
	}

	public void calibrateMotionPlus() {
		calibrated = true;
		mote.calibrateMotionPlus();
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
						mote.rumble(mote.isRumbling()?false:true);
						Thread.sleep(200);
					} catch (Exception e) {

					}
				}
			}
		}.start();
	}

	@Override
	public void statusInformationReceived(StatusInformationReport report) {
		float battery = (float)(report.getBatteryLevel() & 0xff) / 0xc8;		
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
