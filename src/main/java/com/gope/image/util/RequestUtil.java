package com.gope.image.util;

import java.util.HashMap;
import java.util.Map;

public class RequestUtil {
    private RequestUtil() {
    }

    public static Map<String, String> buildHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
        headers.put("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");
        return headers;
    }
}
