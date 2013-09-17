package com.dreamlink.communication.ui.db;

import android.net.Uri;
import android.provider.BaseColumns;

public class MetaData{
	public static final String DATABASE_NAME = "dream.db";
	public static final int DATABASE_VERSION = 1;

	public static final String AUTHORITY = "com.dreamlink.communication.db.comprovider";
	
	/**game table*/
	public static final class Game implements BaseColumns{
		public static final String TABLE_NAME = "game";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/game");
		public static final Uri CONTENT_FILTER_URI = Uri.parse("content://" + AUTHORITY + "/game_filter");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/game";
		public static final String CONTENT_TYPE_ITEM = "vnd.android.cursor.item/game";
		
		//items
		/**package name. Type:String*/
		public static final String PKG_NAME = "pkg_name";
		
		/**order by _id DESC*/
		public static final String SORT_ORDER_DEFAULT = _ID + " DESC"; 
	}
	
	/**
	 * table history
	 */
	public static final class History implements BaseColumns{
		public static final String TABLE_NAME = "history";
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/history");
		public static final Uri CONTENT_FILTER_URI = Uri.parse("content://" + AUTHORITY + "/history_filter");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/history";
		public static final String CONTENT_TYPE_ITEM = "vnd.android.cursor.item/history";
		
		//items
		/**
		 * file path, type:String
		 */
		public static final String FILE_PATH = "file_path";
		/**
		 * file name, type:String
		 */
		public static final String FILE_NAME = "file_name";
		/**
		 * file size, type:long
		 */
		public static final String FILE_SIZE = "file_size";
		/**
		 * send user name,type:String
		 */
		public static final String SEND_USERNAME = "send_username";
		/**
		 * receive user name,type:String
		 */
		public static final String RECEIVE_USERNAME = "receive_username";
		/**
		 * current file transfer bytes,type:double
		 */
		public static final String PROGRESS = "progress";
		/**
		 * file transfer time,mills,type:long
		 */
		public static final String DATE = "date";
		/**
		 * file transfer status, type:int
		 */
		public static final String STATUS = "status";
		/**
		 * message type,send or receive,Type:int
		 */
		public static final String MSG_TYPE  = "msg_type";
		/***/
		public static final String FILE_TYPE = "file_type";
		
		/**order by _id DESC*/
		public static final String SORT_ORDER_DEFAULT = DATE + " DESC"; 
	}
}
