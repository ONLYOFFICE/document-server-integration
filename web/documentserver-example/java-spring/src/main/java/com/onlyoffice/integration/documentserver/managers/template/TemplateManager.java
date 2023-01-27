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

package com.onlyoffice.integration.documentserver.managers.template;

import com.onlyoffice.integration.documentserver.models.filemodel.Template;
import java.util.List;

// specify the template manager functions
public interface TemplateManager {
    List<Template> createTemplates(String fileName);  // create a template document with the specified name
    String getTemplateImageUrl(String fileName);  // get the template image URL for the specified file
}
