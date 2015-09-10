package com.android.volley.ext.tools;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.Context;
import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.ext.HttpCallback;
import com.android.volley.ext.RequestInfo;
import com.android.volley.toolbox.DownloadRequest;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.MultiPartRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class HttpTools {
    private Context mContext;
    private static RequestQueue sRequestQueue;
    private static RequestQueue sDownloadQueue;     

    public HttpTools(Context context) {
        mContext = context.getApplicationContext();
    }
    
    public static void init(Context context) {
        if (sRequestQueue == null) {
            sRequestQueue = Volley.newNoCacheRequestQueue(context.getApplicationContext());
        }
    }
    
    public static void stop() {
        if (sRequestQueue != null) {
            sRequestQueue.stop();
            sRequestQueue = null;
        }
    }
    
    public static RequestQueue getHttpRequestQueue() {
        return sRequestQueue;
    }
    
    /**
     * get 请求
     * @param url
     * @param paramsMap
     * @param httpResult
     */
    public void get(String url, Map<String, String> paramsMap, final HttpCallback httpResult) {
        get(new RequestInfo(url, paramsMap), httpResult);
    }
    
    /**
     * get 请求
     * get
     * @param requestInfo
     * @param httpResult
     * @since 3.5
     */
    public void get(RequestInfo requestInfo, final HttpCallback httpResult) {
        sendRequest(Request.Method.GET, requestInfo, httpResult);
    }

    /**
     * post请求
     * @param url
     * @param paramsMap
     * @param httpResult
     */
    public void post(final String url, final Map<String, String> paramsMap, final HttpCallback httpResult) {
        post(new RequestInfo(url, paramsMap), httpResult);
    }
    
    /**
     * post请求
     * post
     * @param requestInfo
     * @param httpResult
     * @since 3.5
     */
    public void post(RequestInfo requestInfo, final HttpCallback httpResult) {
        sendRequest(Request.Method.POST, requestInfo, httpResult);
    }
    
    /**
     * delete 请求
     * @param requestInfo
     * @param httpResult
     * @since 3.5
     */
    public void delete(RequestInfo requestInfo, final HttpCallback httpResult) {
        sendRequest(Request.Method.DELETE, requestInfo, httpResult);
    }
    
    /**
     * put 请求
     * @param requestInfo
     * @param httpResult
     * @since 3.5
     */
    public void put(RequestInfo requestInfo, final HttpCallback httpResult) {
        sendRequest(Request.Method.PUT, requestInfo, httpResult);
    }
    

    /**
     * upload 请求
     * 
     * @param url
     * @param paramsMap
     * @param fileParams
     * @param httpResult
     * @since 3.4
     */
    public void upload(final String url, final Map<String, Object> params, final HttpCallback httpResult) {
    	RequestInfo requestInfo = new RequestInfo();
    	requestInfo.url = url;
    	requestInfo.putAllParams(params);
    	upload(requestInfo, httpResult);
    }
    
    /**
     * upload 请求
     * @param requestInfo
     * @param httpResult
     */
    public void upload(final RequestInfo requestInfo, final HttpCallback httpResult) {
    	if (sRequestQueue == null) {
            sRequestQueue = Volley.newNoCacheRequestQueue(mContext);
        }
    	
    	final String url = requestInfo.getUrl();
        if (TextUtils.isEmpty(url)) {
            if (httpResult != null) {
                httpResult.onStart();
                httpResult.onError(new Exception("url can not be empty!"));
                httpResult.onFinish();
            }
            return;
        }
        final Map<String, String> paramsMap = requestInfo.getParams();
        final Map<String, File> fileParams = requestInfo.getFileParams();
        VolleyLog.d("upload->%s\t,file->%s\t,form->%s", url, fileParams, paramsMap);
        if (httpResult != null) {
            httpResult.onStart();
        }
        MultiPartRequest<String> request = new MultiPartRequest<String>(Method.POST, url, new Listener<String>() {

            @Override
            public void onResponse(String response) {
                if (httpResult != null) {
                    httpResult.onResult(response);
                    httpResult.onFinish();
                }
            }
        }, new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (httpResult != null) {
                    httpResult.onError(error);
                    httpResult.onFinish();
                }
            }
        }, new Response.LoadingListener() {

            @Override
            public void onLoading(long count, long current) {
                if (httpResult != null) {
                    httpResult.onLoading(count, current);
                }
            }
        }) {

            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                String parsed;
                try {
                    parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                } catch (UnsupportedEncodingException e) {
                    parsed = new String(response.data);
                } catch (NullPointerException e) {
                    parsed = "";
                }
                return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
            }

            @Override
            public void cancel() {
                super.cancel();
                if (httpResult != null) {
                    httpResult.onCancelled();
                }
            }
            
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Charset", "UTF-8");
                headers.putAll(requestInfo.getHeaders());
                return headers;
            }

        };

        if (paramsMap != null && paramsMap.size() != 0) {
            for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
                request.addPart(entry.getKey(), entry.getValue());
            }
        }
        if (fileParams != null && fileParams.size() != 0) {
            for (Map.Entry<String, File> entry : fileParams.entrySet()) {
                String key = entry.getKey();
                int index = key.indexOf(requestInfo.boundary);
                if (index > 0) {
                	key = key.substring(0, index);
                }
                request.addPart(key, entry.getValue());
            }
        }
        request.setTag(this);
        sRequestQueue.add(request);
    }
    
    public DownloadRequest download(String url, String target, final boolean isResume, final HttpCallback httpResult) {
    	RequestInfo requestInfo = new RequestInfo();
    	requestInfo.url = url;
    	return download(requestInfo, target, isResume, httpResult);
    }
    
    public DownloadRequest download(final RequestInfo requestInfo, String target, final boolean isResume, final HttpCallback httpResult) {
    	final String url = requestInfo.getFullUrl();
    	VolleyLog.d("download->%s", url);
        DownloadRequest request = new DownloadRequest(url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                if (httpResult != null) {
                    httpResult.onResult(response);
                    httpResult.onFinish();
                }
            }
        }, new Response.ErrorListener(){

            @Override
            public void onErrorResponse(VolleyError error) {
                if (httpResult != null) {
                    httpResult.onError(error);
                    httpResult.onFinish();
                }
            }
            
        }, new Response.LoadingListener() {

            @Override
            public void onLoading(long count, long current) {
                if (httpResult != null) {
                    httpResult.onLoading(count, current);
                }
            }
        }) {
            @Override
            public void stopDownload() {
                super.stopDownload();
                if (httpResult != null) {
                    httpResult.onCancelled();
                }
            }
            
            @Override
            public void cancel() {
                super.cancel();
                if (httpResult != null) {
                    httpResult.onCancelled();
                }
            }
            
            
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
            	Map<String, String> headers = super.getHeaders();
            	if (headers != null) {
            		headers.putAll(requestInfo.getHeaders());
            	} else {
            		headers = requestInfo.getHeaders();
            	}
                return headers;
            }
        };
        request.setResume(isResume);
        request.setTarget(target);
        if (httpResult != null) {
            httpResult.onStart();
        }
        if (TextUtils.isEmpty(url)) {
            if (httpResult != null) {
                httpResult.onError(new Exception("url can not be empty!"));
                httpResult.onFinish();
            }
            return request;
        }
        if (sDownloadQueue == null) {
            sDownloadQueue = Volley.newNoCacheRequestQueue(mContext);
        }
        sDownloadQueue.add(request);
        return request;
    
    }
    
    /**
     * 发送http请求
     * @param request
     */
    public <T> void sendRequest(Request<T> request) {
    	if (sRequestQueue == null) {
            sRequestQueue = Volley.newNoCacheRequestQueue(mContext);
        }
    	request.setTag(this);
        sRequestQueue.add(request);
    }
    
    private void sendRequest(final int method, final RequestInfo requestInfo, final HttpCallback httpResult) {
        if (sRequestQueue == null) {
            sRequestQueue = Volley.newNoCacheRequestQueue(mContext);
        }
        if (httpResult != null) {
            httpResult.onStart();
        }
        if (requestInfo == null || TextUtils.isEmpty(requestInfo.url)) {
            if (httpResult != null) {
                httpResult.onError(new Exception("url can not be empty!"));
                httpResult.onFinish();
            }
            return;
        }
        switch (method) {
        case Request.Method.GET:
            requestInfo.url = requestInfo.getFullUrl();
            VolleyLog.d("get->%s", requestInfo.getUrl());
            break;
        case Request.Method.DELETE:
            requestInfo.url = requestInfo.getFullUrl();
            VolleyLog.d("delete->%s", requestInfo.getUrl());
            break;

        default:
            break;
        }
        final StringRequest request = new StringRequest(method, requestInfo.url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                if (httpResult != null) {
                    httpResult.onResult(response);
                    httpResult.onFinish();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) { 
                if (httpResult != null) {
                    httpResult.onError(error);
                    httpResult.onFinish();
                }
            }
        }, new Response.LoadingListener() {

            @Override
            public void onLoading(long count, long current) {
                if (httpResult != null) {
                    httpResult.onLoading(count, current);
                }
            }
        }) {

            @Override
            public void cancel() {
                super.cancel();
                if (httpResult != null) {
                    httpResult.onCancelled();
                }
            }
            
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                if (method == Request.Method.POST || method == Request.Method.PUT) {
                    VolleyLog.d((method == Request.Method.POST ? "post->%s" : "put->%s"), requestInfo.getUrl() + ",params->" + requestInfo.getParams().toString());
                    return requestInfo.getParams();
                } 
                return super.getParams();
            }
            
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return requestInfo.getHeaders();
            }
        };
        request.setTag(this);
        sRequestQueue.add(request);
    }
    
    public void cancelAllRequest() {
        if (sRequestQueue != null) {
            sRequestQueue.cancelAll(this);
        }
    }
    
    public void quitDownloadQueue() {
        if (sDownloadQueue != null) {
            sDownloadQueue.stop();
            sDownloadQueue = null;
        }
    }
    
    public Map<String, String> urlEncodeMap(Map<String, String> paramsMap) {
        if (paramsMap != null && !paramsMap.isEmpty()) {
            Iterator<String> iterator = paramsMap.keySet().iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                try {
                    paramsMap.put(key, URLEncoder.encode(paramsMap.get(key), "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return paramsMap;
    }
}
