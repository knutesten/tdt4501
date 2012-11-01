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
package no.ntnu.falldetection.utils.motej.android;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import no.ntnu.falldetection.utils.motej.android.request.MoteRequest;
import no.ntnu.falldetection.utils.motej.android.request.PlayerLedRequest;
import no.ntnu.falldetection.utils.motej.android.request.RumbleRequest;
import no.ntnu.falldetection.utils.motej.android.request.WriteRegisterRequest;
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

	private OutputStream outgoing = null;

	private volatile ConcurrentLinkedQueue<MoteRequest> requestQueue;

	private byte ledByte;

	private long rumbleMillis = Long.MIN_VALUE;

	private Mote source;

	private static final int PORT = 0x11;

	protected OutgoingThread(Mote source, BluetoothDevice remoteDevice)
			throws InterruptedException {
		active = true;
		new ConnectThread(remoteDevice, PORT).start();

		this.source = source;

		requestQueue = new ConcurrentLinkedQueue<MoteRequest>();
		Thread.sleep(THREAD_SLEEP);
	}

	public void disconnect() {
		active = false;
	}

	public void run() {
		while (outgoing == null && active)
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
					if(request instanceof WriteRegisterRequest){
						Log.e("writing", "writing");
						synchronized(this){
							wait();
						}
					}
				}
				Thread.sleep(THREAD_SLEEP);
			} catch (InterruptedException ex) {
//				ex.printStackTrace();
				Log.e("motej.android", "Interrupted exception" + ex.getStackTrace());
			} catch (IOException ex) {
				Log.e("motej.android", "connection closed?" + ex.getMessage());
				active = false;
				source.fireMoteDisconnectedEvent();
			}
		}
		try {
			if (outgoing != null) {
				outgoing.close();
			}
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
			} catch (IOException e) {
				Log.e("motej.android",
						e.getMessage() + ": " + e.getStackTrace());
			}
		}

		@Override
		void connectionFailure(IOException cause) {
			Log.e("motej.android", "Connection Failure: " + cause.getMessage());
			active = false;
		}

	}

	public void writeDone() {
		synchronized (this) {
			notify();
		}
	}
}