package com.onlyoffice.integration.controllers.objects;

public class UserForMention {
    private String name;
    private String email;

    public UserForMention(String name,String email){
        this.email=email;
        this.name=name;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
