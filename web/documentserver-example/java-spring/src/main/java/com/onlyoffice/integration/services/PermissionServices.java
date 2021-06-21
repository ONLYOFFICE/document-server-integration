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
