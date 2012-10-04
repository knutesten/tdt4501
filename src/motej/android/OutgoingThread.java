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

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

import motej.android.request.MoteRequest;
import motej.android.request.PlayerLedRequest;
import motej.android.request.RumbleRequest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * 
 * <p>
 * 
 * @author <a href="mailto:vfritzsch@users.sourceforge.net">Volker Fritzsch</a>
 */
class OutgoingThread extends Thread {

	private static final long THREAD_SLEEP = 10l;

	private volatile boolean active;

	private OutputStream outgoing;

	private volatile ConcurrentLinkedQueue<MoteRequest> requestQueue;

	private byte ledByte;

	private long rumbleMillis = Long.MIN_VALUE;

	private Mote source;

	private boolean connecting;

	private static final int PORT = 0x11;

	protected OutgoingThread(Mote source, BluetoothDevice remoteDevice)
			throws InterruptedException {
		connecting = true;
		new ConnectThread(remoteDevice, PORT).start();

		this.source = source;

		requestQueue = new ConcurrentLinkedQueue<MoteRequest>();
		Thread.sleep(THREAD_SLEEP);
	}

	public void disconnect() {
		active = false;
	}

	public void run() {
		while (connecting)
			;

		while (active || !requestQueue.isEmpty()) {
			try {
				if (rumbleMillis > 0) {
					rumbleMillis -= THREAD_SLEEP;
				}
				if (rumbleMillis == 0) {
					rumbleMillis = Long.MIN_VALUE;
					outgoing.write(RumbleRequest.getStopRumbleBytes(ledByte));
					Thread.sleep(THREAD_SLEEP);
					continue;
				}
				if (!requestQueue.isEmpty()) {
					MoteRequest request = requestQueue.poll();
					if (request instanceof PlayerLedRequest) {
						ledByte = ((PlayerLedRequest) request).getLedByte();
					}
					if (request instanceof RumbleRequest) {
						((RumbleRequest) request).setLedByte(ledByte);
						rumbleMillis = ((RumbleRequest) request).getMillis();
					}

					// if (log.isTraceEnabled()) {
					// byte[] buf = request.getBytes();
					// StringBuffer sb = new StringBuffer();
					// log.trace("sending:");
					// for (int i = 0; i < buf.length; i++) {
					// String hex = Integer.toHexString(buf[i] & 0xff);
					// sb.append(hex.length() == 1 ? "0x0" :
					// "0x").append(hex).append(" ");
					// if ((i + 1) % 8 == 0) {
					// log.trace(sb.toString());
					// sb.delete(0, sb.length());
					// }
					// }
					// if (sb.length() > 0) {
					// log.trace(sb.toString());
					// }
					// }

					outgoing.write(request.getBytes());
				}
				Thread.sleep(THREAD_SLEEP);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			} catch (IOException ex) {
				Log.e("motej.android", "connection closed?" + ex.getMessage());
				active = false;
				source.fireMoteDisconnectedEvent();
			}
		}
		try {
			outgoing.close();
		} catch (IOException ex) {
			Log.e("motej.android", ex.getMessage() + ": " + ex.getStackTrace());
		}
	}

	public void sendRequest(MoteRequest request) {
		requestQueue.add(request);
	}

	private class ConnectThread extends L2CAPConnectThread {

		ConnectThread(BluetoothDevice remoteDevice, int port) {
			super(remoteDevice, port);
		}

		@Override
		void manageConnectedSocket(BluetoothSocket socket) {
			try {
				outgoing = socket.getOutputStream();
				active = true;
				connecting = false;
			} catch (IOException e) {
				Log.e("motej.android", e.getMessage() +": " + e.getStackTrace());
			}
		}

		@Override
		void connectionFailure(IOException cause) {
			Log.e("motej.android", "Connection Failure: " + cause.getMessage());
			active = false;
			connecting = false;
		}

	}
}