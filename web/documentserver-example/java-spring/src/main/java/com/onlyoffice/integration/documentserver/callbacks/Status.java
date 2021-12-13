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

// document status
public enum Status {
    EDITING(1),  // 1 - document is being edited
    SAVE(2),  // 2 - document is ready for saving
    CORRUPTED(3),  // 3 - document saving error has occurred
    MUST_FORCE_SAVE(6),  // 6 - document is being edited, but the current document state is saved
    CORRUPTED_FORCE_SAVE(7);  // 7 - error has occurred while force saving the document
    private int code;
    Status(int code){
        this.code = code;
    }
    public int getCode(){  // get document status
        return this.code;
    }
}
