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
package no.ntnu.falldetection.utils.motej.android.request;


/**
 * 
 * <p>
 * @author <a href="mailto:vfritzsch@users.sourceforge.net">Volker Fritzsch</a>
 */
public class PlayerLedRequest implements MoteRequest {

	protected int ledByte;
	
	public PlayerLedRequest(boolean[] leds) {
		for (int i = 0; i < 4; i++) {
			if (leds[i]) {
				ledByte = ledByte ^ 1 << (4 + i);
			}
		}
	}
	
	public byte[] getBytes() {
		return new byte[] { 82, 17, (byte) ledByte };
	}
	
	public byte getLedByte() {
		return (byte) ledByte;
	}

}