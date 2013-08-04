package com.dreamlink.communication.ui;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.media.MediaInfoManager;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images.Thumbnails;
import android.widget.ImageView;

public class AsyncImageLoader {
	private static final String TAG = AsyncImageLoader.class.getName();
	// SoftReference是软引用，是为了更好的为了系统回收变量
	private HashMap<Long, SoftReference<Bitmap>> audioCache;
	private HashMap<String, SoftReference<Drawable>> apkCache;
	private HashMap<String, SoftReference<Bitmap>> videoCache;
	private Context context;
	private ExecutorService pool ; 
	
	private FileInfoManager fileInfoManager;

	public AsyncImageLoader(Context context) {
		this.context = context;
		audioCache = new HashMap<Long, SoftReference<Bitmap>>();
		apkCache = new HashMap<String, SoftReference<Drawable>>();
		videoCache = new HashMap<String, SoftReference<Bitmap>>();
		
		fileInfoManager = new FileInfoManager(context);
//		pool = Executors.newFixedThreadPool(15);
	}
	
	/**
	 * get apk file icon
	 * @param apkPath
	 * @param imageView
	 * @param callback
	 * @return apk luancher icon
	 */
	public Drawable loadApkDrawable(final String apkPath, final ImageView imageView, final ILoadImageCallback callback){
		if (apkCache.containsKey(apkPath)) {
			// 从缓存中获取
			SoftReference<Drawable> softReference = apkCache.get(apkPath);
			Drawable drawable = softReference.get();
			if (null != drawable) {
				return drawable;
			}
		}
		
		final Handler handler = new Handler() {
			public void handleMessage(Message message) {
				callback.onObtainDrawable((Drawable)message.obj, imageView);
			}
		};
		
		new Thread() {
			public void run() {
				Drawable drawable = fileInfoManager.getApkIcon(apkPath);
				apkCache.put(apkPath, new SoftReference<Drawable>(drawable));
				Message msg = handler.obtainMessage(0, drawable);
				handler.sendMessage(msg);
			}
		}.start();
		return null;
	}

	/**
	 * get audio album and show 
	 * @param songId  song id in db
	 * @param albumId  song album id in db
	 * @param imageView
	 * @param callback
	 * @return audio album
	 */
	public Bitmap loadAudioBitmap(final long songId, final long  albumId, final ImageView imageView, final ILoadImageCallback callback) {
		if (audioCache.containsKey(songId)) {
			// 从缓存中获取
			SoftReference<Bitmap> softReference = audioCache.get(songId);
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
		
		// 建立一个获取专辑图片
		new Thread() {
			public void run() {
				Bitmap bitmap = MediaInfoManager.getArtwork(context, songId, albumId, true, false);
				audioCache.put(songId, new SoftReference<Bitmap>(bitmap));
				Message msg = handler.obtainMessage(0, bitmap);
				handler.sendMessage(msg);
			}
		}.start();
		return null;
	}
	
	/**
	 * get video thumbnail and show in imageview
	 * @param path video path
	 * @param imageView 
	 * @param callback 
	 * @return video thumbnail
	 */
	public Bitmap loadVideoBitmap(final String path, final ImageView imageView, final ILoadImageCallback callback) {
		if (videoCache.containsKey(path)) {
			// 从缓存中获取
			SoftReference<Bitmap> softReference = videoCache.get(path);
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
		
		new Thread() {
			public void run() {
				Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path, Thumbnails.MINI_KIND);
				videoCache.put(path, new SoftReference<Bitmap>(bitmap));
				Message msg = handler.obtainMessage(0, bitmap);
				handler.sendMessage(msg);
			}
		}.start();
		return null;
	}

	/**
	 * 异步加载图片的回调接口
	 */
	public interface ILoadImageCallback {
		public void onObtainBitmap(Bitmap bitmap, ImageView imageView);
		public void onObtainDrawable(Drawable drawable, ImageView imageView);
	}
}
