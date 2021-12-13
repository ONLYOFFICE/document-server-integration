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

package com.onlyoffice.integration;

import com.onlyoffice.integration.documentserver.serializers.FilterState;
import com.onlyoffice.integration.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExampleData {
    @Autowired
    private UserServices userService;
    @PostConstruct
    public void init(){
        List<String> description_user_0=List.of(  // the description for user 0
                "The name is requested when the editor is opened",
                "Doesn’t belong to any group",
                "Can review all the changes",
                "Can perform all actions with comments",
                "The file favorite state is undefined",
                "Can't mention others in comments",
                "Can't create new files from the editor"
        );
        List<String> description_user_1 = List.of(  // the description for user 1
                "File author by default",
                "He doesn’t belong to any of the groups",
                "He can review all the changes",
                "He can do everything with the comments",
                "The file favorite state is undefined",
                "Can create a file from a template with data from the editor"
        );
        List<String> description_user_2 = List.of(  // the description for user 2
                "He belongs to Group2",
                "He can review only his own changes or the changes made by the users who don’t belong to any of the groups",
                "He can view every comment, edit his comments and the comments left by the users who don't belong to any of the groups and remove only his comments",
                "This file is favorite",
                "Can create a file from an editor"
        );
        List<String> description_user_3 = List.of(  // the description for user 3
                "He belongs to Group3",
                "He can review only the changes made by the users from Group2",
                "He can view the comments left by the users from Group2 and Group3 and edit the comments left by the users from Group2",
                "This file isn’t favorite",
                "He can’t copy data from the file into the clipboard",
                "He can’t download the file",
                "He can’t print the file",
                "Can create a file from an editor"
        );
        userService.createUser("John Smith", "smith@example.com",  // create user 1 with the specified parameters
                description_user_1, null, List.of(FilterState.NULL.toString()),
                List.of(FilterState.NULL.toString()),
                List.of(FilterState.NULL.toString()),
                List.of(FilterState.NULL.toString()), null);
        userService.createUser("Mark Pottato", "pottato@example.com",  // create user 2 with the specified parameters
                description_user_2, "group-2", List.of("","group-2"), List.of(FilterState.NULL.toString()),
                List.of("group-2", ""), List.of("group-2"), true);
        userService.createUser("Hamish Mitchell", "mitchell@example.com",  // create user 3 with the specified parameters
                description_user_3, "group-3", List.of("group-2"), List.of("group-2", "group-3"),
                List.of("group-2"), new ArrayList<>(), false);
        userService.createUser("Anonymous",null,  // create user 0 with the specified parameters
                description_user_0,null, List.of(FilterState.NULL.toString()), List.of(FilterState.NULL.toString()),
                List.of(FilterState.NULL.toString()), List.of(FilterState.NULL.toString()), null);
    }
}
