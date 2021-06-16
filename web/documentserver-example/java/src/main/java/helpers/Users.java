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

package helpers;

import entities.User;

import java.util.*;

public class Users {

    static List<String> descr_user_1 = new ArrayList<String>() {{
        add("File author by default");
        add("He doesn’t belong to any of the groups");
        add("He can review all the changes");
        add("The file favorite state is undefined");
        add("Can create a file from a template with data from the editor");
    }};

    static List<String> descr_user_2 = new ArrayList<String>() {{
        add("He belongs to Group2");
        add("He can review only his own changes or the changes made by the users who don’t belong to any of the groups");
        add("This file is favorite");
        add("Can create a file from an editor");
    }};

    static List<String> descr_user_3 = new ArrayList<String>() {{
        add("He belongs to Group3");
        add("He can review only the changes made by the users from Group2");
        add("This file isn’t favorite");
        add("He can’t copy data from the file into the clipboard");
        add("He can’t download the file");
        add("He can’t print the file");
        add("Can create a file from an editor");
    }};

    static List<String> descr_user_0 = new ArrayList<String>() {{
        add("The user without a name. The name is requested upon the editor opening");
        add("He doesn’t belong to any of the groups");
        add("He can review all the changes");
        add("The file favorite state is undefined");
        add("He cannot mention others in the comments");
        add("Can't create file from editor");
    }};

    private static List<User> users = new ArrayList<User>() {{
        add(new User("uid-1", "John Smith", "smith@mail.ru", null, null, null, new ArrayList<String>(), descr_user_1, true));
        add(new User("uid-2", "Mark Pottato", "pottato@mail.ru", "group-2", Arrays.asList("group-2", ""), true, new ArrayList<String>(), descr_user_2, false));
        add(new User("uid-3", "Hamish Mitchell", "mitchell@mail.ru", "group-3", Arrays.asList("group-2"), false, Arrays.asList("copy", "download", "print"), descr_user_3, false));
        add(new User("uid-0", null, null, null, null, null, new ArrayList<String>(), descr_user_0, false));
    }};

    public static User getUser (String id) {
        for (User user : users) {
            if (user.id.equals(id)) {
                return user;
            }
        }
        return users.get(0);
    }

    public static List<User> getAllUsers () {
        return users;
    }

    public static List<Map<String, Object>> getUsersForMentions (String id) {
        List<Map<String, Object>> usersData = new ArrayList<>();
        for (User user : users) {
            if (!user.id.equals(id) && user.name != null && user.email != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("name", user.name);
                data.put("email", user.email);
                usersData.add(data);
            }
        }
        return usersData;
    }
}

