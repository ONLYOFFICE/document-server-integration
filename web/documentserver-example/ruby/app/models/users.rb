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
    attr_accessor :id, :name, :email, :group, :reviewGroups

    def initialize (id, name, email, group, reviewGroups)
        @id = id
        @name = name
        @email = email
        @group = group
        @reviewGroups = reviewGroups
    end
end

class Users
    @@users = [
        User.new("uid-1", "John Smith", "smith@mail.ru", nil, nil),
        User.new("uid-2", "Mark Pottato", "pottato@mail.ru", "group-2", ["group-2", ""]),
        User.new("uid-3", "Hamish Mitchell", "mitchell@mail.ru", "group-3", ["group-2"]),
        User.new("uid-0", nil, nil, nil, nil)
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
    end
end

