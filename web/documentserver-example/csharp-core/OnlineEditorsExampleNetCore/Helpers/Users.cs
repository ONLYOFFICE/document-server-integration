using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace OnlineEditorsExampleNetCore.Helpers
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
            "Can create files from templates using data from the editor"
        };

        static List<string> descr_user_2 = new List<string>()
        {
            "Belongs to Group2",
            "Can review only his own changes or changes made by users with no group",
            "Can view comments, edit his own comments and comments left by users with no group. Can remove his own comments only",
            "This file is marked as favorite",
            "Can create new files from the editor"
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
            "Can create new files from the editor"
        };

        static List<string> descr_user_0 = new List<string>()
        {
            "The name is requested when the editor is opened",
            "Doesn’t belong to any group",
            "Can review all the changes",
            "Can perform all actions with comments",
            "The file favorite state is undefined",
            "Can't mention others in comments",
            "Can't create new files from the editor"
        };

        private static List<User> users = new List<User>() {
            new User(
                    "uid-1",
                    "John Smith",
                    "smith@mail.ru",
                    null,
                    null,
                    new Dictionary<string, object>(),
                    null,
                    new List<string>(),
                    descr_user_1,
                    true
                ),
            new User(
                    "uid-2",
                    "Mark Pottato",
                    "pottato@mail.ru",
                    "group-2",
                    new List<string>() { "group-2", "" },
                    new Dictionary<string, object>()
                    {
                        { "view",  ""  },
                        { "edit", new List<string>() { "group-2", "" } },
                        { "remove", new List<string>() { "group-2" } }
                    },
                    true,
                    new List<string>(),
                    descr_user_2,
                    false
                ),
            new User(
                    "uid-3",
                    "Hamish Mitchell",
                    "mitchell@mail.ru",
                    "group-3",
                    new List<string>() { "group-2" },
                    new Dictionary<string,object>()
                    {
                        { "view", new List<string>() { "group-2", "group-3" } },
                        { "edit", new List<string>() { "group-2" } },
                        { "remove", new List<string>() { } }
                    },
                    false,
                    new List<string>() { "copy", "download", "print" },
                    descr_user_3,
                    false
                ),
            new User(
                    "uid-0",
                    null,
                    null,
                    null,
                    null,
                    new Dictionary<string,object>(),
                    null,
                    new List<string>(),
                    descr_user_0,
                    false
                )
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
                        {"email", user.email },
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

        public User(string id, string name, string email, string group, List<string> reviewGroups, Dictionary<string, object> commentGroups, bool? favorite, List<string> deniedPermissions, List<string> descriptions, bool templates)
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
        }
    }
}
