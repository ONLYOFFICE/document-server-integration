<?php
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

class User {
    function __construct($id, $name, $email, $group, $reviewGroups, $favorite, $deniedPermissions, $descriptions, $templates)
    {
        $this->id = $id;
        $this->name = $name;
        $this->email = $email;
        $this->group = $group;
        $this->reviewGroups = $reviewGroups;
        $this->favorite = $favorite;
        $this->deniedPermissions = $deniedPermissions;
        $this->descriptions = $descriptions;
        $this->templates = $templates;
    }
}

$descr_user_1 = [
    "File author by default",
    "He doesn’t belong to any of the groups",
    "He can review all the changes",
    "The file favorite state is undefined",
    "Can create a file from a template with data from the editor"
];

$descr_user_2 = [
    "He belongs to Group2",
    "He can review only his own changes or the changes made by the users who don’t belong to any of the groups",
    "This file is favorite",
    "Can create a file from an editor"
];

$descr_user_3 = [
    "He belongs to Group3",
    "He can review only the changes made by the users from Group2",
    "This file isn’t favorite",
    "He can’t copy data from the file into the clipboard",
    "He can’t download the file",
    "He can’t print the file",
    "Can create a file from an editor"
];

$descr_user_0 = [
    "The user without a name. The name is requested upon the editor opening",
    "He doesn’t belong to any of the groups",
    "He can review all the changes",
    "The file favorite state is undefined",
    "He cannot mention others in the comments",
    "Can't create file from editor"
];

$users = [
    new User("uid-1", "John Smith", "smith@mial.ru", null, null, null, [], $descr_user_1, true),
    new User("uid-2", "Mark Pottato", "pottato@mial.ru", "group-2", ["group-2", ""], true, [], $descr_user_2, false),
    new User("uid-3", "Hamish Mitchell", "mitchell@mial.ru", "group-3", ["group-2"], false, ["copy", "download", "print"], $descr_user_3, false),
    new User("uid-0", null, null, null, null, null, [], $descr_user_0, false)
];

function getAllUsers() {
    global $users;
    return $users;
}

function getUser($id) {
    global $users;
    foreach ($users as $user){
        if ($user->id == $id) {
            sendlog("User ". $user->id, "common.log");
            return $user;
        }
    }
    return $users[0];
}

function getUsersForMentions($id) {
    global $users;
    $usersData = [];
    foreach ($users as $user) {
        if ($user->id != $id && $user->name != null && $user->email != null) {
            array_push($usersData,[
                "name" => $user->name,
                "email" => $user->email
            ]);
        }
    }
    return $usersData;
}
?>