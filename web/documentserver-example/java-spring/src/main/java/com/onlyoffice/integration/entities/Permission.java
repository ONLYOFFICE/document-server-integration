package com.onlyoffice.integration.entities;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "`permission`")
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private Boolean comment = true;
    private Boolean copy = true;
    private Boolean download = true;
    private Boolean edit = true;
    private Boolean print = true;
    private Boolean fillForms = true;
    private Boolean modifyFilter = true;
    private Boolean modifyContentControl = true;
    private Boolean review = true;
    @ManyToMany
    private List<Group> reviewGroups;
    @ManyToMany
    private List<Group> commentsViewGroups;
    @ManyToMany
    private List<Group> commentsEditGroups;
    @ManyToMany
    private List<Group> commentsRemoveGroups;

    public int getId() {
        return id;
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

    public List<Group> getReviewGroups() {
        return reviewGroups;
    }

    public void setReviewGroups(List<Group> reviewGroups) {
        this.reviewGroups = reviewGroups;
    }

    public List<Group> getCommentsViewGroups() {
        return commentsViewGroups;
    }

    public void setCommentsViewGroups(List<Group> commentsViewGroups) {
        this.commentsViewGroups = commentsViewGroups;
    }

    public List<Group> getCommentsEditGroups() {
        return commentsEditGroups;
    }

    public void setCommentsEditGroups(List<Group> commentsEditGroups) {
        this.commentsEditGroups = commentsEditGroups;
    }

    public List<Group> getCommentsRemoveGroups() {
        return commentsRemoveGroups;
    }

    public void setCommentsRemoveGroups(List<Group> commentsRemoveGroups) {
        this.commentsRemoveGroups = commentsRemoveGroups;
    }
}
