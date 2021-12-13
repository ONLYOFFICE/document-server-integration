package com.onlyoffice.integration.documentserver.util;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class Misc {
    public String convertUserDescriptions(String username, List<String> description){  // cenvert user descriptions to the specified format
        String result = "<div class=\"user-descr\"><b>"+username+"</b><br/><ul>"+description.
                stream().map(text -> "<li>"+text+"</li>")
                .collect(Collectors.joining()) + "</ul></div>";
        return result;
    }
}
