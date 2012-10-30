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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import android.util.Log;

/**
 * 
 * <p>
 * 
 * @author <a href="mailto:vfritzsch@users.sourceforge.net">Volker Fritzsch</a>
 */
public class ExtensionProvider {

	private static Map<String, Class<? extends Extension>> lookup;

	@SuppressWarnings("unchecked")
	public ExtensionProvider() {
		synchronized (ExtensionProvider.class) {
			if (lookup == null) {
				Log.d("motej.android", "Initializing lookup.");
				lookup = new HashMap<String, Class<? extends Extension>>();
				InputStream in = ExtensionProvider.class
						.getResourceAsStream("/no/ntnu/falldetection/utils/motejx/extensions/extensions.properties");
				Properties props = new Properties();

				if (in == null) {
					Log.i("motej.android",
							"no extensions.properties found. as a result, no extensions will be available.");
					
					return;
				}

				try {
					props.load(in);
					for (Object o : props.keySet()) {
						String key = (String) o;
						String value = props.getProperty(key);

							Log.d("motej.android", "Adding extension (" + key + " / "
									+ value + ").");

						Class<? extends Extension> clazz = (Class<? extends Extension>) Class
								.forName(value);
						lookup.put(key, clazz);
					}
				} catch (IOException ex) {
					Log.w("motej.android",
							ex.getMessage() + ": " + ex.getStackTrace());
				} catch (ClassNotFoundException ex) {
					Log.w("motej.android",
							ex.getMessage() + ": " + ex.getStackTrace());
				}
			}
		}
		Log.d("motej.android", "Lookup initialized.");
		
	}

	public Extension getExtension(byte[] id) {
		String id0 = Integer.toHexString(id[0] & 0xff);
		if (id0.length() == 1) {
			id0 = "0" + id0;
		}
		String id1 = Integer.toHexString(id[1] & 0xff);
		if (id1.length() == 1) {
			id1 = "0" + id1;
		}
		String key = id0 + id1;
		Class<? extends Extension> clazz = lookup.get(key);

		if (clazz == null) {
			Log.w("motej.android", "No matching extension found for key: "
					+ key);
			return null;
		}

		Extension extension = null;
		try {
			extension = clazz.newInstance();
		} catch (InstantiationException ex) {
			Log.e("motej.android", ex.getMessage() + ": " + ex.getStackTrace());
		} catch (IllegalAccessException ex) {
			Log.e("motej.android", ex.getMessage() + ": " + ex.getStackTrace());
		}
		return extension;
	}
}
