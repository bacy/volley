package com.android.volley.ext;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.android.volley.VolleyLog;

public class RequestInfo {
	
	public final String boundary = String.valueOf(System.currentTimeMillis());
	
	public String url ;
	public Map<String,String> params = new HashMap<String, String>() ;
	public Map<String, String> headers = new HashMap<String, String>();
	public Map<String, File> fileParams = new HashMap<String, File>();
	
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
            try {
                while (iterotor.hasNext()) {
                    String key = (String) iterotor.next();
                    if (key != null) {
                        if (params.get(key) != null) {
                            sb.append(URLEncoder.encode(key, "utf-8")).append("=")
                                    .append(URLEncoder.encode(params.get(key), "utf-8")).append("&");
                        }
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (sb.length() > 0 && sb.lastIndexOf("&") == sb.length() - 1) {
                sb.deleteCharAt(sb.length() - 1);
            }
            return url + sb.toString();
        }
        return url;
    }
    
    public String getUrl() {
        return url;
    }

    public Map<String, String> getParams() {
        return params;
    }
    
    public Map<String, File> getFileParams() {
        return fileParams;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
	
    
    public void put(String key, String value) {
    	params.put(key, value);
    }
    
    public void put(String key, File file) {
    	if (fileParams.containsKey(key)) {
    		fileParams.put(key + boundary + fileParams.size(), file);
    	} else {
    		fileParams.put(key, file);
    	}
    }
    
    public void putFile(String key, String path) {
    	if (fileParams.containsKey(key)) {
    		fileParams.put(key + boundary + fileParams.size(), new File(path));
    	} else {
    		fileParams.put(key, new File(path));
    	}
    }
    
	public void putAllParams(Map<String, Object> objectParams) {
		for (String key : objectParams.keySet()) {
			Object value = objectParams.get(key);
			if (value instanceof String) {
				put(key, (String) value);
			} else if (value instanceof File) {
				put(key, (File) value);
			}
		}
	}
}
