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

    private String convertDescriptions(String username, List<String> description){
        String result = "<b>"+username+"</b><br/>"+description.
                stream().map(text -> "<li>"+text+"</li>")
                .collect(Collectors.joining());
        return result;
    }

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
        newUser.setDescriptions(convertDescriptions(name, description));

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
