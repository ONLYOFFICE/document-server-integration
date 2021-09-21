package com.onlyoffice.integration.documentserver.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class Misc {
    public String convertUserDescriptions(String username, List<String> description){
        String result = "<b>"+username+"</b><br/>"+description.
                stream().map(text -> "<li>"+text+"</li>")
                .collect(Collectors.joining());
        return result;
    }
}
