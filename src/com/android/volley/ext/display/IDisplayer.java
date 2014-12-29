package com.android.volley.ext.display;

import android.graphics.Bitmap;
import android.view.View;

import com.android.volley.ext.tools.BitmapTools.BitmapDisplayConfig;

public interface IDisplayer {
    void loadCompletedisplay(View imageView, Bitmap bitmap, BitmapDisplayConfig config);

    void loadFailDisplay(View view, BitmapDisplayConfig config);

    void loadDefaultDisplay(View view, BitmapDisplayConfig config);
}
