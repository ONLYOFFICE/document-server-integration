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
    function __construct($id, $name, $email, $group, $reviewGroups, $favorite, $deniedPermissions)
    {
        $this->id = $id;
        $this->name = $name;
        $this->email = $email;
        $this->group = $group;
        $this->reviewGroups = $reviewGroups;
        $this->favorite = $favorite;
        $this->deniedPermissions = $deniedPermissions;
    }
}

$users = [
    new User("uid-1", "John Smith", "smith@mial.ru", null, null, null, []),
    new User("uid-2", "Mark Pottato", "pottato@mial.ru", "group-2", ["group-2", ""], true, []),
    new User("uid-3", "Hamish Mitchell", "mitchell@mial.ru", "group-3", ["group-2"], false, ["copy", "download", "print"]),
    new User("uid-0", null, null, null, null, null, [])
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

?>