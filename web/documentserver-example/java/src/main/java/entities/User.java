/**
 *
 * (c) Copyright Ascensio System SIA 2023
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
    private final String id;
    private final String name;
    private final String email;
    private final String group;
    private final List<String> reviewGroups;
    private final CommentGroups commentGroups;
    private final Boolean favorite;
    private final List<String> deniedPermissions;
    private final List<String> descriptions;
    private final Boolean templates;
    private final List<String> userInfoGroups;

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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getGroup() {
        return group;
    }

    public List<String> getReviewGroups() {
        return reviewGroups;
    }

    public CommentGroups getCommentGroups() {
        return commentGroups;
    }

    public Boolean getFavorite() {
        return favorite;
    }

    public List<String> getDeniedPermissions() {
        return deniedPermissions;
    }

    public List<String> getDescriptions() {
        return descriptions;
    }

    public Boolean getTemplates() {
        return templates;
    }

    public List<String> getUserInfoGroups() {
        return userInfoGroups;
    }
}
