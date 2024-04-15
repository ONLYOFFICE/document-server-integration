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

package helpers;

import java.io.InputStream;
import java.util.Properties;

public final class ConfigManager {
    private static Properties properties;

    private ConfigManager() { }

    static {
        init();
    }

    private static void init() {
        try {
            // get stream from the settings.properties resource and load it
            properties = new Properties();
            InputStream stream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("settings.properties");
            properties.load(stream);
        } catch (Exception ex) {
            properties = null;
        }
    }

    // get name from the settings.properties file
    public static String getProperty(final String name) {
        if (properties == null) {
            return "";
        }

        // get property by its name
        String property = properties.getProperty(name);

        return property == null ? "" : property;
    }
}
