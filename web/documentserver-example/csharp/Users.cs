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

namespace OnlineEditorsExample
{
    public class Users
    {
        private static List<User> users = new List<User>() {
            new User("uid-1", "John Smith", "smith@mail.ru", null, null),
            new User("uid-2", "Mark Pottato", "pottato@mail.ru", "group-2", new List<string>() { "group-2", "" }),
            new User("uid-3", "Hamish Mitchell", "mitchell@mail.ru", "group-3", new List<string>() { "group-2" }),
            new User("uid-0", null, null, null, null)
        };

        public static User getUser(string id)
        {
            foreach (User user in users)
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

        public User(string id, string name, string email, string group, List<string> reviewGroups)
        {
            this.id = id;
            this.name = name;
            this.email = email;
            this.group = group;
            this.reviewGroups = reviewGroups;
        }

    }
}