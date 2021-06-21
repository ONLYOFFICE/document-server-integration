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
