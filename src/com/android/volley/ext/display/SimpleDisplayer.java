package com.android.volley.ext.display;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.android.volley.ext.tools.BitmapTools.BitmapDisplayConfig;

public class SimpleDisplayer implements IDisplayer {
	
    @Override
	public void loadCompletedisplay(View imageView,Bitmap bitmap,BitmapDisplayConfig config){
	    if (imageView == null) return;
	    Drawable bitmapDrawable = null;
        if (config.roundConfig != null) {
            bitmapDrawable = new RoundedDrawable(bitmap, config.roundConfig.radius, config.roundConfig.margin);
            // BitmapUtil.getRoundedCornerBitmapDrawable((BitmapDrawable)bitmapDrawable);
        } else {
            bitmapDrawable = new BitmapDrawable(imageView.getResources(), bitmap);
        }
	    if (config.isImmediate) {
	        
	        if(imageView instanceof ImageView){
	            ((ImageView)imageView).setImageDrawable(bitmapDrawable);
	        }else{
	            imageView.setBackgroundDrawable(bitmapDrawable);
	        }
	    } else {
	        switch (config.animationType) {
	        case BitmapDisplayConfig.AnimationType.fadeIn:
	            fadeInDisplay(imageView,bitmapDrawable);
	            break;
	        case BitmapDisplayConfig.AnimationType.userDefined:
	            animationDisplay(imageView,bitmapDrawable,config.animation);
	            break;
	        default:
	            break;
	        }
	    }
	}
    
    @Override
	public void loadFailDisplay(View view, BitmapDisplayConfig config){
	    if (view == null) return;
        if(view instanceof ImageView){
            ImageView img = (ImageView) view;
            if (config.errorImageResId != 0) {
                img.setImageResource(config.errorImageResId);
            } else {
                img.setImageBitmap(null);
            }
        }else{
            if (config.errorImageResId != 0) {
                view.setBackgroundResource(config.errorImageResId);
            } else {
                view.setBackgroundDrawable(null);
            }
        }
    }
	
    @Override
	public void loadDefaultDisplay(View view, BitmapDisplayConfig config) {
	    if (view == null) return;
	    if (view instanceof ImageView) {
            ImageView img = (ImageView) view;
            if (config.defaultImageResId != 0) {
                img.setImageResource(config.defaultImageResId);
            } else {
                img.setImageBitmap(config.defaultBitmap);
            }
        } else {
            if (config.defaultImageResId != 0) {
                view.setBackgroundResource(config.defaultImageResId);
            } else if (config.defaultBitmap != null) {
                view.setBackgroundDrawable(new BitmapDrawable(view.getResources(), config.defaultBitmap));
            } else {
                view.setBackgroundDrawable(null);
            }
        }
	}

	private void fadeInDisplay(View imageView,Drawable bitmapDrawable){
		AlphaAnimation animation = new AlphaAnimation(0, 1);
	    animation.setDuration(200);
	    animationDisplay(imageView, bitmapDrawable, animation);
//	    if (bitmapDrawable == null) {
//	        return;
//	    }
//        final TransitionDrawable td =
//                new TransitionDrawable(new Drawable[] {
//                        new ColorDrawable(android.R.color.transparent),
//                        bitmapDrawable
//                });
//        if(imageView instanceof ImageView){
//            ((ImageView)imageView).setImageDrawable(td);
//        }else{
//            imageView.setBackgroundDrawable(td);
//        }
//        td.startTransition(200);
    }
    
    
    private void animationDisplay(View imageView,Drawable bitmapDrawable,Animation animation){
        if (bitmapDrawable == null) {
            return;
        }
        if (animation != null) {
            animation.setStartTime(AnimationUtils.currentAnimationTimeMillis());        
        }
        if(imageView instanceof ImageView){
            ((ImageView)imageView).setImageDrawable(bitmapDrawable);
        }else{
            imageView.setBackgroundDrawable(bitmapDrawable);
        }
        if (animation != null) {
            imageView.startAnimation(animation);
        }
    }
    
    public static class RoundedDrawable extends Drawable {

        protected final float cornerRadius;
        protected final int margin;

        protected final RectF mRect = new RectF(),
                mBitmapRect;
        protected final BitmapShader bitmapShader;
        protected final Paint paint;

        public RoundedDrawable(Bitmap bitmap, int cornerRadius, int margin) {
            this.cornerRadius = cornerRadius;
            this.margin = margin;

            bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mBitmapRect = new RectF (margin, margin, bitmap.getWidth() - margin, bitmap.getHeight() - margin);
            
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setShader(bitmapShader);
        }

        @Override
        protected void onBoundsChange(Rect bounds) {
            super.onBoundsChange(bounds);
            mRect.set(margin, margin, bounds.width() - margin, bounds.height() - margin);
            
            // Resize the original bitmap to fit the new bound
            Matrix shaderMatrix = new Matrix();
            shaderMatrix.setRectToRect(mBitmapRect, mRect, Matrix.ScaleToFit.FILL);
            bitmapShader.setLocalMatrix(shaderMatrix);
            
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawRoundRect(mRect, cornerRadius, cornerRadius, paint);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            paint.setColorFilter(cf);
        }
    }
}
