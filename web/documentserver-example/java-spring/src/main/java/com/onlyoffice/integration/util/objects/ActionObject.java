package com.onlyoffice.integration.util.objects;

import com.onlyoffice.integration.Action;

public class ActionObject {
    private String userid;
    private String type;

    public ActionObject(String userid,String type){
        this.userid=userid;
        this.type = type;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
