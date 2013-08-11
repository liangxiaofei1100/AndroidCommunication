package com.dreamlink.communication.ui.db;

import android.net.Uri;
import android.provider.BaseColumns;

public class MetaData{
	public static final String DATABASE_NAME = "game_app.db";
	public static final int DATABASE_VERSION = 1;

	public static final String AUTHORITY = "com.dreamlink.communication.db.comprovider";
	
	/**table*/
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
}
