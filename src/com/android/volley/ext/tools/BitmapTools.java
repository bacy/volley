package com.android.volley.ext.tools;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.animation.Animation;
import android.widget.ImageView;

import com.android.volley.RequestQueue;
import com.android.volley.Response.LoadingListener;
import com.android.volley.VolleyError;
import com.android.volley.ext.display.IDisplayer;
import com.android.volley.ext.display.SimpleDisplayer;
import com.android.volley.ext.tools.BitmapTools.BitmapDisplayConfig.RoundConfig;
import com.android.volley.toolbox.BitmapCache;
import com.android.volley.toolbox.BitmapDecoder;
import com.android.volley.toolbox.ClearCacheRequest;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
/**
 * 图片加载工具类
 * BitmapTools
 * chenbo
 * 2014年8月8日 下午4:12:39
 * @version 3.4
 */
public class BitmapTools {
    // TODO i think nobody will you this id ^_^
    private static final int TAG_ID = 0xffffffff;
    private ImageLoader mImageLoader;
    private static RequestQueue sRequestQueue;
    private BitmapDisplayConfig mDisplayConfig;
    private Context mContext;
    private BitmapCache mBitmapCache;
    private IDisplayer mDisplayer;

    private HashMap<String, BitmapDisplayConfig> configMap = new HashMap<String, BitmapDisplayConfig>();
    
    public static void init(Context context) {
        if (sRequestQueue == null) {
            sRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
    }
    
    public static void stop() {
        if (sRequestQueue != null) {
        	sRequestQueue.stop();
        	sRequestQueue = null;
        }
    }

    public BitmapTools(Context context) {
        mContext = context.getApplicationContext();
        mDisplayer = new SimpleDisplayer();
        mBitmapCache = BitmapCache.getSigleton(context.getApplicationContext());
        init(context);
        mImageLoader = new ImageLoader(sRequestQueue, mBitmapCache);
        mDisplayConfig = new BitmapDisplayConfig();

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int defaultWidth = displayMetrics.widthPixels;
        int defaultHeight = displayMetrics.heightPixels;
        mDisplayConfig.bitmapWidth = (int) (defaultWidth * 1.2f);
        mDisplayConfig.bitmapHeight = (int) (defaultHeight * 1.2f);
    }
    
    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    public ImageContainer display(final View view, String uri) {
        return doDisplay(view, uri, mDisplayConfig);
    }
    
    public ImageContainer display(final View view, String uri, BitmapDisplayConfig displayConfig) {
        return doDisplay(view, uri, displayConfig);
    }
    
    public ImageContainer display(int requestWH, final View view, String uri) {
        BitmapDisplayConfig displayConfig = configMap.get(requestWH + "-" + requestWH + "0_0");
        if (displayConfig == null) {
            displayConfig = getDisplayConfig();
            displayConfig.bitmapWidth = requestWH;
            displayConfig.bitmapHeight = requestWH;
            configMap.put(requestWH + "-" + requestWH + "0_0", displayConfig);
        }
        return doDisplay(view, uri, displayConfig);
    }
    
    public ImageContainer display(int requestWidth, int requestHeight, final View view, String uri) {
        BitmapDisplayConfig displayConfig = configMap.get(requestWidth + "-" + requestHeight + "0_0");
        if (displayConfig == null) {
            displayConfig = getDisplayConfig();
            displayConfig.bitmapWidth = requestWidth;
            displayConfig.bitmapHeight = requestHeight;
            configMap.put(requestWidth + "-" + requestHeight + "0_0", displayConfig);
        }
        return doDisplay(view, uri, displayConfig);
    }
    
    public ImageContainer display(final View view, String uri, int defaultImageResId) {
        BitmapDisplayConfig displayConfig = configMap.get("0-0" + defaultImageResId + "_" + defaultImageResId);
        if (displayConfig == null) {
            displayConfig = getDisplayConfig();
            displayConfig.defaultImageResId = defaultImageResId;
            displayConfig.errorImageResId = defaultImageResId;
            configMap.put("0-0" + defaultImageResId + "_" + defaultImageResId, displayConfig);
        }
        return doDisplay(view, uri, displayConfig);
    }

    public ImageContainer display(final View view, String uri, int defaultImageResId, int errorImageResId) {
        BitmapDisplayConfig displayConfig = configMap.get("0-0" + defaultImageResId + "_" + errorImageResId);
        if (displayConfig == null) {
            displayConfig = getDisplayConfig();
            displayConfig.defaultImageResId = defaultImageResId;
            displayConfig.errorImageResId = errorImageResId;
            configMap.put("0-0" + defaultImageResId + "_" + errorImageResId, displayConfig);
        }
        return doDisplay(view, uri, displayConfig);
    }

    public ImageContainer display(final View view, String uri, int requestWidth, int requestHeight, int defaultImageResId, int errorImageResId) {
        BitmapDisplayConfig displayConfig = configMap.get(requestWidth + "-" + requestHeight + defaultImageResId + "_" + errorImageResId);
        if (displayConfig == null) {
            displayConfig = getDisplayConfig();
            displayConfig.defaultImageResId = defaultImageResId;
            displayConfig.errorImageResId = errorImageResId;
            displayConfig.bitmapWidth = requestWidth;
            displayConfig.bitmapHeight = requestHeight;
            configMap.put(requestWidth + "-" + requestHeight + defaultImageResId + "_" + errorImageResId, displayConfig);
        }
        return doDisplay(view, uri, displayConfig);
    }
    
    public IDisplayer getDisplayer() {
        return mDisplayer;
    }

    public void setDisplayer(IDisplayer displayer) {
        this.mDisplayer = displayer;
    }
    
    public BitmapDisplayConfig getDefaultDisplayConfig() {
    	return mDisplayConfig;
    }
    
    public int getDefaultImageResId() {
        return mDisplayConfig.defaultImageResId;
    }

    public int getErrorImageResId() {
        return mDisplayConfig.errorImageResId;
    }

    public Animation getAnimation() {
        return mDisplayConfig.animation;
    }

    public int getAnimationType() {
        return mDisplayConfig.animationType;
    }
    
    public void setDefaultImageResId(int defaultImageResId) {
        mDisplayConfig.defaultImageResId = defaultImageResId;
    }

    public void setErrorImageResId(int errorImageResId) {
        mDisplayConfig.errorImageResId = errorImageResId;
    }
    
    
    public void setDefaultAndImageResId(int resId) {
        mDisplayConfig.defaultImageResId = resId;
        mDisplayConfig.errorImageResId = resId;
    }
    
    public void setDefaultBitmapWH(int w, int h) {
        mDisplayConfig.bitmapWidth = w;
        mDisplayConfig.bitmapHeight = h;
    }

    public void setAnimation(Animation animation) {
        mDisplayConfig.animation = animation;
    }

    public void setAnimationType(int animationType) {
        mDisplayConfig.animationType = animationType;
    }
    
    public void setCorners(boolean hasRoundCorners) {
        mDisplayConfig.roundConfig = mDisplayConfig.sRoundConfig;
    }
    
    public void setRoundConfig(RoundConfig config) {
        mDisplayConfig.roundConfig = config;
    }
    
    public void setDefaultBitmap(Bitmap defaultBitmap) {
        mDisplayConfig.defaultBitmap = defaultBitmap;
    }
    
    public ImageContainer doDisplay(View v, final String uri, BitmapDisplayConfig displayConfig) {
    	return doDisplay(v, uri, displayConfig, null);
    }
    
    public ImageContainer doDisplay(View v, final String uri, BitmapDisplayConfig displayConfig, LoadingListener loadingListener) {
    	if (displayConfig == null) {
            displayConfig = mDisplayConfig;
        }
        final WeakReference<View> ref = new WeakReference<View>(v);
        final BitmapDisplayConfig curDisplayConfig = displayConfig;
        return doDisplay(ref.get(), uri, curDisplayConfig, new ImageListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
//                showImg(ref.get(), null, curDisplayConfig, false, false);
                mDisplayer.loadFailDisplay(ref.get(), curDisplayConfig);
            }

            @Override
            public void onResponse(ImageContainer response, boolean isImmediate) {
                if (response.getBitmap() == null) {
                    mDisplayer.loadDefaultDisplay(ref.get(), curDisplayConfig);
                } else {
                    curDisplayConfig.isImmediate = isImmediate;
                    mDisplayer.loadCompletedisplay(ref.get(), response.getBitmap(), curDisplayConfig);
                }
            }
        }, loadingListener);
    }
    
    public ImageContainer doDisplay(final View view, final String uri, ImageListener listener, LoadingListener loadingListener) {
        return doDisplay(view, uri, null, listener, loadingListener);
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private ImageContainer doDisplay(final View view, final String uri, BitmapDisplayConfig displayConfig, ImageListener listener, LoadingListener loadingListener) {
    	if (displayConfig == null) {
            displayConfig = mDisplayConfig;
        }

        final BitmapDisplayConfig curDisplayConfig = displayConfig;
        if (view == null) {
            return doDisplay(uri, listener, loadingListener);
        }
        if (view instanceof NetworkImageView) {
            NetworkImageView networkImageView = ((NetworkImageView) view);
            networkImageView.setDefaultImageResId(curDisplayConfig.defaultImageResId);
            networkImageView.setErrorImageResId(curDisplayConfig.errorImageResId);
            networkImageView.setImageUrl(uri, mImageLoader);
            return networkImageView.mImageContainer;
        } else {
            ImageContainer tagContainer = null;
             @SuppressWarnings("unchecked")
            WeakReference<ImageContainer> ref = (WeakReference<ImageContainer>) view.getTag(TAG_ID);
             if (ref != null) {
                 tagContainer = ref.get();
             }
            if (TextUtils.isEmpty(uri)) {
                if (tagContainer != null) {
                    tagContainer.cancelRequest();
                    tagContainer = null;
                    view.setTag(TAG_ID, tagContainer);
                }
//                mDisplayer.loadDefaultDisplay(view, curDisplayConfig);
                if (listener != null) {
                    listener.onErrorResponse(new VolleyError("url can not be empty!"));
                }
                return null;
            }
            if (tagContainer != null && tagContainer.getRequestUrl() != null) {
                if (tagContainer.getRequestUrl().equals(uri)) {
                    return tagContainer;
                } else {
                    tagContainer.cancelRequest();
//                    mDisplayer.setDefaultImageOrNull(view, curDisplayConfig.defaultImageResId);
                }
            }
            final ImageContainer container = mImageLoader.get(mContext, uri, listener, loadingListener, displayConfig.bitmapWidth, displayConfig.bitmapHeight);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 && view.getTag(TAG_ID) == null) {
            	 view.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
                     
					@SuppressWarnings("deprecation")
                    @Override
                     public void onViewDetachedFromWindow(View v) {
                         container.cancelRequest();
                         view.setTag(TAG_ID, null);
                         if (view instanceof ImageView) {
                             ((ImageView) view).setImageBitmap(null);
                         } else {
                             view.setBackgroundDrawable(null);
                         }
                         view.removeOnAttachStateChangeListener(this);
                     }
                     
                     @Override
                     public void onViewAttachedToWindow(View v) {
                         
                     }
                 });
            }
            view.setTag(TAG_ID, new WeakReference<ImageContainer>(container));
            return container;
        }
    }
    
    public ImageContainer doDisplay(final String uri, ImageListener listener) {
        return doDisplay(uri, listener, null);
    } 
    
    public ImageContainer doDisplay(final String uri, ImageListener listener, LoadingListener loadingListener) {
        if (TextUtils.isEmpty(uri)) {
            if (listener != null) {
                listener.onErrorResponse(new VolleyError());
            }
            return null;
        }
        return mImageLoader.get(mContext, uri, listener,loadingListener, mDisplayConfig.bitmapWidth, mDisplayConfig.bitmapHeight);
    }
    
    /**
     * 从res中获取bitmap
     * @param resId
     * @return
     */
    public Bitmap getBitmapFromRes(int resId) {
        return BitmapDecoder.getBitmapFromRes(mContext, mBitmapCache, resId, mDisplayConfig.bitmapWidth, mDisplayConfig.bitmapHeight);
    }
    
    /**
     * 从asset中获取bitmap
     * getBitmapFromAsset
     * @param filePath
     * @return
     * @since 3.6
     */
    public Bitmap getBitmapFromAsset(String filePath) {
        return BitmapDecoder.getBitmapFromAsset(mContext, mBitmapCache, filePath, mDisplayConfig.bitmapWidth, mDisplayConfig.bitmapHeight);
    }
    
    /**
     * 从系统资源中获取bitmap
     * getStreamFromContent
     * @param imageUri
     * @return
     * @since 3.6
     */
    public Bitmap getBitmapFromContent(String imageUri) {
        return BitmapDecoder.getBitmapFromContent(mContext, mBitmapCache, imageUri, mDisplayConfig.bitmapWidth, mDisplayConfig.bitmapHeight);
    }
    
    /**
     * 从sdcard的文件中获取bitmap
     * getBitmapFromFile
     * @param path
     * @return
     * @since 3.5
     */
    public Bitmap getBitmapFromFile(String path) {
        return BitmapDecoder.getBitmapFromFile(mBitmapCache, path, mDisplayConfig.bitmapWidth, mDisplayConfig.bitmapHeight);
    }
    
    public Bitmap getBitmapFromMemCache(String key) {
        return mBitmapCache.getBitmap(ImageLoader.getCacheKey(key,mDisplayConfig.bitmapWidth, mDisplayConfig.bitmapHeight));
    }
    
    public Bitmap getBitmapFromMemCache(String key, int w, int h) {
        return mBitmapCache.getBitmap(ImageLoader.getCacheKey(key,w, h));
    }
    
    /**
     * 继续图片加载任务
     * resume
     * @since 3.6
     */
    public void resume() {
        if (sRequestQueue != null) {
            sRequestQueue.resume();
        }
    }
    
    /**
     * 暂停所有加载的任务
     * pause
     * @since 3.6
     */
    public void pause() {
        if (sRequestQueue != null) {
            sRequestQueue.pause();
        }
    }
    
    /**
     * 取消所有即将开始的请求
     * cancelAllRequest
     * @since 3.6
     */
    public void cancelAllRequest() {
        if (sRequestQueue != null) {
            sRequestQueue.cancelAll(mContext);
        }
    }
    
    /**
     * 清除view上面的任务和标记
     * clearViewTask
     * @param view
     */
    public void clearViewTask(View view) {
        @SuppressWarnings("unchecked")
        WeakReference<ImageContainer> ref = (WeakReference<ImageContainer>) view.getTag(TAG_ID);
        if (ref != null) {
            ImageContainer tagContainer = ref.get();
            if (tagContainer != null) {
                tagContainer.cancelRequest();
            }
        }
        view.setTag(TAG_ID);
    }
    
    /**
     * 清除掉某张图片的内存缓存
     * clearMemoryCache
     * @param key 图片的url
     * @since 3.6
     */
    public void clearMemoryCache(String key) {
        if (mBitmapCache.getBitmap(key) != null) {
            mBitmapCache.putBitmap(key, null);
        }
    }
    
    /**
     * 清除指定的图片缓存
     * clearMemoryCache
     * @param keys
     * @since 3.6
     */
    public void clearMemoryCache(List<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                clearMemoryCache(key);
            }
        }
    }
    
    /**
     * 清除掉所有的图片的内存缓存
     * clearMemoryCache
     * @since 3.6
     */
    public void clearMemoryCache() {
        mBitmapCache.evictAll();
    }
    
    /**
     * 清除掉所有的图片磁盘缓存
     * clearDiskCache
     * @param callback
     * @since 3.6
     */
    public void clearDiskCache(Runnable callback) {
        if (sRequestQueue != null) {
            ClearCacheRequest request = new ClearCacheRequest(sRequestQueue.getCache(), callback);
            sRequestQueue.add(request);
        }
    }
    
    public void showImg(View view, ImageContainer container, boolean success, boolean isImmediate) {
        showImg(view, container, mDisplayConfig, success, isImmediate);
    }
    
    public void showImg(View view, ImageContainer container, BitmapDisplayConfig displayConfig, boolean success,
            boolean isImmediate) {
        if (view == null)
            return;
        if (success) {
            // 显示一张默认图片
            if (container.getBitmap() == null) {
                mDisplayer.loadDefaultDisplay(view, displayConfig);
             // 显示加载好的图片
            } else {
                displayConfig.isImmediate = isImmediate;
                mDisplayer.loadCompletedisplay(view, container.getBitmap(), displayConfig);
            }
        } else {
            mDisplayer.loadFailDisplay(view, displayConfig);
        }
    }
    
    private BitmapDisplayConfig getDisplayConfig() {
        BitmapDisplayConfig config = new BitmapDisplayConfig();
        config.animation = mDisplayConfig.animation;
        config.bitmapWidth = mDisplayConfig.bitmapWidth;
        config.bitmapHeight = mDisplayConfig.bitmapHeight;
        config.animationType = mDisplayConfig.animationType;
        config.defaultImageResId = mDisplayConfig.defaultImageResId;
        config.errorImageResId = mDisplayConfig.errorImageResId;
        return config;
    }

    public static class BitmapDisplayConfig {
        public RoundConfig sRoundConfig;
        public int bitmapWidth;
        public int bitmapHeight;

        public Animation animation;

        public int animationType;
        public int defaultImageResId;
        public int errorImageResId;

//        public boolean hasRoundCorners;
        public RoundConfig roundConfig;
        public Bitmap defaultBitmap;
        
        // 是否直接加载图片
        public boolean isImmediate;
        
        public BitmapDisplayConfig() {
            animationType = BitmapDisplayConfig.AnimationType.fadeIn;
            sRoundConfig = new RoundConfig();
        }

        public class AnimationType {
            public static final int userDefined = 0;
            public static final int fadeIn = 1;
        }
        
        public class RoundConfig {
            public int radius;
            public int margin;
            
            public RoundConfig(int radius, int margin) {
                super();
                this.radius = radius;
                this.margin = margin;
            }
            
            public RoundConfig() {
                this(12, 1);
            }
            
        }

    }
}
