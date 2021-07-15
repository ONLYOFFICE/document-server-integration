package com.onlyoffice.integration.util.objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.onlyoffice.integration.entities.filemodel.User;

import java.util.List;

public class History {
    @JsonProperty("serverVersion")
    private String serverVersion;
    private String key;
    private Integer version;
    private String created;
    private User user;
    private List<History> changes;

    public History(String serverVersion, String key, Integer version, String created, User user,List<History> changes) {
        this.serverVersion = serverVersion;
        this.key = key;
        this.version = version;
        this.created = created;
        this.user = user;
        this.changes=changes;
    }
    public History(){}

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public void setServerVersion(String serverVersion) {
        this.serverVersion = serverVersion;
    }

    public List<History> getChanges() {
        return changes;
    }

    public void setChanges(List<History> changes) {
        this.changes = changes;
    }
}
