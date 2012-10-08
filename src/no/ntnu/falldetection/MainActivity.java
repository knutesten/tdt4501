package no.ntnu.falldetection;


import motej.android.Mote;
import motej.android.event.AccelerometerEvent;
import motej.android.event.AccelerometerListener;
import motej.android.request.ReportModeRequest;
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
	private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	private ExternalStorage store;
	
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
					
					Mote mote = new Mote(device);
  
					AccelerometerListener<Mote> listener = new AccelerometerListener<Mote>() {
						
						public void accelerometerChanged(AccelerometerEvent<Mote> evt) {
							Log.i("Accelerometer", evt.getX() + " : " + evt.getY() + " : " + evt.getZ());
							store.writeToFile(evt.getX() + " : " + evt.getY() + " : " + evt.getZ());
						}
					
					};
					
					mote.addAccelerometerListener(listener);   
					
					mote.setReportMode(ReportModeRequest.DATA_REPORT_0x31);
				}
			}
		}   
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		adapterList = (ListView) findViewById(R.id.adapterList);
		deviceArrayAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1);
		adapterList.setAdapter(deviceArrayAdapter);

		if (mBluetoothAdapter == null) {
			Log.d("BLUETOOTH", "NO BLUETOOTH");
		} else if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter); // Don't forget to unregister
												// during onDestroy

		mBluetoothAdapter.startDiscovery();
		
		//Create application directory on SD card
		store = new ExternalStorage();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_settings:
			deviceArrayAdapter.add("hest");
			return true;
		case R.id.menu_exit:
			finish();
			System.exit(0);
		}
		
		
		return true;
	}
	 
	
	//This method is called when the system is shutting down

}