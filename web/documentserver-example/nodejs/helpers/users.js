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

var users = [
    new User("uid-1", "John Smith", "smith@mail.ru", null, null, null, []),
    new User("uid-2", "Mark Pottato", "pottato@mail.ru", "group-2", ["group-2", ""], true, []),  // own and without group
    new User("uid-3", "Hamish Mitchell", "mitchell@mail.ru", "group-3", ["group-2"], false, ["copy", "download", "print"]),  // other group only
    new User("uid-0", null, null, null, null, null, []),
];

function User(id, name, email, group, reviewGroups, favorite, deniedPermissions) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.group = group;
    this.reviewGroups = reviewGroups;
    this.favorite = favorite;
    this.deniedPermissions = deniedPermissions;
};

users.getUser = function (id) {
    var result = null;
    this.forEach(user => {
        if (user.id == id) {
            result = user;
        }
    });
    return result ? result : this[0];
};

module.exports = users;