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

var descr_user_1 = [
    "File author by default",
    "He doesn’t belong to any of the groups",
    "He can review all the changes",
    "The file favorite state is undefined"
];

var descr_user_2 = [
    "He belongs to Group2",
    "He can review only his own changes or the changes made by the users who don’t belong to any of the groups",
    "This file is favorite"
];

var descr_user_3 = [
    "He belongs to Group3",
    "He can review only the changes made by the users from Group2",
    "This file isn’t favorite",
    "He can’t copy data from the file into the clipboard",
    "He can’t download the file",
    "He can’t print the file"
];

var descr_user_0 = [
    "The user without a name. The name is requested upon the editor opening",
    "He doesn’t belong to any of the groups",
    "He can review all the changes",
    "The file favorite state is undefined"
];

var users = [
    new User("uid-1", "John Smith", "smith@mail.ru", null, null, null, [], descr_user_1),
    new User("uid-2", "Mark Pottato", "pottato@mail.ru", "group-2", ["group-2", ""], true, [], descr_user_2),  // own and without group
    new User("uid-3", "Hamish Mitchell", "mitchell@mail.ru", "group-3", ["group-2"], false, ["copy", "download", "print"], descr_user_3),  // other group only
    new User("uid-0", null, null, null, null, null, [], descr_user_0),
];

function User(id, name, email, group, reviewGroups, favorite, deniedPermissions, descriptions) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.group = group;
    this.reviewGroups = reviewGroups;
    this.favorite = favorite;
    this.deniedPermissions = deniedPermissions;
    this.descriptions = descriptions;
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

users.getUsersForMentions = function (id) {
    var result = [];
    this.forEach(user => {
        if (user.id != id && user.name != null && user.email != null) {
            result.push({ name: user.name, email: user.email });
        }
    });
    return result;
}

module.exports = users;