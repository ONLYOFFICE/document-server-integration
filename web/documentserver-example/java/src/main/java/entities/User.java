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
    public CommentGroups commentGroups;
    public Boolean favorite;
    public List<String> deniedPermissions;
    public List<String> descriptions;
    public Boolean templates;
    public List<String> userInfoGroups;

    public User(final String idParam, final String nameParam, final String emailParam, final String groupParam,
                final List<String> reviewGroupsParam, final CommentGroups commentGroupsParam,
                final List<String> userInfoGroupsParam, final Boolean favoriteParam,
                final List<String> deniedPermissionsParam, final List<String> descriptionsParam,
                final Boolean templatesParam) {
        this.id = idParam;
        this.name = nameParam;
        this.email = emailParam;
        this.group = groupParam;
        this.reviewGroups = reviewGroupsParam;
        this.commentGroups = commentGroupsParam;
        this.favorite = favoriteParam;
        this.deniedPermissions = deniedPermissionsParam;
        this.descriptions = descriptionsParam;
        this.templates = templatesParam;
        this.userInfoGroups = userInfoGroupsParam;
    }
}
