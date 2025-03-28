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

class Format {
    constructor(name, type, actions, convert, mime) {
        this.name = name;
        this.type = type;
        this.actions = actions;
        this.convert = convert;
        this.mime = mime;
    }

    isAutoConvertible() {
        return this.actions.includes('auto-convert');
    }

    isEditable() {
        return this.actions.includes('edit') || this.actions.includes('lossy-edit');
    }

    isFillable() {
        return this.actions.includes('fill');
    }
}

class FormatManager {
    formats = [];

    constructor(formats) {
        if(Array.isArray(formats)) this.formats = formats;
    }

    findByExtension(extension) {
        return this.formats.find(format => format.name == extension);
    }

    isAutoConvertible(extension) {
        let format = this.findByExtension(extension);
        return format !== undefined && format.isAutoConvertible();
    }

    isEditable(extension) {
        let format = this.findByExtension(extension);
        return format !== undefined && format.isEditable();
    }

    isFillable(extension) {
        let format = this.findByExtension(extension);
        return format !== undefined && format.isFillable();
    }
}