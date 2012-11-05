package no.ntnu.falldetection.utils.motea;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

abstract class L2CAPConnectThread extends Thread {
	final BluetoothDevice mRemoteDevice;
	final int mPort;
	final ReentrantReadWriteLock mLock;
	boolean mClosed;

	BluetoothSocket mSocket;

	L2CAPConnectThread(BluetoothDevice remoteDevice, int port) {
		mRemoteDevice = remoteDevice;
		mPort = port;
		mLock = new ReentrantReadWriteLock();
	}

	@Override
	public void run() {
		boolean succeed = ensureSocket();

		if (!succeed) {
			return;
		}

		waitForConnectionEstablished();
	}

	final void cancel() {
		mLock.readLock().lock();

		try {
			if (mClosed) {
				return;
			}
		} finally {
			mLock.readLock().unlock();
		}

		mLock.writeLock().lock();

		try {
			mClosed = true;
		} finally {
			mLock.writeLock().unlock();
		}

		try {
			mSocket.close();
		} catch (IOException e) {
		}
	}

	final boolean ensureSocket() {
		try {
			BluetoothSocket socket = null;
			socket = BluetoothConnectionFactory.createBluetoothSocket(
					BluetoothConnectionFactory.TYPE_L2CAP, -1, false, false, mRemoteDevice, mPort, null);
			mSocket = socket;
			return true;
		} catch (IOException e) {
			connectionFailure(e);
		}

		return false;
	}

	final void waitForConnectionEstablished() {
		try {
			mSocket.connect();
		} catch (IOException e) {
			mLock.readLock().lock();

			try {
				if (mClosed) {
					return;
				}
			} finally {
				mLock.readLock().unlock();
			}

			cancel();
			connectionFailure(e);
			return;
		}

		mLock.writeLock().lock();

		try {
			mClosed = true;
			manageConnectedSocket(mSocket);
		} finally {
			mLock.writeLock().unlock();
		}
	}

	abstract void manageConnectedSocket(BluetoothSocket socket);

	abstract void connectionFailure(IOException cause);
}
