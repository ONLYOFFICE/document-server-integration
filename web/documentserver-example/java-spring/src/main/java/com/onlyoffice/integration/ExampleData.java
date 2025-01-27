/**
 *
 * (c) Copyright Ascensio System SIA 2025
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
import com.onlyoffice.integration.entities.Goback;
import com.onlyoffice.integration.entities.Close;
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
    public void init() {
        // the description for user 0
        List<String> descriptionUserZero = List.of(
                "The name is requested when the editor is opened",
                "Doesn’t belong to any group",
                "Can review all the changes",
                "Can perform all actions with comments",
                "The file favorite state is undefined",
                "Can't mention others in comments",
                "Can't create new files from the editor",
                "Can’t see anyone’s information",
                "Can't rename files from the editor",
                "Can't view chat",
                "Can't protect file",
                "View file without collaboration",
                "Can’t submit forms",
                "Can't refresh outdated file"
        );

        // the description for user 1
        List<String> descriptionUserFirst = List.of(
                "File author by default",
                "He doesn’t belong to any of the groups",
                "He can review all the changes",
                "He can do everything with the comments",
                "The file favorite state is undefined",
                "Can create a file from a template with data from the editor",
                "Can see the information about all users",
                "Can view chat",
                "Has an avatar",
                "Can submit forms"
        );

        // the description for user 2
        List<String> descriptionUserSecond = List.of(
                "He belongs to Group2",
                "He can review only his own changes or the changes made by the users who don’t belong"
                        + " to any of the groups",
                "He can view every comment, edit his comments and the comments left by the users "
                        + "who don't belong to any of the groups and remove only his comments",
                "This file is favorite",
                "Can create a file from an editor",
                "Can see the information about users from Group2 and users who don’t belong to any group",
                "Can view chat",
                "Has an avatar",
                "Can’t submit forms"
        );

        // the description for user 3
        List<String> descriptionUserThird = List.of(
                "He belongs to Group3",
                "He can review only the changes made by the users from Group2",
                "He can view the comments left by the users from Group2 and Group3 and edit the comments left by "
                        + "the users from Group2",
                "This file isn’t favorite",
                "He can’t copy data from the file into the clipboard",
                "He can’t download the file",
                "He can’t print the file",
                "Can create a file from an editor",
                "Can see the information about Group2 users",
                "Can view chat",
                "Can’t submit forms",
                "Can't close history",
                "Can't restore the file version"
        );

        // create user 1 with the specified parameters
        userService.createUser("John Smith", "smith@example.com", descriptionUserFirst,
                "", List.of(FilterState.NULL.toString()), List.of(FilterState.NULL.toString()),
                List.of(FilterState.NULL.toString()), List.of(FilterState.NULL.toString()),
                List.of(FilterState.NULL.toString()), null, true, true, true,
                new Goback(null, false), new Close(null, false), true);

        // create user 2 with the specified parameters
        userService.createUser("Mark Pottato", "pottato@example.com", descriptionUserSecond,
                "group-2", List.of("", "group-2"), List.of(FilterState.NULL.toString()),
                List.of("group-2", ""), List.of("group-2"), List.of("group-2", ""), true, true,
                true, true, new Goback("Go to Documents", null), new Close(null, true), false);

        // create user 3 with the specified parameters
        userService.createUser("Hamish Mitchell", null, descriptionUserThird,
                "group-3", List.of("group-2"), List.of("group-2", "group-3"), List.of("group-2"),
                new ArrayList<>(), List.of("group-2"), false, true, true, false,
                null, new Close(null, true), false);

        // create user 0 with the specified parameters
        userService.createUser("Anonymous", null, descriptionUserZero, "",
                List.of(FilterState.NULL.toString()), List.of(FilterState.NULL.toString()),
                List.of(FilterState.NULL.toString()), List.of(FilterState.NULL.toString()),
                new ArrayList<>(), null, false, false, false, null, null, false);
    }
}
