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

    public Group createGroup(String name){
        if(name == null) return null;
        Optional<Group> group = groupRepository.findGroupByName(name);
        if(group.isPresent()) return group.get();
        Group newGroup = new Group();
        newGroup.setName(name);

        groupRepository.save(newGroup);

        return newGroup;
    }

    public List<Group> createGroups(List<String> reviewGroups){
        if(reviewGroups == null) return null;
        return reviewGroups.stream()
                .map(group -> createGroup(group))
                .collect(Collectors.toList());
    }
}
