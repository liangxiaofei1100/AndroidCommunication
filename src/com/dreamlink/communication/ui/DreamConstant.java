package com.dreamlink.communication.ui;

public class DreamConstant {

	//context action menu id
	public static final int ACTION_MENU_OPEN = 0x01;
	public static final int ACTION_MENU_SEND = 0x02;
	public static final int ACTION_MENU_DELETE = 0x03;
	public static final int ACTION_MENU_INFO = 0x04;
	public static final int ACTION_MENU_MORE = 0x05;
	public static final int ACTION_MENU_PLAY = 0x06;
	public static final int ACTION_MENU_RENAME = 0x07;
	//context action menu id
	
	public static class Extra{
		public static final String IMAGE_POSITION = "image_position";
		public static final String IMAGE_INFO = "image_info";
		
		public static final String AUDIO_SIZE = "audio_size";
		public static final String VIDEO_SIZE = "video_size";
	}
	
	public static final String MEDIA_AUDIO_ACTION = "intent.media.audio.action";
	public static final String MEDIA_VIDEO_ACTION = "intent.media.video.action";
}
