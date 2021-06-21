package com.onlyoffice.integration.serializer;

import java.util.List;

public class SerializerFilter {
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof List) {
            if(((List<?>) obj).size() == 1 && ((List<?>) obj).get(0) == FilterState.NULL.toString()){
                return true;
            }
            return false;
        }
        return false;
    }
}
