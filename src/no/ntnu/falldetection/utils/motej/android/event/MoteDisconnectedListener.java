/*
 * Copyright 2007-2008 motej
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
package no.ntnu.falldetection.utils.motej.android.event;

import java.util.EventListener;

/**
 *
 * <p>
 * @author Guillaume Sauthier
 */
public interface MoteDisconnectedListener<T> extends EventListener {

    public void moteDisconnected(MoteDisconnectedEvent<T> evt);

}