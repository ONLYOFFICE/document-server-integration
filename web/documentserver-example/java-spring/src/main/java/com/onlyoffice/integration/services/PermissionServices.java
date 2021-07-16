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

package com.onlyoffice.integration.services;

import com.onlyoffice.integration.entities.Group;
import com.onlyoffice.integration.entities.Permission;
import com.onlyoffice.integration.repositories.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionServices {

    @Autowired
    private PermissionRepository permissionRepository;

    public Permission createPermission(List<Group> reviewGroups,
                                       List<Group> commentViewGroups,
                                       List<Group> commentEditGroups,
                                       List<Group> commentRemoveGroups){

        Permission permission = new Permission();
        permission.setReviewGroups(reviewGroups);
        permission.setCommentsViewGroups(commentViewGroups);
        permission.setCommentsEditGroups(commentEditGroups);
        permission.setCommentsRemoveGroups(commentRemoveGroups);

        permissionRepository.save(permission);

        return permission;
    }

    public Permission updatePermission(Permission newPermission){
        permissionRepository.save(newPermission);

        return newPermission;
    }
}
