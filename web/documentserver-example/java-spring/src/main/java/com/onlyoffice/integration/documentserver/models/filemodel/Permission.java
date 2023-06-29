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

package com.onlyoffice.integration.documentserver.models.filemodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onlyoffice.integration.documentserver.models.AbstractModel;
import com.onlyoffice.integration.documentserver.serializers.SerializerFilter;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
@Getter
@Setter
public class Permission extends AbstractModel {  // the permission for the document to be edited and downloaded or not
    private Boolean comment = true;  // if the document can be commented or not
    private Boolean copy = true;  // if the content can be copied to the clipboard or not
    private Boolean download = true;  // if the document can be downloaded or only viewed or edited online
    private Boolean edit = true;  // if the document can be edited or only viewed
    private Boolean print = true;  // if the document can be printed or not
    private Boolean fillForms = true;  // if the forms can be filled
    private Boolean modifyFilter = true;  /* if the filter can applied globally (true) affecting all the
     other users, or locally (false) */
    private Boolean modifyContentControl = true;  // if the content control settings can be changed
    private Boolean review = true;  // if the document can be reviewed or not
    private Boolean chat = true;  // if a chat can be used
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = SerializerFilter.class)
    private List<String> reviewGroups;  // the groups whose changes the user can accept/reject
    @Autowired
    private CommentGroup commentGroups;  //  the groups whose comments the user can edit, remove and/or view
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = SerializerFilter.class)
    private List<String> userInfoGroups;
    private Boolean protect = true;
}
