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
package no.ntnu.falldetection.utils.motea.request;

/**
 * Requests a specific DataReport mode.
 * <p>
 * @author <a href="mailto:vfritzsch@users.sourceforge.net">Volker Fritzsch</a>
 */
public class ReportModeRequest implements MoteRequest {

	/**
	 * Status Information
	 * <p>
	 * <i>not requestable</i>
	 */
	public static final byte DATA_REPORT_0x20 = 0x20;
	
	/**
	 * Answer report to read data command (0x17)
	 * <p>
	 * <i>not requestable</i>
	 */
	public static final byte DATA_REPORT_0x21 = 0x21;
	
	/**
	 * Ack
	 */
	public static final byte DATA_REPORT_0x22 = 0x22;
	
	/**
	 * Core Buttons
	 */
	public static final byte DATA_REPORT_0x30 = 0x30;
	
	/**
	 * Core Buttons and Accelerometer
	 */
	public static final byte DATA_REPORT_0x31 = 0x31;
	
	/**
	 * Core Buttons with 8 Extension bytes
	 */
	public static final byte DATA_REPORT_0x32 = 0x32;
	
	/**
	 * Core Buttons and Accelerometer with 12 IR bytes
	 */
	public static final byte DATA_REPORT_0x33 = 0x33;
	
	/**
	 * Core Buttons with 19 Extension bytes
	 */
	public static final byte DATA_REPORT_0x34 = 0x34;
	
	/**
	 * Core Buttons and Accelerometer with 16 Extension Bytes
	 */
	public static final byte DATA_REPORT_0x35 = 0x35;
	
	/**
	 * Core Buttons with 10 IR bytes and 9 Extension Bytes
	 */
	public static final byte DATA_REPORT_0x36 = 0x36;

	/**
	 * Core Buttons and Accelerometer with 10 IR bytes and 6 Extension Bytes
	 */
	public static final byte DATA_REPORT_0x37 = 0x37;
	
	/**
	 * 21 Extension Bytes
	 */
	public static final byte DATA_REPORT_0x3d = 0x3d;
	
	/**
	 * Interleaved Core Buttons and Accelerometer with 36 IR bytes
	 * 
	 * @see no.ntnu.falldetection.utils.motej.android.request#DATA_REPORT_0x3f
	 */
	public static final byte DATA_REPORT_0x3e = 0x3e;
	
	/**
	 * Interleaved Core Buttons and Accelerometer with 36 IR bytes
	 * 
	 * @see no.ntnu.falldetection.utils.motej.android.request#DATA_REPORT_0x3e
	 */
	public static final byte DATA_REPORT_0x3f = 0x3f;
	
	private byte mode;
	
	private boolean rumble;
	
	private boolean continuous;
	
	public ReportModeRequest(byte mode, boolean rumble) {
		this(mode, true, rumble);
	}
	
	public ReportModeRequest(byte mode, boolean continuous, boolean rumble) {
		if (mode < 0x30 || (mode > 0x37 && mode < 0x3d) || mode > 0x3f) {
			throw new IllegalArgumentException("Undefined data report mode");
		}
		this.mode = mode;
		this.continuous = continuous;
		this.rumble = rumble;
	}
	
	public byte[] getBytes() {
		byte[] bytes = new byte[4];
		bytes[0] = (byte)0xa2;
		bytes[1] = 0x12;
		bytes[2] = (byte)((continuous ? 0x04 :0x00)|(rumble? 0x01:0x00));
		bytes[3] = mode;
		
		return bytes;
	}
}
