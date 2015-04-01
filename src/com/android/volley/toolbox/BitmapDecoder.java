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
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;

import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader.ImageCache;

public class BitmapDecoder {
    public static final String SCHEME_RES = "drawable://";
    public static final String SCHEME_ASSET = "assets://";
    public static final String SCHEME_CONTENT = "content://";
    
    protected static final String CONTENT_CONTACTS_URI_PREFIX = "content://com.android.contacts/";
    protected static final int BUFFER_SIZE = 32 * 1024; // 32 Kb
    
    /**
     * decode bitmap from resource
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
     * decode bitmap from asset
     * bitmap getBitmapFromAsset
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
     * decode bitmap from content
     * getStreamFromContent
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
     * decode bitmap from sdcard
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
    
    // get the best size to decode bitmap
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
     * 将流文件转换为bitmap对象
     * inputStream2Bitmap
     * @param context 上下文
     * @param key 
     * @param is 
     * @param config
     * @param maxWidth
     * @param maxHeight
     * @return
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
            // 如果是本地图片，读取exif信息，处理图片
            if (canDefineExifParams(key)) {
                bitmap = considerExactScaleAndOrientatiton(bitmap, defineExifOrientation(key));
            }
        }
        return bitmap;
    }
    
    public static Bitmap bytes2Bitmap(byte[] data, int maxWidth, int maxHeight) {
        return bytes2Bitmap(data, Config.RGB_565, maxWidth, maxHeight);
    }
    
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
    
    private static boolean canDefineExifParams(String imageUri) {
        String uri = imageUri.toLowerCase();
        return (uri.endsWith("jpg") || uri.endsWith("png") || uri.endsWith("jpeg")) && imageUri.startsWith("/");
    }

    private static ExifInfo defineExifOrientation(String imageUri) {
        int rotation = 0;
        boolean flip = false;
        try {
            ExifInterface exif = new ExifInterface(imageUri);
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    flip = true;
                case ExifInterface.ORIENTATION_NORMAL:
                    rotation = 0;
                    break;
                case ExifInterface.ORIENTATION_TRANSVERSE:
                    flip = true;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                    flip = true;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                    flip = true;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;
            }
        } catch (IOException e) {
            VolleyLog.e("Can't read EXIF tags from file [%s]", imageUri);
        }
        return new ExifInfo(rotation, flip);
    }
    
    protected static Bitmap considerExactScaleAndOrientatiton(Bitmap subsampledBitmap, ExifInfo exifInfo) {
    	if (subsampledBitmap == null) {
            return subsampledBitmap;
        }
        Matrix m = new Matrix();
        // Flip bitmap if need
        if (exifInfo.flipHorizontal) {
            m.postScale(-1, 1);
        }
        // Rotate bitmap if need
        if (exifInfo.rotation != 0) {
            m.postRotate(exifInfo.rotation);
        }

        Bitmap finalBitmap = Bitmap.createBitmap(subsampledBitmap, 0, 0, subsampledBitmap.getWidth(), subsampledBitmap
                .getHeight(), m, true);
        if (finalBitmap != subsampledBitmap) {
            subsampledBitmap.recycle();
        }
        return finalBitmap;
    }


    protected static class ExifInfo {

        public final int rotation;
        public final boolean flipHorizontal;

        protected ExifInfo() {
            this.rotation = 0;
            this.flipHorizontal = false;
        }

        protected ExifInfo(int rotation, boolean flipHorizontal) {
            this.rotation = rotation;
            this.flipHorizontal = flipHorizontal;
        }
    }
}
