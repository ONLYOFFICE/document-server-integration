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

package com.onlyoffice.integration.sdk.manager;

import com.onlyoffice.manager.settings.DefaultSettingsManager;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Properties;

@Component
public class SettingsManagerImpl extends DefaultSettingsManager {
    private static final String SETTINGS_PREFIX = "docservice";

    private static Properties properties;

    public SettingsManagerImpl() {
        init();
    }

    @Override
    public String getSetting(final String name) {
        return properties.getProperty(SETTINGS_PREFIX + "." + name);
    }

    @Override
    public void setSetting(final String name, final String value) {
        properties.put(SETTINGS_PREFIX + "." + name, value);
    }

    protected static void init() {
        try {
            properties = new Properties();
            InputStream stream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("application.properties");
            properties.load(stream);
        } catch (Exception e) {
            properties = null;
        }
    }
}
