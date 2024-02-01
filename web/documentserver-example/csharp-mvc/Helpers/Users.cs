/**
 *
 * (c) Copyright Ascensio System SIA 2024
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
using System.Linq;

namespace OnlineEditorsExampleMVC.Helpers
{
    public class Users
    {
        static List<string> descr_user_1 = new List<string>()
        {
            "File author by default",
            "Doesn’t belong to any group",
            "Can review all the changes",
            "Can perform all actions with comments",
            "The file favorite state is undefined",
            "Can create files from templates using data from the editor",
            "Can see the information about all users",
            "Has an avatar",
            "Can submit forms"
        };

        static List<string> descr_user_2 = new List<string>()
        {
            "Belongs to Group2",
            "Can review only his own changes or changes made by users with no group",
            "Can view comments, edit his own comments and comments left by users with no group. Can remove his own comments only",
            "This file is marked as favorite",
            "Can create new files from the editor",
            "Can see the information about users from Group2 and users who don’t belong to any group",
            "Has an avatar",
            "Can’t submit forms"
        };

        static List<string> descr_user_3 = new List<string>()
        {
            "Belongs to Group3",
            "Can review changes made by Group2 users",
            "Can view comments left by Group2 and Group3 users. Can edit comments left by the Group2 users",
            "This file isn’t marked as favorite",
            "Can’t copy data from the file to clipboard",
            "Can’t download the file",
            "Can’t print the file",
            "Can create new files from the editor",
            "Can see the information about Group2 users",
            "Can’t submit forms"
        };

        static List<string> descr_user_0 = new List<string>()
        {
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
            "Can’t submit forms"
        };

        private static List<User> users = new List<User>() {
            new User(
                    "uid-1",
                    "John Smith",
                    "smith@example.com",
                    "",
                    null,
                    new Dictionary<string, object>(),
                    null,
                    null,
                    new List<string>(),
                    descr_user_1,
                    true,
                    true
                ),
            new User(
                    "uid-2",
                    "Mark Pottato",
                    "pottato@example.com",
                    "group-2",
                    new List<string>() { "group-2", "" },
                    new Dictionary<string, object>()
                    {
                        { "view",  ""  },
                        { "edit", new List<string>() { "group-2", "" } },
                        { "remove", new List<string>() { "group-2" } }
                    },
                    new List<string>() { "group-2", "" },
                    true,
                    new List<string>(),
                    descr_user_2,
                    false,
                    true
                ),
            new User(
                    "uid-3",
                    "Hamish Mitchell",
                    null,
                    "group-3",
                    new List<string>() { "group-2" },
                    new Dictionary<string,object>()
                    {
                        { "view", new List<string>() { "group-2", "group-3" } },
                        { "edit", new List<string>() { "group-2" } },
                        { "remove", new List<string>() { } }
                    },
                    new List<string>() { "group-2" },
                    false,
                    new List<string>() { "copy", "download", "print" },
                    descr_user_3,
                    false,
                    false
                ),
            new User(
                    "uid-0",
                    null,
                    null,
                    "",
                    null,
                    new Dictionary<string,object>(),
                    new List<string>(),
                    null,
                    new List<string>() { "protect" },
                    descr_user_0,
                    false,
                    false
                )
        };

        // get a user by id specified
        public static User getUser(string id)
        {
            foreach(User user in users)
            {
                if (user.id.Equals(id)) return user;
            }
            return users[0];
        }

        // get a list of all the users
        public static List<User> getAllUsers()
        {
            return users;
        }

        // get a list of users with their names and emails for mentions
        public static List<Dictionary<string, object>> getUsersForMentions(string id)
        {
            List<Dictionary<string, object>> usersData = new List<Dictionary<string, object>>();
            foreach (User user in users)
            {
                if (!user.id.Equals(id) && user.name != null && user.email != null)
                {
                    usersData.Add(new Dictionary<string, object>()
                    {
                        {"name", user.name },
                        {"email", user.email }
                    });
                }
            }
            return usersData;
        }

        public static List<Dictionary<string, object>> getUsersInfo(string id)
        {
            List<Dictionary<string, object>> usersData = new List<Dictionary<string, object>>();
            if (id != "uid-0") {
                foreach (User user in users)
                {
                    usersData.Add(new Dictionary<string, object>()
                        {
                            {"id", user.id},
                            {"name", user.name },
                            {"email", user.email },
                            {"image", user.avatar ? DocManagerHelper.GetServerUrl(false) + "/Content/images/" + user.id + ".png" : null}
                        });
                }
            }
            return usersData;
        }

        // get a list of users with their names and emails for protect
        public static List<Dictionary<string, object>> getUsersForProtect(string id)
        {
            List<Dictionary<string, object>> usersData = new List<Dictionary<string, object>>();
            foreach (User user in users)
            {
                if (!user.id.Equals(id) && user.name != null)
                {
                    usersData.Add(new Dictionary<string, object>()
                    {
                        {"name", user.name },
                        {"email", user.email },
                        {"id", user.id}
                    });
                }
            }
            return usersData;
        }
    }

    public class User
    {
        public string id;
        public string name;
        public string email;
        public string group;
        public List<string> reviewGroups;
        public Dictionary<string, object> commentGroups;
        public bool? favorite;
        public List<string> deniedPermissions;
        public List<string> descriptions;
        public bool templates;
        public List<string> userInfoGroups;
        public bool avatar;

        public User(string id, string name, string email, string group, List<string> reviewGroups, Dictionary<string, object> commentGroups, List<string> userInfoGroups, bool? favorite, List<string> deniedPermissions, List<string> descriptions, bool templates, bool avatar)
        {
            this.id = id;
            this.name = name;
            this.email = email;
            this.group = group;
            this.reviewGroups = reviewGroups;
            this.commentGroups = commentGroups;
            this.favorite = favorite;
            this.deniedPermissions = deniedPermissions;
            this.descriptions = descriptions;
            this.templates = templates;
            this.userInfoGroups = userInfoGroups;
            this.avatar = avatar;
        }
    }
}
