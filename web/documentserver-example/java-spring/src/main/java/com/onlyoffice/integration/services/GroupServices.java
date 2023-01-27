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

package com.onlyoffice.integration.services;

import com.onlyoffice.integration.entities.Group;
import com.onlyoffice.integration.repositories.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GroupServices {

    @Autowired
    private GroupRepository groupRepository;

    // create a new group with the specified name
    public Group createGroup(final String name) {
        if (name == null) {
            return null;  // check if a name is specified
        }
        Optional<Group> group = groupRepository
                .findGroupByName(name);  // check if group with such a name already exists
        if (group.isPresent()) {
            return group.get();  // if it exists, return it
        }
        Group newGroup = new Group();
        newGroup.setName(name);  // otherwise, create a new group with the specified name

        groupRepository.save(newGroup);  // save a new group

        return newGroup;
    }

    // create a list of groups from the reviewGroups permission parameter
    public List<Group> createGroups(final List<String> reviewGroups) {
        if (reviewGroups == null) {
            return null;  // check if the reviewGroups permission exists
        }
        // convert this parameter to a list of groups whose changes the user can accept/reject
        return reviewGroups.stream()
                .map(group -> createGroup(group))
                .collect(Collectors.toList());
    }
}
