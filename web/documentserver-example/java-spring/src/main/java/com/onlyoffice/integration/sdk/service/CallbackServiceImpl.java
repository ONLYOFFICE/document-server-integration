/**
 *
 * (c) Copyright Ascensio System SIA 2023
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

package com.onlyoffice.integration.sdk.service;

import com.onlyoffice.integration.documentserver.managers.callback.CallbackManager;
import com.onlyoffice.manager.security.JwtManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.model.documenteditor.Callback;
import com.onlyoffice.model.documenteditor.callback.Action;
import com.onlyoffice.model.documenteditor.callback.action.Type;
import com.onlyoffice.service.documenteditor.callback.DefaultCallbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CallbackServiceImpl extends DefaultCallbackService {

    private Logger logger = LoggerFactory.getLogger(CallbackServiceImpl.class);

    @Autowired
    private CallbackManager callbackManager;

    public CallbackServiceImpl(final JwtManager jwtManager, final SettingsManager settingsManager) {
        super(jwtManager, settingsManager);
    }

    @Override
    public void handlerEditing(final Callback callback, final String fileId) throws Exception {
        Action action = callback.getActions().get(0);  // get the user ID who is editing the document
        if (action.getType().equals(Type.CONNECTED)) {  // if this value is not equal to the user ID
            String user = action.getUserid();  // get user ID
            if (!callback.getUsers().contains(user)) {  // if this user is not specified in the body
                String key = callback.getKey();  // get document key
                // create a command request to forcibly save the document being edited without closing it
                callbackManager.commandRequest("forcesave", key, null);

            }
        }
    }

    @Override
    public void handlerSave(final Callback callback, final String fileId) throws Exception {
        callbackManager.processSave(callback, fileId);
    }

    @Override
    public void handlerClosed(final Callback callback, final String fileId) throws Exception {
        logger.warn("Callback status " + callback.getStatus() + " is not supported yet");
    }

    @Override
    public void handlerForcesave(final Callback callback, final String fileId) throws Exception {
        callbackManager.processForceSave(callback, fileId);
    }
}
