package no.ntnu.falldetection.controllers;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;

import no.ntnu.falldetection.activities.CubeView;
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
	
	private CubeView cubeView;
	
	private long start;

	public WiiMoteHandler(BluetoothDevice device, CubeView cubeView) {
		this.mote = new Mote(device);
		start = System.currentTimeMillis();
		mote.addAccelerometerListener(this);
		mote.addMoteDisconnectedListener(this);
		mote.addExtensionListener(this);
		mote.addStatusInformationListener(this);
	
	
		this.cubeView = cubeView;
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
		printTime();
	}

	private void printTime() {
		long time = System.currentTimeMillis() - start;
		time/=1000;
		int hours = (int)time/3600;
		int minutes = (int)(time/60)%60;
		int seconds = (int)(time)%60;
		
		String s = hours+":";
		s+=(minutes<10?"0"+minutes:minutes) + ":";
		s+=seconds<10?"0"+seconds:seconds +"";
		
		
		cubeView.setText(s);
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
		mote.rumble(Long.MAX_VALUE);
	}

	@Override
	public void statusInformationReceived(StatusInformationReport report) {
		printTime();
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
