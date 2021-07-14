package com.onlyoffice.integration.util.objects;


import com.onlyoffice.integration.Action;

public class ActionObject {
    private String userid;
    private Action type;

    public ActionObject(String userid,Action type){
        this.userid=userid;
        this.type = type;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public Action getType() {
        return type;
    }

    public void setType(Action type) {
        this.type = type;
    }
}
