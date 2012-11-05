package no.ntnu.falldetection.utils.motea;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import no.ntnu.falldetection.utils.motea.event.GyroEvent;
import no.ntnu.falldetection.utils.motea.extension.MotionPlus;
import no.ntnu.falldetection.utils.motea.request.MoteRequest;
import no.ntnu.falldetection.utils.motea.request.ReportModeRequest;
import no.ntnu.falldetection.utils.motea.request.WriteRegisterRequest;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class WiiMoteConnection {
	private final long LOCK_TIME = 1000l;
	private IncomingThread incomingThread;
	private OutgoingThread outgoingThread;
	private boolean active;
	private Mote source;
	private Semaphore mutex = new Semaphore(1);
	private final SimpleLock lock = new SimpleLock();
	
	public volatile ConcurrentLinkedQueue<MoteRequest> requestQueue = new ConcurrentLinkedQueue<MoteRequest>();

	public WiiMoteConnection(BluetoothSocket socket, Mote source)
			throws IOException {
		this.source = source;
		active = true;
		
		incomingThread = new IncomingThread(socket.getInputStream());
		outgoingThread = new OutgoingThread(socket.getOutputStream());
		
		incomingThread.start();
		outgoingThread.start();
	}

	public void sendRequest(MoteRequest request) {
		requestQueue.add(request);
	}

	private class IncomingThread extends Thread {
		InputStream incoming;
		public IncomingThread(InputStream inputStream) {
			incoming = inputStream;
		}

		public void run() {
			int count = 0;
			byte[] buf = new byte[23];
			while (active) {
				try {
					incoming.read(buf);
					
//					if(count++%100==0){
//						Log.e("shit", "is happening");
//					}
					
					switch (buf[1]) {
					case ReportModeRequest.DATA_REPORT_0x20:
						parseStatusInformation(buf);
						break;

					case ReportModeRequest.DATA_REPORT_0x21:
						parseCoreButtonData(buf);
						parseMemoryData(buf);
						break;

					case ReportModeRequest.DATA_REPORT_0x22:
						Log.e("ack", "is here");
						outgoingThread.writeDone();
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
					Log.e("motej.android", "Connection closed?");
					active = false;
				} catch (NullPointerException ex) {
					Log.e("motej.android", "Connection lost");
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
	}

	private class OutgoingThread extends Thread {
		OutputStream outgoing;
		public OutgoingThread(OutputStream outputStream) {
			outgoing = outputStream;
		}

		public void run() {
			MoteRequest request = null;
			while (active) {
				try {
					request = requestQueue.poll();
					if (request == null) {
						Thread.sleep(10);
						continue;
					}
					outgoing.write(request.getBytes());
					if(request instanceof WriteRegisterRequest){
						Log.e("write", "write");
						synchronized(this){
							wait();
						}
					}
				} catch (InterruptedException e) {
				} catch (IOException e) {
					Log.e("motea", e.getMessage() + ": " + e.getStackTrace());
				} catch (NullPointerException e) {
					Log.e("motea", e.getMessage() + ": " + e.getStackTrace());
				}
			}
		}
		
		public synchronized void writeDone(){
			notify();
		}
	}

	protected void disconnect() {
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
		float x = ((bytes[4] & 0xff) << 2) | ((bytes[2] & 0xff) >> 5 & 0x03);
		float y = ((bytes[5] & 0xff) << 2) | ((bytes[3] & 0xff) >> 5 & 0x02);
		float z = ((bytes[6] & 0xff) << 2) | ((bytes[3] & 0xff) >> 5 & 0x02);

		CalibrationDataReport c = source.getCalibrationDataReport();
		if (c == null) {
			return;
		}

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
