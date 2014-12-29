package com.android.volley.ext;

public interface HttpCallback {
	public void onStart();
	public void onFinish();
	public void onResult(String string);
	public void onError(Exception e); 
	public void onCancelled();
	public void onLoading(long count, long current);
}
