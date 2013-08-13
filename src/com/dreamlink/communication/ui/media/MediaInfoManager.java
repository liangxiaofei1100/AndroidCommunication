package com.dreamlink.communication.ui.media;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;

import com.dreamlink.communication.R;
import com.dreamlink.communication.fileshare.FileInfo;
import com.dreamlink.communication.ui.image.ImageInfo;
import com.dreamlink.communication.util.Log;

/**
 * get video/audio/image info from db
 */
public class MediaInfoManager {
	private static final String TAG = "MediaInfoManager";

	// 获取专辑封面的Uri
	private static final Uri albumArtUri = Uri
			.parse("content://media/external/audio/albumart");

	private Context context;

	// 保存扫描到的音频文件
	public static List<FileInfo> musicList = new ArrayList<FileInfo>();

	// SD卡中的图片保存数据库Uri
	private Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
	// SD卡中的Audio保存数据库Uri
	private Uri audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
	// SD卡中的Video保存数据库Uri
	private Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

	public MediaInfoManager(Context context) {
		this.context = context;
	}

	/** get audios form {@link MediaStore.Audio.Media.EXTERNAL_CONTENT_URI} */
	public List<MediaInfo> getAudioInfo() {
		List<MediaInfo> list = new ArrayList<MediaInfo>();
		Cursor cursor = context.getContentResolver().query(audioUri, null,
				null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

		if (cursor.moveToFirst()) {
			MediaInfo mediaInfo = null;
			do {
				mediaInfo = new MediaInfo();
				long id = cursor.getLong(cursor
						.getColumnIndex(MediaStore.Audio.Media._ID)); // 音乐id
				String title = cursor.getString((cursor
						.getColumnIndex(MediaStore.Audio.Media.TITLE))); // 音乐标题
				// Log.d("Yuri", title);
				String artist = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.ARTIST)); // 艺术家
				String album = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.ALBUM)); // 专辑
				long albumId = cursor.getInt(cursor
						.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
				long duration = cursor.getLong(cursor
						.getColumnIndex(MediaStore.Audio.Media.DURATION)); // 时长
				long size = cursor.getLong(cursor
						.getColumnIndex(MediaStore.Audio.Media.SIZE)); // 文件大小
				String url = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.DATA)); // 文件路径
				int isMusic = cursor.getInt(cursor
						.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)); // 是否为音乐
				if (isMusic != 0) { // 只把音乐添加到集合当中
					mediaInfo.setId(id);
					mediaInfo.setDisplayName(title);
					mediaInfo.setArtist(artist);
					mediaInfo.setAlbum(album);
					mediaInfo.setAlbumId(albumId);
					mediaInfo.setDuration(duration);
					mediaInfo.setSize(size);
					mediaInfo.setUrl(url);
					list.add(mediaInfo);
				}
			} while (cursor.moveToNext());
		}
		cursor.close();
		return list;
	}

	/** get videos form {@link MediaStore.Video.Media.EXTERNAL_CONTENT_URI} */
	public List<MediaInfo> getVideoInfo() {
		List<MediaInfo> list = new ArrayList<MediaInfo>();
		ContentResolver contentResolver = context.getContentResolver();
		Cursor cursor = contentResolver.query(videoUri, null, null, null,
				MediaStore.Video.Media.DEFAULT_SORT_ORDER);

		if (cursor.moveToFirst()) {
			MediaInfo mediaInfo = null;
			do {
				mediaInfo = new MediaInfo();
				long id = cursor.getLong(cursor
						.getColumnIndex(MediaStore.Video.Media._ID));
				long duration = cursor.getLong(cursor
						.getColumnIndex(MediaStore.Video.Media.DURATION)); // 时长
				long size = cursor.getLong(cursor
						.getColumnIndex(MediaStore.Video.Media.SIZE)); // 文件大小
				String url = cursor.getString(cursor
						.getColumnIndex(MediaStore.Video.Media.DATA)); // 文件路径
				String displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
				if (new File(url).exists()) {
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inDither = false;
					options.inPreferredConfig = Bitmap.Config.ARGB_8888;
					// get video thumbail
					Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(
							contentResolver, id, Images.Thumbnails.MICRO_KIND,
							options);
					mediaInfo.setId(id);
					mediaInfo.setDuration(duration);
					mediaInfo.setSize(size);
					mediaInfo.setUrl(url);
					mediaInfo.setDisplayName(displayName);
					mediaInfo.setIcon(bitmap);
					list.add(mediaInfo);
				}
				
			} while (cursor.moveToNext());
		}
		cursor.close();
		return list;
	}

	/** get images form {@link MediaStore.Images.Media.EXTERNAL_CONTENT_URI} */
	public List<ImageInfo> getImageInfo() {
		Log.d(TAG, "getImageInfo.start:" + System.currentTimeMillis());
		List<ImageInfo> imageInfos = new ArrayList<ImageInfo>();
		ContentResolver contentResolver = context.getContentResolver();
		Cursor cursor = contentResolver.query(imagesUri, null, null, null,
				MediaStore.MediaColumns.DATE_MODIFIED);

		if (cursor.moveToFirst()) {
			ImageInfo imageInfo = null;
			do {
				long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID));
				String path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
				// 图片所在文件夹名
				String folder = cursor.getString(cursor
								.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME));
				long width = cursor.getLong(cursor.getColumnIndex("width"));
				long height = cursor.getLong(cursor.getColumnIndex("height"));
				if (new File(path).exists()) {
					imageInfo = new ImageInfo(id);
					imageInfo.setPath(path);
					imageInfo.setBucketDisplayName(folder);
					imageInfo.setWidth(width);
					imageInfo.setHeight(height);
					
					imageInfos.add(imageInfo);
				}

			} while (cursor.moveToNext());
		}
		cursor.close();
		Log.d(TAG, "getImageInfo.end:" + System.currentTimeMillis());
		return imageInfos;
	}

	/**
	 * get a media file total play time
	 * 
	 * @param path
	 * @return
	 */
	public long getTotalTime(String path) {
		long time = 0;
		MediaPlayer mediaPlayer = new MediaPlayer();
		try {
			mediaPlayer.reset();
			mediaPlayer.setDataSource(path);
			mediaPlayer.prepare();
			time = mediaPlayer.getDuration();
			mediaPlayer.release();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return time;
	}

	/**
	 * 获取默认专辑图片
	 * 
	 * @param context
	 * @return
	 */
	public static Bitmap getDefaultArtwork(Context context, boolean small) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Bitmap.Config.RGB_565;
		if (small) { // 返回小图片
			return BitmapFactory.decodeStream(context.getResources()
					.openRawResource(R.drawable.default_audio_iv), null, opts);
		}
		return BitmapFactory.decodeStream(context.getResources()
				.openRawResource(R.drawable.default_audio_iv), null, opts);
	}

	/**
	 * 从文件当中获取专辑封面位图
	 * 
	 * @param context
	 * @param songid
	 * @param albumid
	 * @return
	 */
	public static Bitmap getArtworkFromFile(Context context, long songid,
			long albumid) {
		Bitmap bm = null;
		if (albumid < 0 && songid < 0) {
			throw new IllegalArgumentException(
					"Must specify an album or a song id");
		}
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			FileDescriptor fd = null;
			if (albumid < 0) {
				Uri uri = Uri.parse("content://media/external/audio/media/"
						+ songid + "/albumart");
				ParcelFileDescriptor pfd = context.getContentResolver()
						.openFileDescriptor(uri, "r");
				if (pfd != null) {
					fd = pfd.getFileDescriptor();
				}
			} else {
				Uri uri = ContentUris.withAppendedId(albumArtUri, albumid);
				ParcelFileDescriptor pfd = context.getContentResolver()
						.openFileDescriptor(uri, "r");
				if (pfd != null) {
					fd = pfd.getFileDescriptor();
				}
			}
			options.inSampleSize = 1;
			// 只进行大小判断
			options.inJustDecodeBounds = true;
			// 调用此方法得到options得到图片大小
			BitmapFactory.decodeFileDescriptor(fd, null, options);
			// 我们的目标是在800pixel的画面上显示
			// 所以需要调用computeSampleSize得到图片缩放的比例
			options.inSampleSize = 100;
			// 我们得到了缩放的比例，现在开始正式读入Bitmap数据
			options.inJustDecodeBounds = false;
			options.inDither = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;

			// 根据options参数，减少所需要的内存
			bm = BitmapFactory.decodeFileDescriptor(fd, null, options);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return bm;
	}

	/**
	 * 获取专辑封面位图对象
	 * 
	 * @param context
	 * @param song_id
	 * @param album_id
	 * @param allowdefalut
	 * @return
	 */
	public static Bitmap getArtwork(Context context, long song_id,
			long album_id, boolean allowdefalut, boolean small) {
		if (album_id < 0) {
			if (song_id < 0) {
				Bitmap bm = getArtworkFromFile(context, song_id, -1);
				if (bm != null) {
					return bm;
				}
			}
			if (allowdefalut) {
				return getDefaultArtwork(context, small);
			}
			return null;
		}
		ContentResolver res = context.getContentResolver();
		Uri uri = ContentUris.withAppendedId(albumArtUri, album_id);
		if (uri != null) {
			InputStream in = null;
			try {
				in = res.openInputStream(uri);
				BitmapFactory.Options options = new BitmapFactory.Options();
				// 先制定原始大小
				options.inSampleSize = 1;
				// 只进行大小判断
				options.inJustDecodeBounds = true;
				// 调用此方法得到options得到图片的大小
				BitmapFactory.decodeStream(in, null, options);
				/** 我们的目标是在你N pixel的画面上显示。 所以需要调用computeSampleSize得到图片缩放的比例 **/
				/** 这里的target为800是根据默认专辑图片大小决定的，800只是测试数字但是试验后发现完美的结合 **/
				if (small) {
					options.inSampleSize = computeSampleSize(options, 40);
				} else {
					options.inSampleSize = computeSampleSize(options, 600);
				}
				// 我们得到了缩放比例，现在开始正式读入Bitmap数据
				options.inJustDecodeBounds = false;
				options.inDither = false;
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				in = res.openInputStream(uri);
				return BitmapFactory.decodeStream(in, null, options);
			} catch (FileNotFoundException e) {
				Bitmap bm = getArtworkFromFile(context, song_id, album_id);
				if (bm != null) {
					if (bm.getConfig() == null) {
						bm = bm.copy(Bitmap.Config.RGB_565, false);
						if (bm == null && allowdefalut) {
							return getDefaultArtwork(context, small);
						}
					}
				} else if (allowdefalut) {
					bm = getDefaultArtwork(context, small);
				}
				return bm;
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * 对图片进行合适的缩放
	 * 
	 * @param options
	 * @param target
	 * @return
	 */
	public static int computeSampleSize(Options options, int target) {
		int w = options.outWidth;
		int h = options.outHeight;
		int candidateW = w / target;
		int candidateH = h / target;
		int candidate = Math.max(candidateW, candidateH);
		if (candidate == 0) {
			return 1;
		}
		if (candidate > 1) {
			if ((w > target) && (w / candidate) < target) {
				candidate -= 1;
			}
		}
		if (candidate > 1) {
			if ((h > target) && (h / candidate) < target) {
				candidate -= 1;
			}
		}
		return candidate;
	}
}
