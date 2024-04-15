/**
 *
 * (c) Copyright Ascensio System SIA 2024
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
 *
 */

package com.onlyoffice.integration.documentserver.managers.callback;

import com.onlyoffice.integration.dto.Track;
import java.util.HashMap;

public interface CallbackManager {  // specify the callback manager functions
    void processSave(Track body, String fileName);  // file saving process
    void commandRequest(String method, String key, HashMap meta);  // create a command request
    void processForceSave(Track body, String fileName);  // file force saving process
}
