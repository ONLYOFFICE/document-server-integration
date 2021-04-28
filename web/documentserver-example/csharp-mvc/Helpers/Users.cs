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

using System.Collections.Generic;

namespace OnlineEditorsExampleMVC.Helpers
{
    public class Users
    {
        static List<string> descr_user_1 = new List<string>()
        {
            "File author by default",
            "He doesn’t belong to any of the groups",
            "He can review all the changes",
            "The file favorite state is undefined"
        };

        static List<string> descr_user_2 = new List<string>()
        {
            "He belongs to Group2",
            "He can review only his own changes or the changes made by the users who don’t belong to any of the groups",
            "This file is favorite"
        };

        static List<string> descr_user_3 = new List<string>()
        {
            "He belongs to Group3",
            "He can review only the changes made by the users from Group2",
            "This file isn’t favorite",
            "He can’t copy data from the file into the clipboard",
            "He can’t download the file",
            "He can’t print the file"
        };

        static List<string> descr_user_0 = new List<string>()
        {
            "The user without a name. The name is requested upon the editor opening",
            "He doesn’t belong to any of the groups",
            "He can review all the changes",
            "The file favorite state is undefined"
        };

        private static List<User> users = new List<User>() {
            new User("uid-1", "John Smith", "smith@mail.ru", null, null, null, new List<string>(), descr_user_1),
            new User("uid-2", "Mark Pottato", "pottato@mail.ru", "group-2", new List<string>() { "group-2", "" }, true, new List<string>(), descr_user_2),
            new User("uid-3", "Hamish Mitchell", "mitchell@mail.ru", "group-3", new List<string>() { "group-2" }, false, new List<string>() { "copy", "download", "print" }, descr_user_3),
            new User("uid-0", null, null, null, null, null, new List<string>(), descr_user_0)
        };
        
        public static User getUser(string id)
        {
            foreach(User user in users)
            {
                if (user.id.Equals(id)) return user;
            }
            return users[0];
        }

        public static List<User> getAllUsers()
        {
            return users;
        }
    }

    public class User
    {
        public string id;
        public string name;
        public string email;
        public string group;
        public List<string> reviewGroups;
        public bool? favorite;
        public List<string> deniedPermissions;
        public List<string> descriptions;

        public User(string id, string name, string email, string group, List<string> reviewGroups, bool? favorite, List<string> deniedPermissions, List<string> descriptions)
        {
            this.id = id;
            this.name = name;
            this.email = email;
            this.group = group;
            this.reviewGroups = reviewGroups;
            this.favorite = favorite;
            this.deniedPermissions = deniedPermissions;
            this.descriptions = descriptions;
        }

    }
}