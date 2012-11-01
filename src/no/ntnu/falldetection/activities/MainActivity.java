package no.ntnu.falldetection.activities;

import no.ntnu.falldetection.controllers.AngleCalc;
import no.ntnu.falldetection.controllers.WiiMoteHandler;
import no.ntnu.falldetection.models.OrientationModel;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	private int REQUEST_ENABLE_BT;
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
			.getDefaultAdapter();

	private Handler handler = new Handler();
	private WiiMoteHandler wiiMoteHandler;
	
	private Button connectButton;
	private Button calibrateButton;


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

				if (device.getName().startsWith("Nintendo")) {
					mBluetoothAdapter.cancelDiscovery();
					connectToWiiMote(device);
				}
			}
		}
	};
	
	private void connectToWiiMote(BluetoothDevice device){
		//Create hander for wii mote
		wiiMoteHandler = new WiiMoteHandler(device);
		
		//Create orientation model for the connected wii mote
		OrientationModel model = new OrientationModel();
		
		//Create angleCalc to convert the values from the sensors to angles
		AngleCalc angleCalc = new AngleCalc(model);
		wiiMoteHandler.addSensorListener(angleCalc);
		
		//Let the view listen to changes made to the model
		CubeView cubeView = (CubeView)findViewById(R.id.cubeView);
		model.addOrientationListener(cubeView);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (mBluetoothAdapter == null) {
			Log.d("BLUETOOTH", "NO BLUETOOTH");
		} else if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter);

		connectButton = (Button) findViewById(R.id.buttonConnect);
		calibrateButton = (Button) findViewById(R.id.buttonCalibrate);

		connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mBluetoothAdapter.startDiscovery();
				connectButton.setEnabled(false);

				new Thread() {
					String[] connectingAnimation = new String[] {
							"Connecting.", "Connecting..", "Connecting..." };
					int counter = 0;

					public void run() {
						setButtonText(connectingAnimation[counter++]);
						try {
							Thread.sleep(1000l);
						} catch (Exception e) {
						}
						while (mBluetoothAdapter.isDiscovering()) {
							setButtonText(connectingAnimation[counter++
									% connectingAnimation.length]);
							try {
								Thread.sleep(1000l);
							} catch (Exception e) {
							}
						}
						if (wiiMoteHandler!= null && wiiMoteHandler.isConnected()) {
							setButtonText("Connected");
						} else {
							setButtonEnabled(true);
							setButtonText("Connect");
						}
					}

					private void setButtonText(String text) {
						final String finText = text;
						handler.post(new Runnable() {
							@Override
							public void run() {
								connectButton.setText(finText);
							}
						});
					}
					
					private void setButtonEnabled(boolean on){
						final boolean bool = on;
						handler.post(new Runnable() {
							@Override
							public void run() {
								connectButton.setEnabled(bool);
							}
						});
					}
				}.start();
			}
		});

		calibrateButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				wiiMoteHandler.calibrateMotionPlus();
			}
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		wiiMoteHandler.disconnect();
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
			if (wiiMoteHandler != null) {
				wiiMoteHandler.searchForMotionPlus();
			}
			return true;
		case R.id.test:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}