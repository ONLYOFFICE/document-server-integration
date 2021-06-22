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

package com.onlyoffice.integration.entities.filemodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onlyoffice.integration.serializer.SerializerFilter;
import com.onlyoffice.integration.entities.Group;

import java.util.List;
import java.util.stream.Collectors;

public class CommentGroup {
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = SerializerFilter.class)
    private List<String> view;
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = SerializerFilter.class)
    private List<String> edit;
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = SerializerFilter.class)
    private List<String> remove;

    public CommentGroup(List<Group> view, List<Group> edit, List<Group> remove) {
        this.view = view.stream()
                .map(group -> group.getName())
                .collect(Collectors.toList());

        this.edit = edit.stream()
                .map(group -> group.getName())
                .collect(Collectors.toList());

        this.remove = remove.stream()
                .map(group -> group.getName())
                .collect(Collectors.toList());
    }

    public List<String> getView() {
        return view;
    }

    public List<String> getEdit() {
        return edit;
    }

    public List<String> getRemove() {
        return remove;
    }
}
