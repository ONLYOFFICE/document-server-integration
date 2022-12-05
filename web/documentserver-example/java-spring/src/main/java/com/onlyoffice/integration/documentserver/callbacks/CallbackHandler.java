/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

package com.onlyoffice.integration.documentserver.callbacks;

import com.onlyoffice.integration.dto.Track;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class CallbackHandler {

    private final Map<Integer, Callback> callbackHandlers = new HashMap<>();

    public void register(int code, Callback callback) {  // register a callback handler
        callbackHandlers.put(code, callback);
    }

    public int handle(Track body, String fileName) {  // handle a callback
        Callback callback = callbackHandlers.get(body.getStatus());
        if (callback == null) {
            log.warn("Callback status " + body.getStatus() + " is not supported yet");
            return 0;
        }

        return callback.handle(body, fileName);
    }
}
