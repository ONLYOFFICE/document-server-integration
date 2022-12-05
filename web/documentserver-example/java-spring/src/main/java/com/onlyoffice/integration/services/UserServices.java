/**
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServices {
    private final UserRepository userRepository;
    private final GroupServices groupServices;
    private final PermissionServices permissionService;

    // get a list of all users
    public List<User> findAll() {
        return userRepository.findAll();
    }

    // get a user by their ID
    public Optional<User> findUserById(Integer id) {
        return userRepository.findById(id);
    }

    // create a user with the specified parameters
    public User createUser(String name, String email,
                           List<String> description, String group,
                           List<String> reviewGroups,
                           List<String> viewGroups,
                           List<String> editGroups,
                           List<String> removeGroups,
                           List<String> userInfoGroups, Boolean favoriteDoc,
                           Boolean chat) {
        User newUser = new User();
        newUser.setName(name);  // set the user name
        newUser.setEmail(email);  // set the user email
        newUser.setGroup(groupServices.createGroup(group));  // set the user group
        newUser.setDescriptions(description);  // set the user description
        newUser.setFavorite(favoriteDoc);  // specify if the user has the favorite documents or not

        List<Group> groupsReview = groupServices.createGroups(reviewGroups);  // define the groups whose changes the user can accept/reject
        List<Group> commentGroupsView = groupServices.createGroups(viewGroups);  // defines the groups whose comments the user can view
        List<Group> commentGroupsEdit = groupServices.createGroups(editGroups);  // defines the groups whose comments the user can edit
        List<Group> commentGroupsRemove = groupServices.createGroups(removeGroups);  // defines the groups whose comments the user can remove
        List<Group> usInfoGroups = groupServices.createGroups(userInfoGroups);

        Permission permission = permissionService
                .createPermission(groupsReview, commentGroupsView, commentGroupsEdit, commentGroupsRemove, usInfoGroups, chat);  // specify permissions for the current user
        newUser.setPermissions(permission);

        userRepository.save(newUser); // save a new user

        return newUser;
    }
}
