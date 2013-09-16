package com.dreamlink.communication.ui;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.media.MediaInfoManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images.Thumbnails;
import android.widget.ImageView;

public class AsyncImageLoader {
	private static final String TAG = "AsyncImageLoader";
	// SoftReference是软引用，是为了更好的为了系统回收变量
	public static  HashMap<String, SoftReference<Bitmap>> bitmapCache;
	private Context context;
	private ExecutorService pool ; 
	
	private FileInfoManager fileInfoManager;
	private ILoadImageCallback callback;
	private ImageView imageView;

	public AsyncImageLoader(Context context) {
		this.context = context;
		bitmapCache = new HashMap<String, SoftReference<Bitmap>>();
		
		fileInfoManager = new FileInfoManager(context);
		pool = Executors.newCachedThreadPool();
	}
	
	public void setCallBack(ILoadImageCallback callback){
		this.callback = callback;
	}
	
	public void setImageView(ImageView imageView){
		this.imageView = imageView;
	}
	
	final Handler mHandler = new Handler() {
		public void handleMessage(Message message) {
			callback.onObtainBitmap((Bitmap) message.obj, imageView);
		}
	};
	
	//I want use a same fun to do these
	/**
	 * 方法一
	 * 这个方法，同步性不是很好，有的时候无法显示图片
	 * @param path
	 * @param type
	 * @return
	 */
	public Bitmap loadImage(final String path, final int type){
		//we use file path as key
		if (bitmapCache.containsKey(path)) {
			// 从缓存中获取
			SoftReference<Bitmap> softReference = bitmapCache.get(path);
			Bitmap bitmap = softReference.get();
			if (null != bitmap) {
				return bitmap;
			}
		}
		
		final Handler handler = new Handler() {
			public void handleMessage(Message message) {
				callback.onObtainBitmap((Bitmap) message.obj, imageView);
			}
		};
		
		switch (type) {
		case FileInfoManager.TYPE_APK:
			pool.execute(new ApkRunnable(path));
			break;
		case FileInfoManager.TYPE_IMAGE:
			break;
		case FileInfoManager.TYPE_VIDEO:
			break;
		default:
			break;
		}
		
		return null;
	}
	
	class ApkRunnable implements Runnable{
		String apkPath;
		public ApkRunnable(String path) {
			apkPath = path;
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			Bitmap bitmap = null;
			Drawable drawable = fileInfoManager.getApkIcon(apkPath);
			BitmapDrawable bd = (BitmapDrawable) drawable;
			if (null != bd) {
				bitmap = bd.getBitmap();
			}
			
			bitmapCache.put(apkPath, new SoftReference<Bitmap>(bitmap));
			Message msg = mHandler.obtainMessage(0, bitmap);
			mHandler.sendMessage(msg);
		}
	}
	
	class VideoRunnable implements Runnable{
		String videoPath;
		public  VideoRunnable(String path){
			videoPath = path;
		}
		
		@Override
		public void run() {
			Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, Thumbnails.MINI_KIND);
			bitmapCache.put(videoPath, new SoftReference<Bitmap>(bitmap));
			Message msg = mHandler.obtainMessage(0, bitmap);
			mHandler.sendMessage(msg);
		}
		
	}
	
	public Bitmap loadImage(final String path, final int type, final ImageView imageView, final ILoadImageCallback callback){
		return loadImage(path, type, null, imageView, callback);
	}
	
	/**
	 * 方法三
	 * 也不是很完美，不过现在将就用这个吧
	 * @param path
	 * @param type
	 * @param imageView
	 * @param callback
	 * @return
	 */
	public Bitmap loadImage(final String path, final int type, final Map<String, Bitmap> caches, 
			final ImageView imageView, final ILoadImageCallback callback){
		//we use file path as key
		if (bitmapCache.containsKey(path)) {
			// 从缓存中获取
			SoftReference<Bitmap> softReference = bitmapCache.get(path);
			Bitmap bitmap = softReference.get();
			if (null != bitmap) {
				return bitmap;
			}
		}
		
		final Handler handler = new Handler() {
			public void handleMessage(Message message) {
				callback.onObtainBitmap((Bitmap) message.obj, imageView);
			}
		};
		
		switch (type) {
		case FileInfoManager.TYPE_APK:
			pool.execute(new Runnable() {
				@Override
				public void run() {
					Bitmap bitmap = null;
					Drawable drawable = fileInfoManager.getApkIcon(path);
					BitmapDrawable bd = (BitmapDrawable) drawable;
					if (null != bd) {
						bitmap = bd.getBitmap();
					}
					
					bitmapCache.put(path, new SoftReference<Bitmap>(bitmap));
					Message msg = handler.obtainMessage(0, bitmap);
					handler.sendMessage(msg);
				}
			});
			break;
		case FileInfoManager.TYPE_IMAGE:
			pool.execute(new Runnable() {
				@Override
				public void run() {
					Bitmap bitmap = null;
					bitmap = getBitmapFromUrl(path, caches);
					bitmapCache.put(path, new SoftReference<Bitmap>(bitmap));
					Message msg = handler.obtainMessage(0, bitmap);
					handler.sendMessage(msg);
				}
			});
			break;
		case FileInfoManager.TYPE_VIDEO:
			pool.execute(new Runnable() {
				@Override
				public void run() {
					Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path, Thumbnails.MINI_KIND);
					bitmapCache.put(path, new SoftReference<Bitmap>(bitmap));
					Message msg = handler.obtainMessage(0, bitmap);
					handler.sendMessage(msg);
				}
			});
			break;
		default:
			break;
		}
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
