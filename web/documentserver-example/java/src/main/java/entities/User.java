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

package entities;

import java.util.List;

public class User {
    public String id;
    public String name;
    public String email;
    public String group;
    public List<String> reviewGroups;
    public Boolean favorite;
    public List<String> deniedPermissions;
    public List<String> descriptions;
    public Boolean templates;

    public User(String id, String name, String email, String group, List<String> reviewGroups, Boolean favorite, List<String> deniedPermissions, List<String> descriptions, Boolean templates) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.group = group;
        this.reviewGroups = reviewGroups;
        this.favorite = favorite;
        this.deniedPermissions = deniedPermissions;
        this.descriptions = descriptions;
        this.templates = templates;
    }
}