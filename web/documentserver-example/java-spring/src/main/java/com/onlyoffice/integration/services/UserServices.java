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
import com.onlyoffice.integration.entities.User;
import com.onlyoffice.integration.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServices {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupServices groupServices;

    @Autowired
    private PermissionServices permissionService;

    public List<User> findAll(){
        return userRepository.findAll();
    }

    public Optional<User> findUserById(Integer id){
        return userRepository.findById(id);
    }

    public User createUser(String name, String email,
                           List<String> description, String group,
                           List<String> reviewGroups,
                           List<String> viewGroups,
                           List<String> editGroups,
                           List<String> removeGroups){
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setGroup(groupServices.createGroup(group));
        newUser.setDescriptions(description);

        List<Group> groupsReview = groupServices.createGroups(reviewGroups);
        List<Group> commentGroupsView = groupServices.createGroups(viewGroups);
        List<Group> commentGroupsEdit = groupServices.createGroups(editGroups);
        List<Group> commentGroupsRemove = groupServices.createGroups(removeGroups);

        Permission permission = permissionService
                .createPermission(groupsReview, commentGroupsView, commentGroupsEdit, commentGroupsRemove);
        newUser.setPermissions(permission);

        userRepository.save(newUser);

        return newUser;
    }
}
