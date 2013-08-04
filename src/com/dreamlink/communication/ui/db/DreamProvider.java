package com.dreamlink.communication.ui.db;

import com.dreamlink.communication.util.Log;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;

public class DreamProvider extends ContentProvider {
	private static final String TAG = "ComProvider";
	
	private SQLiteDatabase mSqLiteDatabase;
	private DatabaseHelper mDatabaseHelper;
	
	public static final int GAME_COLLECTION = 1;
	public static final int GAME_SINGLE = 2;
	public static final int GAME_FILTER = 5;
	
	public static final UriMatcher uriMatcher;
	
	static{
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(MetaData.AUTHORITY, "game", GAME_COLLECTION);
		uriMatcher.addURI(MetaData.AUTHORITY, "game/*", GAME_SINGLE);
		uriMatcher.addURI(MetaData.AUTHORITY, "game_filter/*", GAME_FILTER);
	}
	
	@Override
	public boolean onCreate() {
		mDatabaseHelper = new DatabaseHelper(getContext());
		return (mDatabaseHelper == null) ? false : true;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		mSqLiteDatabase = mDatabaseHelper.getWritableDatabase();
		
		int count = 0;
		switch (uriMatcher.match(uri)) {
		case GAME_COLLECTION:
			count = mSqLiteDatabase.delete(MetaData.Game.TABLE_NAME, selection, selectionArgs);
			break;
		case GAME_SINGLE:
			String segment = uri.getPathSegments().get(1);
			if (selection != null && segment.length() > 0) {
				selection = "_id=" + segment + " AND (" + selection + ")";
			}else {
				//由于segment是个string，那么需要给他加个'',如果是int型的就不需要了
				selection = "pkg_name='" +  segment + "'";
			}
			count = mSqLiteDatabase.delete(MetaData.Game.TABLE_NAME, selection, selectionArgs);
			break;
			
		default:
			throw new IllegalArgumentException("UnKnow Uri:" + uri);
		}
		
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case GAME_COLLECTION:
			return MetaData.Game.CONTENT_TYPE;
		case GAME_SINGLE:
			return MetaData.Game.CONTENT_TYPE_ITEM;
		default:
			throw new IllegalArgumentException("Unkonw uri:" + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.d(TAG, "insert db");
		switch (uriMatcher.match(uri)) {
		case GAME_COLLECTION:
		case GAME_SINGLE:
			mSqLiteDatabase = mDatabaseHelper.getWritableDatabase();
			long rowId = mSqLiteDatabase.insertWithOnConflict(MetaData.Game.TABLE_NAME, "", 
					values, SQLiteDatabase.CONFLICT_REPLACE);
			if (rowId > 0) {
				Uri rowUri = ContentUris.withAppendedId(MetaData.Game.CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(uri, null);
				return rowUri;
			}
			throw new IllegalArgumentException("Cannot insert into uri:" + uri);
		default:
			throw new IllegalArgumentException("Unknow uri:" + uri);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		switch (uriMatcher.match(uri)) {
		case GAME_COLLECTION:
			qb.setTables(MetaData.Game.TABLE_NAME);
			break;
		case GAME_SINGLE:
			qb.setTables(MetaData.Game.TABLE_NAME);
			qb.appendWhere("_id=");
			qb.appendWhere(uri.getPathSegments().get(1));
			break;
		case GAME_FILTER:
			qb.setTables(MetaData.Game.TABLE_NAME);
			qb.appendWhere(MetaData.Game.PKG_NAME + " like \'%"
					+ uri.getPathSegments().get(1) + "%\'");
			break;
		default:
			throw new IllegalArgumentException("Unknow uri:" + uri);
		}
		
		mSqLiteDatabase = mDatabaseHelper.getReadableDatabase();
		Cursor ret = qb.query(mSqLiteDatabase, projection, selection, selectionArgs, null, null, sortOrder);
		
		if (ret != null) {
			ret.setNotificationUri(getContext().getContentResolver(), uri);
		}
		
		return ret;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int count;
		long rowId = 0;
		int match = uriMatcher.match(uri);
		mSqLiteDatabase = mDatabaseHelper.getWritableDatabase();
		
		switch (match) {
		case GAME_SINGLE:
			String segment = uri.getPathSegments().get(1);
			rowId = Long.parseLong(segment);
			count = mSqLiteDatabase.update(MetaData.Game.TABLE_NAME, values, "_id=" + rowId, null);
			break;
		case GAME_COLLECTION:
			count = mSqLiteDatabase.update(MetaData.Game.TABLE_NAME, values, selection, null);
			break;
		default:
			throw new UnsupportedOperationException("Cannot update uri:" + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		
		return count;
	}
	
	
	private static class DatabaseHelper extends SQLiteOpenHelper{

		public DatabaseHelper(Context context) {
			super(context, MetaData.DATABASE_NAME, null, MetaData.DATABASE_VERSION);
			Log.d(TAG, "DatabaseHelper");
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "DatabaseHelper.onCreate");
			//notes table
			db.execSQL("create table " + MetaData.Game.TABLE_NAME
					+ " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ MetaData.Game.PKG_NAME + " TEXT);"
					);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + MetaData.Game.TABLE_NAME);
			onCreate(db);
		}
		
	}

}
