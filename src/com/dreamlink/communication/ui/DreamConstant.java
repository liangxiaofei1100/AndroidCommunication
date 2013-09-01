package com.dreamlink.communication.ui;

import android.net.Uri;
import android.provider.MediaStore;

public class DreamConstant {

	// context action menu id
	public static final int ACTION_MENU_OPEN = 0x01;
	public static final int ACTION_MENU_SEND = 0x02;
	public static final int ACTION_MENU_DELETE = 0x03;
	public static final int ACTION_MENU_INFO = 0x04;
	public static final int ACTION_MENU_MORE = 0x05;
	public static final int ACTION_MENU_PLAY = 0x06;
	public static final int ACTION_MENU_RENAME = 0x07;

	// context action menu id

	public static class Extra {
		public static final String IMAGE_POSITION = "image_position";
		public static final String IMAGE_INFO = "image_info";

		public static final String AUDIO_SIZE = "audio_size";
		public static final String VIDEO_SIZE = "video_size";

		public static final String CAMERA_SIZE = "camera_size";
		public static final String GALLERY_SIZE = "gallery_size";
		
		public static final String IS_SERVER = "is_server";
		
		public static final String IS_FIRST_START = "is_first_start";
		
		public static final String COPY_PATH = "copy_path";
	}
	
	public static class Cmd{
		/** List files of current directory */
		public static final int LS = 100;
		/**copy command*/
		public static final int COPY = 101;
		/**stop send file command*/
		public static final int STOP_SEND_FILE = 102;
		/**LRETN*/
		public static final int LSRETN = 103;
		/**stop return*/
		public static final int STOP_RETN = 104;
		/**end flag*/
		public static final int END_FLAG = 105;
	}

	public static final String MEDIA_AUDIO_ACTION = "intent.media.audio.action";
	public static final String MEDIA_VIDEO_ACTION = "intent.media.video.action";
	public static final String SERVER_CREATED_ACTION = "com.dreamlink.communication.server.created";

	public static final String FILE_EX = "file://";
	public static final String ENTER = "\n";

	public static final boolean CACHE = false;

	public static final Uri AUDIO_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
	public static final Uri VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
	public static final Uri IMAGE_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

	/** package name for this app */
	public static final String PACKAGE_NAME = "com.dreamlink.communication";
	
	public static final String EXIT_ACTION = "intent.exit.aciton";
	public static final String APP_ACTION = "com.dreamlink.communication.action.app";
	public static final String APP_CATEGORY = "com.dreamlink.communication.category.app";

	/**test for yuri,only for yuri*/
	public static final boolean UREY_TEST = true;
}
