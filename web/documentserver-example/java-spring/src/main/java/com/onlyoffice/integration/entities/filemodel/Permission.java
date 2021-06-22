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

package com.onlyoffice.integration.entities.filemodel;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.onlyoffice.integration.serializer.SerializerFilter;
import com.onlyoffice.integration.entities.Group;
import com.onlyoffice.integration.entities.User;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class Permission {
    private Boolean comment = true;
    private Boolean copy = true;
    private Boolean download = true;
    private Boolean edit = true;
    private Boolean print = true;
    private Boolean fillForms = true;
    private Boolean modifyFilter = true;
    private Boolean modifyContentControl = true;
    private Boolean review = true;
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = SerializerFilter.class)
    private List<String> reviewGroups;
    private CommentGroup commentGroups;

    public void configure(User user){
        this.comment = user.getPermissions().getComment();
        this.copy = user.getPermissions().getCopy();
        this.download = user.getPermissions().getDownload();
        this.edit = user.getPermissions().getEdit();
        this.print = user.getPermissions().getPrint();
        this.fillForms = user.getPermissions().getFillForms();
        this.modifyFilter = user.getPermissions().getModifyFilter();
        this.modifyContentControl = user.getPermissions().getModifyContentControl();
        this.review = user.getPermissions().getReview();
        this.reviewGroups = user.getPermissions()
                    .getReviewGroups()
                    .stream()
                    .distinct()
                    .map(group -> group.getName())
                    .collect(Collectors.toList());

        List<Group> commentViewGroups = user.getPermissions().getCommentsViewGroups();
        List<Group> commentEditGroups = user.getPermissions().getCommentsEditGroups();
        List<Group> commentRemoveGroups = user.getPermissions().getCommentsRemoveGroups();

        this.commentGroups = new CommentGroup(commentViewGroups, commentEditGroups, commentRemoveGroups);
    }

    public Boolean getComment() {
        return comment;
    }

    public void setComment(Boolean comment) {
        this.comment = comment;
    }

    public Boolean getCopy() {
        return copy;
    }

    public void setCopy(Boolean copy) {
        this.copy = copy;
    }

    public Boolean getDownload() {
        return download;
    }

    public void setDownload(Boolean download) {
        this.download = download;
    }

    public Boolean getEdit() {
        return edit;
    }

    public void setEdit(Boolean edit) {
        this.edit = edit;
    }

    public Boolean getPrint() {
        return print;
    }

    public void setPrint(Boolean print) {
        this.print = print;
    }

    public Boolean getFillForms() {
        return fillForms;
    }

    public void setFillForms(Boolean fillForms) {
        this.fillForms = fillForms;
    }

    public Boolean getModifyFilter() {
        return modifyFilter;
    }

    public void setModifyFilter(Boolean modifyFilter) {
        this.modifyFilter = modifyFilter;
    }

    public Boolean getModifyContentControl() {
        return modifyContentControl;
    }

    public void setModifyContentControl(Boolean modifyContentControl) {
        this.modifyContentControl = modifyContentControl;
    }

    public Boolean getReview() {
        return review;
    }

    public void setReview(Boolean review) {
        this.review = review;
    }

    public List<String> getReviewGroups() {
        return reviewGroups;
    }

    public void setReviewGroups(List<String> reviewGroups) {
        this.reviewGroups = reviewGroups;
    }

    public CommentGroup getCommentGroups() {
        return commentGroups;
    }

    public void setCommentGroups(CommentGroup commentGroups) {
        this.commentGroups = commentGroups;
    }
}
