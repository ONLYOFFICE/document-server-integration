#
# (c) Copyright Ascensio System SIA 2021
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
    attr_accessor :id, :name, :email, :group, :reviewGroups, :commentGroups, :favorite, :deniedPermissions, :descriptions

    def initialize (id, name, email, group, reviewGroups, commentGroups, favorite, deniedPermissions, descriptions)
        @id = id
        @name = name
        @email = email
        @group = group
        @reviewGroups = reviewGroups
        @commentGroups = commentGroups
        @favorite = favorite
        @deniedPermissions = deniedPermissions
        @descriptions = descriptions
    end
end

class Users
    @@descr_user_1 = [
        "File author by default",
        "He doesn’t belong to any of the groups",
        "He can review all the changes",
        "He can do everything with the comments",
        "The file favorite state is undefined"
    ];

    @@descr_user_2 = [
        "He belongs to Group2",
        "He can review only his own changes or the changes made by the users who don’t belong to any of the groups",
        "He can view, edit and delete only his comments and the comments left by the users who don't belong to any of the groups",
        "This file is favorite"
    ];

    @@descr_user_3 = [
        "He belongs to Group3",
        "He can review only the changes made by the users from Group2",
        "He can view, edit and delete only his comments and the comments left by the users from Group2",
        "This file isn’t favorite",
        "He can’t copy data from the file into the clipboard",
        "He can’t download the file",
        "He can’t print the file"
    ];

    @@descr_user_0 = [
        "The user without a name. The name is requested upon the editor opening",
        "He doesn’t belong to any of the groups",
        "He can review all the changes",
        "He can do everything with the comments",
        "The file favorite state is undefined",
        "He cannot mention others in the comments"
    ];

    @@users = [
        User.new("uid-1", "John Smith", "smith@mail.ru",
                nil, nil, {},
                nil, [], @@descr_user_1),
        User.new("uid-2", "Mark Pottato", "pottato@mail.ru",
                "group-2", ["group-2", ""], {
                    :view => ["group-2", ""],
                    :edit => ["group-2", ""],
                    :remove => ["group-2", ""]
                },
                true, [], @@descr_user_2),
        User.new("uid-3", "Hamish Mitchell", "mitchell@mail.ru",
                "group-3", ["group-2"], {
                    :view => ["group-3", "group-2"],
                    :edit => ["group-2"],
                    :remove => ["group-2"]
                },
                false, ["copy", "download", "print"], @@descr_user_3),
        User.new("uid-0", nil, nil,
                nil, nil, {},
                nil, [], @@descr_user_0)
    ]

    class << self
        def get_all_users()
            @@users
        end

        def get_user(id)
            for user in @@users do
                if user.id.eql?(id)
                    return user
                end
            end
            return @@users[0]
        end

        def get_users_for_mentions(id)
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

