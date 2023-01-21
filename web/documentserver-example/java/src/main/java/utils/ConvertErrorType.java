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

package utils;

public enum ConvertErrorType {
    EMPTY_ERROR(0, ""),
    CONVERTATION_UNKNOWN(-1, "Error convertation unknown"),
    CONVERTATION_TIMEOUT(-2, "Error convertation timeout"),
    CONVERTATION_ERROR(-3, "Error convertation error"),
    DOWNLOAD_ERROR(-4, "Error download error"),
    UNEXPECTED_GUID_ERROR(-5, "Error unexpected guid"),
    DATABASE_ERROR(-6, "Error database"),
    DOCUMENT_REQUEST_ERROR(-7, "Error document request"),
    DOCUMENT_VKEY_ERROR(-8, "Error document VKey");

    private final int code;
    private final String label;

    ConvertErrorType(final int codeParam, final String labelParam) {
        this.code = codeParam;
        this.label = labelParam;
    }

    public static String labelOfCode(final int code) {
        for (ConvertErrorType convertErrorType : values()) {
            if (convertErrorType.code == code) {
                return convertErrorType.label;
            }
        }
        return "ErrorCode = " + code;
    }
}

