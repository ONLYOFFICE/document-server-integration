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

public enum StatusType {

    EDITING(1),
    MUST_SAVE(2),
    CORRUPTED(3),
    MUST_FORCE_SAVE(6),
    CORRUPTED_FORCE_SAVE(7);
    private final int code;

    StatusType(final int codeParam) {
        this.code = codeParam;
    }

    public int getCode() {
       return code;
    }
}
