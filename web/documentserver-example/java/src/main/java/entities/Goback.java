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

package entities;

public class Goback {
    private String text;
    private Boolean blank;

    public Goback() { }

    public Goback(final String textParam, final Boolean blankParam) {
        this.text = textParam;
        this.blank = blankParam;
    }

    public String getText() {
        return text;
    }

    public Boolean getBlank() {
        return blank;
    }
}
