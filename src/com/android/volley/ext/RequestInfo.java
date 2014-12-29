package com.android.volley.ext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RequestInfo {
	
	public String url ;
	public Map<String,String> params = new HashMap<String, String>() ;
	public Map<String, String> headers = new HashMap<String, String>();
	
    public RequestInfo() {
    }

    public RequestInfo(String url, Map<String, String> params) {
        this.url = url;
        this.params = params;
    }
    

    public String getFullUrl() {
        if (url != null && params != null) {
            StringBuilder sb = new StringBuilder();
            if (!url.contains("?")) {
                url = url + "?";
            } else {
                if (!url.endsWith("?")) {
                    url = url + "&";
                }
            }
            Iterator<String> iterotor = params.keySet().iterator();
            while (iterotor.hasNext()) {
                String key = (String) iterotor.next();
                if (key != null) {
                    sb.append(key).append("=").append(params.get(key)).append("&");
                }
            }
            if (sb.lastIndexOf("&") == sb.length() - 1) {
                sb.deleteCharAt(sb.length() - 1);
            }
            return url + sb.toString();
        }
        return url;
    }
	
}
