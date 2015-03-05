package com.android.volley.toolbox;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

import com.android.volley.LruCache;
import com.android.volley.Utils;
import com.android.volley.toolbox.ImageLoader.ImageCache;

public class BitmapCache implements ImageCache {
	private static BitmapCache sUniqueInstance = null;
	public LruCache<String, Bitmap> mCache;
	
	public static synchronized BitmapCache getSigleton(Context context) {
		if (sUniqueInstance == null) {
			sUniqueInstance = new BitmapCache(context.getApplicationContext());
		}
		return sUniqueInstance;
	}

	@TargetApi(Build.VERSION_CODES.KITKAT)
    private BitmapCache(Context context) {
        if (mCache == null) {
            int maxSize = getCacheSize(context);
            mCache = new LruCache<String, Bitmap>(maxSize) {
                
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    int size = 1;
                    if (Utils.hasKitKat()) { // API
                        size = bitmap.getAllocationByteCount();
                    } else if (Utils.hasHoneycombMR1()) {// API
                        size = bitmap.getByteCount();
                    } else {
                        size = bitmap.getRowBytes() * bitmap.getHeight();
                    }
//                    LogUtils.d("bacy", key + "<bitmap>" + size);
                    return size;
                }

            };
        }

    }

	@Override
	public Bitmap getBitmap(String url) {
		return mCache.get(url);
	}

	@Override
	public void putBitmap(String url, Bitmap bitmap) {
		mCache.put(url, bitmap);
	}
	
    public final void evictAll() {
        mCache.evictAll();
    }

	private int getCacheSize(Context context) {
		int memClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		// Use 1/8th of the available memory for this memory cache.
//		LogUtils.d("bacy", memClass + "" +  1024 * 1024 * memClass / 8);
		return (1024 * 1024 * memClass / 8);
	}

}
