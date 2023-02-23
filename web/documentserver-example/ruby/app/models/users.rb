#
# (c) Copyright Ascensio System SIA 2023
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

class User
    attr_accessor :id, :name, :email, :group, :reviewGroups, :commentGroups, :userInfoGroups, :favorite, :deniedPermissions, :descriptions, :templates

    def initialize (id, name, email, group, reviewGroups, commentGroups, userInfoGroups, favorite, deniedPermissions, descriptions, templates)
        @id = id
        @name = name
        @email = email
        @group = group
        @reviewGroups = reviewGroups
        @commentGroups = commentGroups
        @favorite = favorite
        @deniedPermissions = deniedPermissions
        @descriptions = descriptions
        @templates = templates
        @userInfoGroups = userInfoGroups
    end
end

class Users
    @@descr_user_1 = [
        "File author by default",
        "Doesn’t belong to any group",
        "Can review all the changes",
        "Can perform all actions with comments",
        "The file favorite state is undefined",
        "Can create files from templates using data from the editor",
        "Can see the information about all users"
    ];

    @@descr_user_2 = [
        "Belongs to Group2",
        "Can review only his own changes or changes made by users with no group",
        "Can view comments, edit his own comments and comments left by users with no group. Can remove his own comments only",
        "This file is marked as favorite",
        "Can create new files from the editor",
        "Can see the information about users from Group2 and users who don’t belong to any group"
    ];

    @@descr_user_3 = [
        "Belongs to Group3",
        "Can review changes made by Group2 users",
        "Can view comments left by Group2 and Group3 users. Can edit comments left by the Group2 users",
        "This file isn’t marked as favorite",
        "Can’t copy data from the file to clipboard",
        "Can’t download the file",
        "Can’t print the file",
        "Can create new files from the editor",
        "Can see the information about Group2 users",
    ];

    @@descr_user_0 = [
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
        "View file without collaboration"
    ];

    @@users = [
        User.new("uid-1", "John Smith", "smith@example.com",
                "", nil, {}, nil,
                nil, [], @@descr_user_1, true),
        User.new("uid-2", "Mark Pottato", "pottato@example.com",
                "group-2", ["group-2", ""], {
                    :view => "",
                    :edit => ["group-2", ""],
                    :remove => ["group-2"]
                },
                 ["group-2", ""],
                true, [], @@descr_user_2, false),
        User.new("uid-3", "Hamish Mitchell", "mitchell@example.com",
                "group-3", ["group-2"], {
                    :view => ["group-3", "group-2"],
                    :edit => ["group-2"],
                    :remove => []
                },
                 ["group-2"],
                false, ["copy", "download", "print"], @@descr_user_3, false),
        User.new("uid-0", nil, nil,
                "", nil, {}, [],
                nil, ["protect"], @@descr_user_0, false)
    ]

    class << self
        def get_all_users()  # get a list of all the users
            @@users
        end

        def get_user(id)  # get a user by id specified
            for user in @@users do
                if user.id.eql?(id)
                    return user
                end
            end
            return @@users[0]
        end

        def get_users_for_mentions(id)  # get a list of users with their names and emails for mentions
            usersData = []
            for user in @@users do
                if (!user.id.eql?(id) && user.name != nil && user.email != nil)
                    usersData.push({:name => user.name, :email => user.email})
                end
            end
            return usersData
        end

    end
end

