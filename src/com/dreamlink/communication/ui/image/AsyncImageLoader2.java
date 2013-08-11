package com.dreamlink.communication.ui.image;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

import com.dreamlink.communication.ui.BitmapUtilities;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

public class AsyncImageLoader2 {
	private static final String TAG = AsyncImageLoader2.class.getName();
	// SoftReference是软引用，是为了更好的为了系统回收?
	public static  HashMap<String ,SoftReference<Bitmap>> bitmapCache;

	public AsyncImageLoader2(Context context) {
		bitmapCache = new HashMap<String, SoftReference<Bitmap>>();
	}
	
	public Bitmap loadImage(final String url, final Map<String, Bitmap> caches,
			final ImageView imageView, final ILoadImageCallback callback){
		if (bitmapCache.containsKey(url)) {
			SoftReference<Bitmap> softReference = bitmapCache.get(url);
			Bitmap bitmap = softReference.get();
			if (null != bitmap) {
				return bitmap;
			}
		}
		
		final Handler handler = new Handler(){
			public void handleMessage(Message msg) {
				callback.onObtainBitmap((Bitmap)msg.obj, imageView);
			}
		};
		
		new Thread(){
			public void run() {
				Bitmap bitmap = null;
				bitmap = getBitmapFromUrl(url, caches);
				bitmapCache.put(url, new SoftReference<Bitmap>(bitmap));
				Message msg = handler.obtainMessage(0, bitmap);
				handler.sendMessage(msg);
			}
		}.start();
		
		return null;
	}
	
	private int width = 120;//每个Item的宽度,可以根据实际情况修改
	private int height = 150;//每个Item的高度,可以根据实际情况修改
	private Bitmap getBitmapFromUrl(String url, Map<String, Bitmap> caches){
		Bitmap bitmap = null;
		bitmap = caches.get(url);
		if(bitmap != null){
			return bitmap;
		}
		
//		BitmapFactory.Options options = new BitmapFactory.Options();
//		options.inSampleSize =4;
//		Bitmap bitmap2 = BitmapFactory.decodeFile(url, options);
		
//		bitmap = BitmapUtilities.getBitmapThumbnail(bitmap2,width,height);
		bitmap = BitmapUtilities.getBitmapThumbnail(url, width, height);
		return bitmap;
	}

	/**
	 * 异步加载图片的回调接口
	 */
	public interface ILoadImageCallback {
		public void onObtainBitmap(Bitmap bitmap, ImageView imageView);
	}
}
