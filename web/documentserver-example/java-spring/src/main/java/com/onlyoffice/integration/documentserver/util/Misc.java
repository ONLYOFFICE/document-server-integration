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

package com.onlyoffice.integration.documentserver.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class Misc {

    // convert user descriptions to the specified format
    public String convertUserDescriptions(final String username, final List<String> description) {
        String result = "<div class=\"user-descr\"><b>" + username + "</b><br/><ul>" + description.
                stream().map(text -> "<li>" + text + "</li>")
                .collect(Collectors.joining()) + "</ul></div>";
        return result;
    }
}
