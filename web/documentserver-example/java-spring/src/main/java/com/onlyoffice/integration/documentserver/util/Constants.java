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

package com.onlyoffice.integration.documentserver.util;

public final class Constants {
    public static final Integer MAX_FILE_SIZE = 5 * 1024 * 1024;
    public static final Integer CONVERT_TIMEOUT_MS = 120000;
    public static final String CONVERTATION_ERROR_MESSAGE_TEMPLATE = "Error occurred in the ConvertService: ";
    public static final Long FULL_LOADING_IN_PERCENT = 100L;
    public static final Integer FILE_SAVE_TIMEOUT = 5000;
    public static final Integer MAX_KEY_LENGTH = 20;
    public static final Integer ANONYMOUS_USER_ID = 4;
    public static final Integer KILOBYTE_SIZE = 1024;

    private Constants() { }
}

