package motej.android;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;

abstract class BluetoothConnectionFactory {

	static final int TYPE_RFCOMM = 1;
	static final int TYPE_SCO = 2;
	static final int TYPE_L2CAP = 3;
	static final Constructor<BluetoothSocket> BLUETOOTH_SOCKET_CONSTRUCTOR;

	static {
		BLUETOOTH_SOCKET_CONSTRUCTOR = obtainConstructor();
	}

	static final BluetoothSocket createBluetoothSocket(int type, int fd,
			boolean auth, boolean encrypt, BluetoothDevice device, int port,
			ParcelUuid uuid) throws IOException {
		Constructor<BluetoothSocket> constructor = obtainConstructor();
		BluetoothSocket tmp = null;

		try {
			tmp = constructor.newInstance(type, fd, auth, encrypt, device,
					port, uuid);
		} catch (IllegalArgumentException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}

		return tmp;
	}

	private static final Constructor<BluetoothSocket> obtainConstructor() {
		Class<BluetoothSocket> cls = BluetoothSocket.class;
		Constructor<BluetoothSocket> tmp = null;

		try {
			tmp = cls.getDeclaredConstructor(int.class, int.class,
					boolean.class, boolean.class, BluetoothDevice.class,
					int.class, ParcelUuid.class);
			if (!tmp.isAccessible()) {
				tmp.setAccessible(true);
			}
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		}

		return tmp;
	}
}
