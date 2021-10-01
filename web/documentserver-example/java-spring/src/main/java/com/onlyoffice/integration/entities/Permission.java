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

package com.onlyoffice.integration.entities;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "`permission`")
@Getter
@Setter
public class Permission extends AbstractEntity {
    private Boolean comment = true;
    private Boolean copy = true;
    private Boolean download = true;
    private Boolean edit = true;
    private Boolean print = true;
    private Boolean fillForms = true;
    private Boolean modifyFilter = true;
    private Boolean modifyContentControl = true;
    private Boolean review = true;
    private Boolean templates=true;
    @ManyToMany
    private List<Group> reviewGroups;
    @ManyToMany
    private List<Group> commentsViewGroups;
    @ManyToMany
    private List<Group> commentsEditGroups;
    @ManyToMany
    private List<Group> commentsRemoveGroups;
}
