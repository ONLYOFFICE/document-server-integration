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

public class Close {
    private String text;
    private Boolean visible;

    public Close() { }

    public Close(final String textParam, final Boolean visibleParam) {
        this.text = textParam;
        this.visible = visibleParam;
    }

    public String getText() {
        return text;
    }

    public void setText(final String textParam) {
        this.text = textParam;
    }

    public Boolean isVisible() {
        return visible;
    }

    public void setVisible(final Boolean visibleParam) {
        this.visible = visibleParam;
    }
}
