package no.ntnu.falldetection.activities;

import no.ntnu.falldetection.utils.ExternalStorage;
import no.ntnu.falldetection.utils.MadgwickAHRS;
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
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends Activity {

	private int REQUEST_ENABLE_BT;
	private ArrayAdapter<String> deviceArrayAdapter;
	private ListView adapterList;
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();
	private Mote mote;
	private ExternalStorage es = new ExternalStorage();

	private long start;
	private long time;

	private GyroEvent newGyroEvent = null;
	private AccelerometerEvent<Mote> newAccelEvent = null;

	// Variables for the Madgwick Algorithm

	private float[] quat;

	// Create a BroadcastReceiver for ACTION_FOUND
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// Add the name and address to an array adapter to show in a
				// ListView
				deviceArrayAdapter.add(device.getName() + "\n"
						+ device.getAddress());

				if (device.getName().startsWith("Nintendo")) {
					Log.e("HESTEMANN", "HERE GOES!");
					mBluetoothAdapter.cancelDiscovery();

					mote = new Mote(device);

					AccelerometerListener<Mote> listener = new AccelerometerListener<Mote>() {

						public void accelerometerChanged(
								AccelerometerEvent<Mote> evt) {
							// Log.i("accel", evt.getX() + " : " + evt.getY() +
							// " : " + evt.getZ());
							shit(null, evt);
						}

					};

					final GyroListener listener3 = new GyroListener() {

						public void gyroChanged(GyroEvent evt) {
							Log.i("gyro", evt.getYaw() + " " + evt.getRoll()
									+ " " + evt.getPitch());
							shit(evt, null);
						}

					};

					ExtensionListener listener2 = new ExtensionListener() {

						public void extensionConnected(ExtensionEvent evt) {
							((MotionPlus) mote.getExtension())
									.addGyroListener(listener3);

							try {
								Thread.sleep(1000);
							} catch (Exception e) {

							}
							mote.setReportMode(ReportModeRequest.DATA_REPORT_0x37);
							mote.setPlayerLeds(new boolean[] { true, true,
									false, false });
						}

						public void extensionDisconnected(ExtensionEvent evt) {
							// TODO Auto-generated method stub
						}
					};

					start = System.currentTimeMillis();
					MoteDisconnectedListener<Mote> listener4 = new MoteDisconnectedListener<Mote>() {

						public void moteDisconnected(
								MoteDisconnectedEvent<Mote> evt) {
							time = (System.currentTimeMillis() - start);
							Log.w("time", "" + time);
							es.writeToFile("Time: " + time);
						}

					};

					mote.addExtensionListener(listener2);
					mote.addAccelerometerListener(listener);
					mote.addMoteDisconnectedListener(listener4);

				}
			}
		}
	};

	public void shit(GyroEvent ge, AccelerometerEvent<Mote> ae) {
		if (ge != null) {
			newGyroEvent = ge;
		}
		if (ae != null) {
			newAccelEvent = ae;
		}

		if (newGyroEvent != null && newAccelEvent != null) {

			// float pitch = converter.convertTo(newGyroEvent.getPitch(),
			// newGyroEvent.getRoll(), newGyroEvent.getYaw(),
			// newAccelEvent.getX(), newAccelEvent.getY(),
			// newAccelEvent.getZ());
			//
			// Log.i("alg", "pitch: " + pitch);
			//

			// Log.i("alg", "pa: " + pitchAngle + " ya: " + yawAngle + " ra: " +
			// rollAngle);

			newGyroEvent = null;
			newAccelEvent = null;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		if (mBluetoothAdapter == null) {
//			Log.d("BLUETOOTH", "NO BLUETOOTH");
//		} else if (!mBluetoothAdapter.isEnabled()) {
//			Intent enableBtIntent = new Intent(
//					BluetoothAdapter.ACTION_REQUEST_ENABLE);
//			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//		}
//
//		// Register the BroadcastReceiver
//		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//		registerReceiver(mReceiver, filter); 
//		mBluetoothAdapter.startDiscovery();
		
		
		
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		es.writeToFile("Time: " + time);
		es.close();
		mote.disconnect();
		unregisterReceiver(mReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// deviceArrayAdapter.add("hest");
		switch (item.getItemId()) {
		case R.id.menu_settings:
			if (mote != null) {
				mote.readRegisters(
						new byte[] { (byte) 0xa6, 0x00, (byte) 0xfa },
						new byte[] { 0x00, 0x06 });
			}
			return true;
		case R.id.test:
			// mote.setReportMode(ReportModeRequest.DATA_REPORT_0x37);
			((MotionPlus) mote.getExtension()).calibrate();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// Hjelpemetode

}