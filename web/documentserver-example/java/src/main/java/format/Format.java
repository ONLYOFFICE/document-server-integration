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

package format;

import java.util.List;

import entities.FileType;

public final class Format {
    private final String name;

    public String getName() {
        return this.name;
    }

    private final FileType type;

    public FileType getType() {
        return this.type;
    }

    private final List<String> actions;

    public List<String> getActions() {
        return this.actions;
    }

    private final List<String> convert;

    public List<String> getConvert() {
        return this.convert;
    }

    private final List<String> mime;

    public List<String> getMime() {
        return this.mime;
    }

    public Format(final String nameParameter,
                  final FileType typeParameter,
                  final List<String> actionsParameter,
                  final List<String> convertParameter,
                  final List<String> mimeParameter) {
        this.name = nameParameter;
        this.type = typeParameter;
        this.actions = actionsParameter;
        this.convert = convertParameter;
        this.mime = mimeParameter;
    }

    public String extension() {
        return "." + this.name;
    }
}
