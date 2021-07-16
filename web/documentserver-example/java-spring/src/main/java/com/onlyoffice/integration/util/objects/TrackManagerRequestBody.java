package com.onlyoffice.integration.util.objects;

import java.util.List;

public class TrackManagerRequestBody {
    private String url;
    private String key;
    private String changesurl;
    private History history;
    private List<History> changeshistory;
    private String token;
    private Integer forcesavetype;
    private Integer status;
    private List<String> users;
    private List<ActionObject> actions;
    private String userdata;
    private String lastsave;
    private Boolean notmodified;


    public TrackManagerRequestBody(){}

    public TrackManagerRequestBody(String url, String key, String changesurl, List<History> changeshistory, History history, String token,
                                   Integer forcesavetype, Integer status, List<ActionObject> actions, List<String> users, String userdata, String lastsave, Boolean notmodified){
        this.actions=actions;
        this.changeshistory=changeshistory;
        this.changesurl=changesurl;
        this.forcesavetype=forcesavetype;
        this.history=history;
        this.key=key;
        this.status=status;
        this.token=token;
        this.url=url;
        this.users=users;
        this.userdata = userdata;
        this.lastsave = lastsave;
        this.notmodified = notmodified;
    }
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getChangesurl() {
        return changesurl;
    }

    public void setChangesurl(String changesurl) {
        this.changesurl = changesurl;
    }

    public History getHistory() {
        return history;
    }

    public void setHistory(History history) {
        this.history = history;
    }

    public List<History> getChangeshistory() {
        return changeshistory;
    }

    public void setChangeshistory(List<History> changeshistory) {
        this.changeshistory = changeshistory;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Integer getForcesavetype() {
        return forcesavetype;
    }

    public void setForcesavetype(Integer forcesavetype) {
        this.forcesavetype = forcesavetype;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String > users) {
        this.users = users;
    }

    public List<ActionObject> getActions() {
        return actions;
    }

    public void setActions(List<ActionObject> actions) {
        this.actions = actions;
    }

    public String getUserdata() {
        return userdata;
    }

    public void setUserdata(String userdata) {
        this.userdata = userdata;
    }

    public Boolean getNotmodified() {
        return notmodified;
    }

    public void setNotmodified(Boolean notmodified) {
        this.notmodified = notmodified;
    }

    public String getLastsave() {
        return lastsave;
    }

    public void setLastsave(String lastsave) {
        this.lastsave = lastsave;
    }
}
