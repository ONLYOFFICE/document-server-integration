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

package helpers;

import entities.CommentGroups;
import entities.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Users {

    private static List<String> descriptionUserFirst = new ArrayList<String>() {{
        add("File author by default");
        add("Doesn’t belong to any group");
        add("Can review all the changes");
        add("Can perform all actions with comments");
        add("The file favorite state is undefined");
        add("Can create files from templates using data from the editor");
        add("Can see the information about all users");
    }};

    private static List<String> descriptionUserSecond = new ArrayList<String>() {{
        add("Belongs to Group2");
        add("Can review only his own changes or changes made by users with no group");
        add("Can view comments, edit his own comments and comments left by users with no group."
                + " Can remove his own comments only");
        add("This file is marked as favorite");
        add("Can create new files from the editor");
        add("Can see the information about users from Group2 and users who don’t belong to any group");
    }};

    private static List<String> descriptionUserThird = new ArrayList<String>() {{
        add("Belongs to Group3");
        add("Can review changes made by Group2 users");
        add("Can view comments left by Group2 and Group3 users. Can edit comments left by the Group2 users");
        add("This file isn’t marked as favorite");
        add("Can’t copy data from the file to clipboard");
        add("Can’t download the file");
        add("Can’t print the file");
        add("Can create new files from the editor");
        add("Can see the information about Group2 users");
    }};

    private static List<String> descriptionUserZero = new ArrayList<String>() {{
        add("The name is requested when the editor is opened");
        add("Doesn’t belong to any group");
        add("Can review all the changes");
        add("Can perform all actions with comments");
        add("The file favorite state is undefined");
        add("Can't mention others in comments");
        add("Can't create new files from the editor");
        add("Can’t see anyone’s information");
        add("Can't rename files from the editor");
        add("Can't view chat");
        add("Can't protect file");
        add("View file without collaboration");
    }};

    private static List<User> users = new ArrayList<User>() {{
        add(new User("uid-1", "John Smith", "smith@example.com",
                "", null, new CommentGroups(), null,
                null, new ArrayList<String>(), descriptionUserFirst, true));
        add(new User("uid-2", "Mark Pottato", "pottato@example.com",
                "group-2", Arrays.asList("group-2", ""), new CommentGroups(null,
                Arrays.asList("group-2", ""), Arrays.asList("group-2")), Arrays.asList("group-2", ""),
                true, new ArrayList<String>(), descriptionUserSecond, false));
        add(new User("uid-3", "Hamish Mitchell", "mitchell@example.com",
                "group-3", Arrays.asList("group-2"), new CommentGroups(Arrays.asList("group-3", "group-2"),
                Arrays.asList("group-2"), null), Arrays.asList("group-2"),
                false, Arrays.asList("copy", "download", "print"),
                descriptionUserThird, false));
        add(new User("uid-0", null, null,
                "", null, null, null,
                null, Arrays.asList("protect"), descriptionUserZero, false));
    }};

    private Users() { }

    // get a user by id specified
    public static User getUser(final String id) {
        for (User user : users) {
            if (user.getId().equals(id)) {
                return user;
            }
        }
        return users.get(0);
    }

    // get a list of all the users
    public static List<User> getAllUsers() {
        return users;
    }

    // get a list of users with their names and emails for mentions
    public static List<Map<String, Object>> getUsersForMentions(final String id) {
        List<Map<String, Object>> usersData = new ArrayList<>();
        for (User user : users) {
            if (!user.getId().equals(id) && user.getName() != null && user.getEmail() != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("name", user.getName());
                data.put("email", user.getEmail());
                usersData.add(data);
            }
        }
        return usersData;
    }
}

