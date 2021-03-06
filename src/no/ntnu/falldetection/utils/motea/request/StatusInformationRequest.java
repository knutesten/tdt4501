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
 * 
 * <p>
 * @author <a href="mailto:vfritzsch@users.sourceforge.net">Volker Fritzsch</a>
 */
public class StatusInformationRequest implements MoteRequest {
	private boolean rumble;
	
	public StatusInformationRequest(boolean rumble){
		this.rumble = rumble;
	}
	
	public byte[] getBytes() {
		byte[] bytes = new byte[3];
		bytes[0] = (byte)0xa2;
		bytes[1] = 0x15;
		bytes[2] = (byte) (rumble?0x01:0x00);
		
		return bytes;
	}
}
