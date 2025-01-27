/**
 *
 * (c) Copyright Ascensio System SIA 2025
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
        callbackManager.processEditing(callback, fileId);
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
