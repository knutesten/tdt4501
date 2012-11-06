package no.ntnu.falldetection.utils.motea;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import no.ntnu.falldetection.libs.EventListenerList;
import no.ntnu.falldetection.utils.motea.event.AccelerometerEvent;
import no.ntnu.falldetection.utils.motea.event.AccelerometerListener;
import no.ntnu.falldetection.utils.motea.event.CoreButtonEvent;
import no.ntnu.falldetection.utils.motea.event.CoreButtonListener;
import no.ntnu.falldetection.utils.motea.event.DataEvent;
import no.ntnu.falldetection.utils.motea.event.DataListener;
import no.ntnu.falldetection.utils.motea.event.GyroEvent;
import no.ntnu.falldetection.utils.motea.event.GyroListener;
import no.ntnu.falldetection.utils.motea.event.MoteDisconnectedEvent;
import no.ntnu.falldetection.utils.motea.event.MoteDisconnectedListener;
import no.ntnu.falldetection.utils.motea.event.StatusInformationListener;
import no.ntnu.falldetection.utils.motea.extension.MotionPlus;
import no.ntnu.falldetection.utils.motea.request.CalibrationDataRequest;
import no.ntnu.falldetection.utils.motea.request.PlayerLedRequest;
import no.ntnu.falldetection.utils.motea.request.ReadRegisterRequest;
import no.ntnu.falldetection.utils.motea.request.ReportModeRequest;
import no.ntnu.falldetection.utils.motea.request.StatusInformationRequest;
import no.ntnu.falldetection.utils.motea.request.WriteRegisterRequest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class Mote extends L2CAPConnectThread{
	private Timer timer = new Timer();
	private WiiMoteConnection connection;
	private static final byte PORT = 0x13;
	private CalibrationDataReport calibrationDataReport = null;
	private byte reportMode = (byte)0x37;
	private EventListenerList listenerList = new EventListenerList();
	private boolean rumble = false;
	private boolean[] leds = new boolean[]{true, true, true, true};
	private Extension extension = null;
	
	public Mote(BluetoothDevice device){
		super(device, PORT);
		this.start();
	}	
	
	@Override
	void manageConnectedSocket(BluetoothSocket socket) {
		try{
			connection = new WiiMoteConnection(socket, this);
			connection.start();
			initialize();
		}catch(IOException e){
			connectionFailure(e);
		}
	}
	
	private void initialize(){
		connection.sendRequest(new CalibrationDataRequest(rumble));
		setReportMode(reportMode);
		getStatus();
		timer.schedule(new CheckStatus(), 1000, 1000);
	}
	
	private class CheckStatus extends TimerTask {
		@Override
		public void run() {
			getStatus();

			// Check for MotionPlus
			if(extension == null){
				readRegisters(new byte[] { (byte) 0xa6, 0x00, (byte) 0xfa },
						new byte[] { 0x00, 0x06 });
			}
//			setReportMode(reportMode);
		}
	}
	
	private void getStatus() {
		connection.sendRequest(new StatusInformationRequest(rumble));
	}
	
	protected Extension getExtension(){
		return extension;
	}
	
	@Override
	void connectionFailure(IOException cause) {
		Log.e("motea", "Connection failure");
		fireMoteDisconnectedEvent();
	}
	
	public void addGyroListener(GyroListener listener){
		listenerList.add(GyroListener.class, listener);
	}
	
	public void addDataListener(DataListener listener) {
		listenerList.add(DataListener.class, listener);
	}
	
	public void addAccelerometerListener(AccelerometerListener<Mote> listener) {
		listenerList.add(AccelerometerListener.class, listener);
	}

	public void addCoreButtonListener(CoreButtonListener listener) {
		listenerList.add(CoreButtonListener.class, listener);
	}

	public void addMoteDisconnectedListener(
			MoteDisconnectedListener<Mote> listener) {
		listenerList.add(MoteDisconnectedListener.class, listener);
	}

	public void addStatusInformationListener(StatusInformationListener listener) {
		listenerList.add(StatusInformationListener.class, listener);
	}
	
	public void removeGyroListener(GyroListener listener){
		listenerList.remove(GyroListener.class, listener);
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

	public void remoteMoteDisconnectedListener(
			MoteDisconnectedListener<Mote> listener) {
		listenerList.remove(MoteDisconnectedListener.class, listener);
	}

	public void removeStatusInformationListener(
			StatusInformationListener listener) {
		listenerList.remove(StatusInformationListener.class, listener);
	}
	@SuppressWarnings("unchecked")
	protected void fireMoteDisconnectedEvent() {
		timer.cancel();
		
		MoteDisconnectedListener<Mote>[] listeners = listenerList
				.getListeners(MoteDisconnectedListener.class);
		MoteDisconnectedEvent<Mote> evt = new MoteDisconnectedEvent<Mote>(this);
		for (MoteDisconnectedListener<Mote> l : listeners) {
			l.moteDisconnected(evt);
		}
	}

	@SuppressWarnings("unchecked")
	protected void fireAccelerometerEvent(float x, float y, float z) {
		AccelerometerListener<Mote>[] listeners = listenerList
				.getListeners(AccelerometerListener.class);
		AccelerometerEvent<Mote> evt = new AccelerometerEvent<Mote>(this, x, y,
				z);
		for (AccelerometerListener<Mote> l : listeners) {
			l.accelerometerChanged(evt);
		}
	}
	
	protected void fireReadDataEvent(byte[] address, byte[] payload, int error) {
		byte[] buff = payload;
		if (calibrationDataReport == null && error == 0 && address[0] == 0x00
				&& address[1] == 0x20) {
			// calibration data (most probably)
			Log.d("motea", "Received Calibration Data Report.");

			int x0 = ((buff[0] & 0xff) << 2) | (buff[3] & 0x03);
			int y0 = ((buff[1] & 0xff) << 2) | ((buff[3] & 0xff) >> 2 & 0x03);
			int z0 = ((buff[2] & 0xff) << 2) | ((buff[3] & 0xff) >> 4 & 0x03);

			int xg = ((buff[4] & 0xff) << 2) | (buff[3] & 0x03);
			int yg = ((buff[5] & 0xff) << 2) | ((buff[3] & 0xff) >> 2 & 0x03);
			int zg = ((buff[6] & 0xff) << 2) | ((buff[3] & 0xff) >> 4 & 0x03);

			CalibrationDataReport report = new CalibrationDataReport(x0, y0,
					z0, xg, yg, zg);
			calibrationDataReport = report;
		}

		int motionplus = 0;
		if (error == 0 && address[0] == 0 && (address[1] & 0xff) == 0xfa) {
			motionplus = ((int) buff[0] << 40) | ((int) buff[1] << 32)
					| ((int) buff[2]) << 24 | ((int) buff[3]) << 16
					| ((int) buff[4]) << 8 | buff[5];
			if(extension == null && (motionplus & 0xffffffff) == 0xa6200005){
				extension = new MotionPlus(this);
			}
		}
		
		DataListener[] listeners = listenerList.getListeners(DataListener.class);
		DataEvent evt = new DataEvent(address, payload, error);
		for(DataListener l : listeners){
			l.dataRead(evt);
		}
	}

	protected void fireStatusInformationChangedEvent(
			StatusInformationReport report) {
		if(extension != null && !report.isExtensionControllerConnected()){
			extension = null;
		}
		
		StatusInformationListener[] listeners = listenerList
				.getListeners(StatusInformationListener.class);
		for (StatusInformationListener l : listeners) {
			l.statusInformationReceived(report);
		}
	}
	
	public void setReportMode(byte mode) {
		reportMode = mode;
		connection.sendRequest(new ReportModeRequest(mode, rumble));
	}

	protected void fireCoreButtonEvent(int modifiers) {
		CoreButtonListener[] listeners = listenerList
				.getListeners(CoreButtonListener.class);
		CoreButtonEvent evt = new CoreButtonEvent(this, modifiers);
		for (CoreButtonListener l : listeners) {
			l.buttonPressed(evt);
		}
	}
	
	protected void fireGyroEvent(GyroEvent evt){
		GyroListener[] listeners = listenerList.getListeners(GyroListener.class);
		for(GyroListener l : listeners){
			l.gyroChanged(evt);
		}
	}

	public CalibrationDataReport getCalibrationDataReport() {
		return calibrationDataReport;
	}
	
	public void calibrateMotionPlus(){
		if(extension !=null	 && extension instanceof MotionPlus){
			((MotionPlus)extension).calibrate();
		}
	}

	public void disconnect() {
		connection.disconnect();
	}

	public void rumble(boolean on) {
		rumble = on;
		setLeds(leds);
	}

	public boolean isRumbling(){
		return rumble;
	}
	
	public void setLeds(boolean[] leds) {
		this.leds = leds;
		connection.sendRequest(new PlayerLedRequest(leds, rumble));
	}

	public void writeRegisters(byte[] offset, byte[] payload){
		connection.sendRequest(new WriteRegisterRequest(offset, payload, rumble));
	}
	
	public void readRegisters(byte[] offset, byte[] size){
		connection.sendRequest(new ReadRegisterRequest(offset, size, rumble));
	}

	public void setReportMode() {
		setReportMode(reportMode);
	}
}
