package com.android.volley.toolbox;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;

import com.android.volley.toolbox.ImageLoader.ImageCache;

public class BitmapDecoder {
    public static final String SCHEME_RES = "android_res://";
    public static final String SCHEME_ASSET = "android_asset://";
    public static final String SCHEME_CONTENT = "android_content://";
    
    protected static final String CONTENT_CONTACTS_URI_PREFIX = "content://com.android.contacts/";
    protected static final int BUFFER_SIZE = 32 * 1024; // 32 Kb
    
    /**
     * 从res中获取bitmap
     * 
     * @param resId
     * @return
     */
    public static Bitmap getBitmapFromRes(Context context, ImageCache cache, int resId, int maxWidth, int maxHeight) {
        Bitmap bitmap = null;
        if (cache != null) {
            bitmap = cache.getBitmap(SCHEME_RES + resId);
        }
        if (bitmap == null) {
            bitmap = inputStream2Bitmap(context, SCHEME_RES + resId, getInputStream(context, SCHEME_RES + resId), Config.RGB_565, maxWidth, maxHeight);
            if (cache != null && bitmap != null) {
                cache.putBitmap(SCHEME_RES + resId, bitmap);
            }
        }
        return bitmap;
    }

    /**
     * 从asset中获取bitmap getBitmapFromAsset
     * 
     * @param filePath
     * @return
     * @since 3.6
     */
    public static Bitmap getBitmapFromAsset(Context context, ImageCache cache, String filePath, int maxWidth, int maxHeight) {
        Bitmap bitmap = null;
        if (cache != null) {
            bitmap = cache.getBitmap(SCHEME_ASSET + filePath);
        }
        if (bitmap == null) {
            bitmap = inputStream2Bitmap(context, SCHEME_ASSET + filePath, getInputStream(context, SCHEME_ASSET + filePath), Config.RGB_565, maxWidth, maxHeight);
            if (cache != null && bitmap != null) {
                cache.putBitmap(SCHEME_ASSET + filePath, bitmap);
            }
        }
        return bitmap;
    }

    /**
     * 从系统资源中获取bitmap getStreamFromContent
     * 
     * @param imageUri
     * @return
     * @since 3.6
     */
    public static Bitmap getBitmapFromContent(Context context, ImageCache cache, String imageUri, int maxWidth, int maxHeight) {
        Bitmap bitmap = null;
        if (cache != null) {
            bitmap = cache.getBitmap(SCHEME_CONTENT + imageUri);
        }
        if (bitmap == null) {
            bitmap = inputStream2Bitmap(context, SCHEME_CONTENT + imageUri, getInputStream(context, SCHEME_CONTENT + imageUri), Config.RGB_565, maxWidth, maxHeight);
            if (cache != null && bitmap != null) {
                cache.putBitmap(SCHEME_CONTENT + imageUri, bitmap);
            }
        }
        return bitmap;
    }
    
    /**
     * 从文件获取图片Bitmap
     * getBitmapFromFile
     * @param cache
     * @param path
     * @param maxWidth
     * @param maxHeight
     * @return
     * @since 3.5
     */
    public static Bitmap getBitmapFromFile(ImageCache cache, String path, int maxWidth, int maxHeight) {
        Bitmap bitmap = null;
        if (cache != null) {
            bitmap = cache.getBitmap(path);
        }
        if (bitmap == null) {
            bitmap = inputStream2Bitmap(null, path, getInputStream(null, path), Config.RGB_565, maxWidth, maxHeight);
            if (cache != null && bitmap != null) {
                cache.putBitmap(path, bitmap);
            }
        }
        return bitmap;
    }
    
    private static boolean isVideoUri(Context context, Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null) {
            return false;
        }
        return mimeType.startsWith("video/");
    }
    
    // 获取最合适的缩放比例
    static int findBestSampleSize(
            int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
        double wr = (double) actualWidth / desiredWidth;
        double hr = (double) actualHeight / desiredHeight;
        double ratio = Math.min(wr, hr);
        float n = 1.0f;
        while ((n * 2) <= ratio) {
            n *= 2;
        }
        return (int) n;
    }
    
    static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary,
            int actualSecondary) {
        // If no dominant value at all, just return the actual.
        if (maxPrimary == 0 && maxSecondary == 0) {
            return actualPrimary;
        }

        // If primary is unspecified, scale primary to match secondary's scaling ratio.
        if (maxPrimary == 0) {
            double ratio = (double) maxSecondary / (double) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0) {
            return maxPrimary;
        }

        double ratio = (double) actualSecondary / (double) actualPrimary;
        int resized = maxPrimary;
        if (resized * ratio > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }
    
    /**
     * 根据传入的key获取对应文件的流
     * getInputStream
     * @param context
     * @param key
     * @return
     * @since 3.5
     */
    @SuppressLint("DefaultLocale")
    public static InputStream getInputStream(Context context, String key) {
        if (key.startsWith(SCHEME_RES)) {
            int index = key.indexOf(SCHEME_RES);
            int id = Integer.parseInt(key.substring(index + SCHEME_RES.length()));
            return context.getResources().openRawResource(id);

        } else if (key.startsWith(SCHEME_ASSET)) {
            int index = key.indexOf(SCHEME_ASSET);
            String filePath = key.substring(index + SCHEME_ASSET.length());
            try {
                return context.getAssets().open(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (key.startsWith(SCHEME_CONTENT)) {
            ContentResolver res = context.getContentResolver();
            int index = key.indexOf(SCHEME_CONTENT);
            String imageUri = key.substring(index + SCHEME_CONTENT.length());
            Uri uri = Uri.parse(imageUri);
            if (isVideoUri(context, uri)) { // video thumbnail
                Long origId = Long.valueOf(uri.getLastPathSegment());
                Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(res, origId,
                        MediaStore.Images.Thumbnails.MINI_KIND, null);
                if (bitmap != null) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(CompressFormat.PNG, 0, bos);
                    return new ByteArrayInputStream(bos.toByteArray());
                }
            } else if (imageUri.startsWith(CONTENT_CONTACTS_URI_PREFIX)) { // contacts
                return ContactsContract.Contacts.openContactPhotoInputStream(res, uri);
            } else {
                try {
                    return res.openInputStream(uri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

        } else if (key.trim().toLowerCase().startsWith("file")) {
            try {
                String filePath = new URI(key).getPath();
                return new ContentLengthInputStream(new BufferedInputStream(new FileInputStream(filePath), BUFFER_SIZE),
                        (int) new File(filePath).length());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

        } else if (key.trim().toLowerCase().startsWith("/")) {
            try {
                return new ContentLengthInputStream(new BufferedInputStream(new FileInputStream(key), BUFFER_SIZE),
                        (int) new File(key).length());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    /**
     * 把流转成bitmap
     * inputStream2Bitmap
     * @param context 上下文
     * @param key 流所对应文件的key
     * @param is 流
     * @param config
     * @param maxWidth
     * @param maxHeight
     * @return
     * @since 3.5
     */
    public static Bitmap inputStream2Bitmap(Context context, String key, InputStream is, Config config, int maxWidth, int maxHeight) {
        if (is == null) return null;
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap = null;
        if (maxWidth == 0 && maxWidth == 0) {
            decodeOptions.inPreferredConfig = config;
            decodeOptions.inPurgeable = true;
            bitmap = BitmapFactory.decodeStream(is, null, decodeOptions);
        } else {
            // If we have to resize this image, first get the natural bounds.
            decodeOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, decodeOptions);
            int actualWidth = decodeOptions.outWidth;
            int actualHeight = decodeOptions.outHeight;

            // Then compute the dimensions we would ideally like to decode to.
            int desiredWidth = getResizedDimension(maxWidth, maxHeight, actualWidth, actualHeight);
            int desiredHeight = getResizedDimension(maxHeight, maxWidth, actualHeight, actualWidth);

            // Decode to the nearest power of two scaling factor.
            decodeOptions.inJustDecodeBounds = false;
            decodeOptions.inPurgeable = true;
            // TODO(ficus): Do we need this or is it okay since API 8 doesn't
            // support it?
            // decodeOptions.inPreferQualityOverSpeed =
            // PREFER_QUALITY_OVER_SPEED;
            decodeOptions.inSampleSize = findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
            try {
                is.reset();
            } catch (IOException e) {
                try {
                    is.close();
                } catch (IOException e1) {
                }
                is = getInputStream(context, key);
            }
            Bitmap tempBitmap = BitmapFactory.decodeStream(is, null, decodeOptions);

            // If necessary, scale down to the maximal acceptable size.
            if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight)) {
                bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
                tempBitmap.recycle();
            } else {
                bitmap = tempBitmap;
            }
        }
        return bitmap;
    }
    
    /**
     * 把byte数组转换成bitmap，默认图片采用RGB_565
     * bytes2Bitmap
     * @param data byte数组
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return
     * @since 3.5
     */
    public static Bitmap bytes2Bitmap(byte[] data, int maxWidth, int maxHeight) {
        return bytes2Bitmap(data, Config.RGB_565, maxWidth, maxHeight);
    }
    
    /**
     * 把byte数组转换成bitmap
     * bytes2Bitmap
     * @param data byte数组
     * @param config 图片的配置
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @return
     * @since 3.5
     */
    public static Bitmap bytes2Bitmap(byte[] data, Config config, int maxWidth, int maxHeight) {
        if (data == null) return null;
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap = null;
        if (maxWidth == 0 && maxWidth == 0) {
            decodeOptions.inPreferredConfig = config;
            decodeOptions.inPurgeable = true;
            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        } else {
            // If we have to resize this image, first get the natural bounds.
            decodeOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            int actualWidth = decodeOptions.outWidth;
            int actualHeight = decodeOptions.outHeight;

            // Then compute the dimensions we would ideally like to decode to.
            int desiredWidth = getResizedDimension(maxWidth, maxHeight, actualWidth, actualHeight);
            int desiredHeight = getResizedDimension(maxHeight, maxWidth, actualHeight, actualWidth);

            // Decode to the nearest power of two scaling factor.
            decodeOptions.inJustDecodeBounds = false;
            decodeOptions.inPurgeable = true;
            // TODO(ficus): Do we need this or is it okay since API 8 doesn't
            // support it?
            // decodeOptions.inPreferQualityOverSpeed =
            // PREFER_QUALITY_OVER_SPEED;
            decodeOptions.inSampleSize = findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
            Bitmap tempBitmap = null;
            try {
                tempBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            } catch (OutOfMemoryError e) {
                decodeOptions.inSampleSize++;
                tempBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
            }

            // If necessary, scale down to the maximal acceptable size.
            if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight)) {
                bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
                tempBitmap.recycle();
            } else {
                bitmap = tempBitmap;
            }
        }
        return bitmap;
    }
    
}
