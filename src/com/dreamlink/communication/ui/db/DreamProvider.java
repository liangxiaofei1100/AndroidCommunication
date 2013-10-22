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

public class DreamProvider extends ContentProvider {
	private static final String TAG = "DreamProvider";

	private SQLiteDatabase mSqLiteDatabase;
	private DatabaseHelper mDatabaseHelper;

	public static final int HISTORY_COLLECTION = 10;
	public static final int HISTORY_SINGLE = 11;
	public static final int HISTORY_FILTER = 12;

	public static final int TRAFFIC_STATICS_RX_COLLECTION = 20;
	public static final int TRAFFIC_STATICS_RX_SINGLE = 21;
	public static final int TRAFFIC_STATICS_TX_COLLECTION = 22;
	public static final int TRAFFIC_STATICS_TX_SINGLE = 23;

	public static final UriMatcher uriMatcher;

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(MetaData.AUTHORITY, "history", HISTORY_COLLECTION);
		uriMatcher.addURI(MetaData.AUTHORITY, "history/*", HISTORY_SINGLE);
		uriMatcher.addURI(MetaData.AUTHORITY, "history_filter/*",
				HISTORY_FILTER);

		uriMatcher.addURI(MetaData.AUTHORITY, "trafficstatics_rx",
				TRAFFIC_STATICS_RX_COLLECTION);
		uriMatcher.addURI(MetaData.AUTHORITY, "trafficstatics_rx/#",
				TRAFFIC_STATICS_RX_SINGLE);
		uriMatcher.addURI(MetaData.AUTHORITY, "trafficstatics_tx",
				TRAFFIC_STATICS_TX_COLLECTION);
		uriMatcher.addURI(MetaData.AUTHORITY, "trafficstatics_tx/#",
				TRAFFIC_STATICS_TX_SINGLE);

	}

	@Override
	public boolean onCreate() {
		mDatabaseHelper = new DatabaseHelper(getContext());
		return (mDatabaseHelper == null) ? false : true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		mSqLiteDatabase = mDatabaseHelper.getWritableDatabase();
		String table;

		switch (uriMatcher.match(uri)) {
		case HISTORY_COLLECTION:
			table = MetaData.History.TABLE_NAME;
			break;
		case HISTORY_SINGLE:
			table = MetaData.History.TABLE_NAME;
			String segment2 = uri.getPathSegments().get(1);
			if (selection != null && segment2.length() > 0) {
				selection = "_id=" + segment2 + " AND (" + selection + ")";
			} else {
				// 由于segment是个string，那么需要给他加个'',如果是int型的就不需要了
				// selection = "pkg_name='" + segment + "'";
			}
			break;

		case TRAFFIC_STATICS_RX_SINGLE:
			table = MetaData.TrafficStaticsRX.TABLE_NAME;
			selection = "_id=" + uri.getPathSegments().get(1);
			selectionArgs = null;
			break;
		case TRAFFIC_STATICS_RX_COLLECTION:
			table = MetaData.TrafficStaticsRX.TABLE_NAME;
			break;
		case TRAFFIC_STATICS_TX_SINGLE:
			table = MetaData.TrafficStaticsTX.TABLE_NAME;
			selection = "_id=" + uri.getPathSegments().get(1);
			selectionArgs = null;
			break;
		case TRAFFIC_STATICS_TX_COLLECTION:
			table = MetaData.TrafficStaticsTX.TABLE_NAME;
			break;
		default:
			throw new IllegalArgumentException("UnKnow Uri:" + uri);
		}

		int count = mSqLiteDatabase.delete(table, selection, selectionArgs);
		if (count > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}

		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case HISTORY_COLLECTION:
			return MetaData.History.CONTENT_TYPE;
		case HISTORY_SINGLE:
			return MetaData.History.CONTENT_TYPE_ITEM;

		case TRAFFIC_STATICS_TX_COLLECTION:
			return MetaData.TrafficStaticsRX.CONTENT_TYPE;
		case TRAFFIC_STATICS_RX_SINGLE:
			return MetaData.TrafficStaticsRX.CONTENT_TYPE_ITEM;
		default:
			throw new IllegalArgumentException("Unkonw uri:" + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		Log.v(TAG, "insert db");
		mSqLiteDatabase = mDatabaseHelper.getWritableDatabase();
		String table;
		Uri contentUri;

		switch (uriMatcher.match(uri)) {
		case HISTORY_COLLECTION:
		case HISTORY_SINGLE:
			table = MetaData.History.TABLE_NAME;
			contentUri = MetaData.History.CONTENT_URI;
			break;

		case TRAFFIC_STATICS_RX_SINGLE:
		case TRAFFIC_STATICS_RX_COLLECTION:
			table = MetaData.TrafficStaticsRX.TABLE_NAME;
			contentUri = MetaData.TrafficStaticsRX.CONTENT_URI;
			break;

		case TRAFFIC_STATICS_TX_SINGLE:
		case TRAFFIC_STATICS_TX_COLLECTION:
			table = MetaData.TrafficStaticsTX.TABLE_NAME;
			contentUri = MetaData.TrafficStaticsTX.CONTENT_URI;
			break;
		default:
			throw new IllegalArgumentException("Unknow uri:" + uri);
		}

		long rowId = mSqLiteDatabase.insertWithOnConflict(table, "", values,
				SQLiteDatabase.CONFLICT_REPLACE);
		if (rowId > 0) {
			Uri rowUri = ContentUris.withAppendedId(contentUri, rowId);
			getContext().getContentResolver().notifyChange(uri, null);
			Log.v(TAG, "insertDb.rowId=" + rowId);
			return rowUri;
		}
		throw new IllegalArgumentException("Cannot insert into uri:" + uri);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (uriMatcher.match(uri)) {
		case HISTORY_COLLECTION:
			qb.setTables(MetaData.History.TABLE_NAME);
			break;
		case HISTORY_SINGLE:
			qb.setTables(MetaData.History.TABLE_NAME);
			qb.appendWhere("_id=");
			qb.appendWhere(uri.getPathSegments().get(1));
			break;
		case HISTORY_FILTER:
			break;

		case TRAFFIC_STATICS_RX_SINGLE:
			qb.setTables(MetaData.TrafficStaticsRX.TABLE_NAME);
			qb.appendWhere("_id=" + uri.getPathSegments().get(1));
			break;
		case TRAFFIC_STATICS_RX_COLLECTION:
			qb.setTables(MetaData.TrafficStaticsRX.TABLE_NAME);
			break;

		case TRAFFIC_STATICS_TX_SINGLE:
			qb.setTables(MetaData.TrafficStaticsTX.TABLE_NAME);
			qb.appendWhere("_id=" + uri.getPathSegments().get(1));
			break;
		case TRAFFIC_STATICS_TX_COLLECTION:
			qb.setTables(MetaData.TrafficStaticsTX.TABLE_NAME);
			break;
		default:
			throw new IllegalArgumentException("Unknow uri:" + uri);
		}

		mSqLiteDatabase = mDatabaseHelper.getReadableDatabase();
		Cursor ret = qb.query(mSqLiteDatabase, projection, selection,
				selectionArgs, null, null, sortOrder);

		if (ret != null) {
			ret.setNotificationUri(getContext().getContentResolver(), uri);
		}

		return ret;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int match = uriMatcher.match(uri);
		mSqLiteDatabase = mDatabaseHelper.getWritableDatabase();
		String table;

		switch (match) {
		case HISTORY_SINGLE:
			table = MetaData.History.TABLE_NAME;
			selection = "_id=" + uri.getPathSegments().get(1);
			selectionArgs = null;
			break;
		case HISTORY_COLLECTION:
			table = MetaData.History.TABLE_NAME;
			break;

		case TRAFFIC_STATICS_RX_SINGLE:
			table = MetaData.TrafficStaticsRX.TABLE_NAME;
			selection = "_id=" + uri.getPathSegments().get(1);
			selectionArgs = null;
			break;
		case TRAFFIC_STATICS_RX_COLLECTION:
			table = MetaData.TrafficStaticsRX.TABLE_NAME;
			break;
		case TRAFFIC_STATICS_TX_SINGLE:
			table = MetaData.TrafficStaticsTX.TABLE_NAME;
			selection = "_id=" + uri.getPathSegments().get(1);
			selectionArgs = null;
			break;
		case TRAFFIC_STATICS_TX_COLLECTION:
			table = MetaData.TrafficStaticsTX.TABLE_NAME;
			break;

		default:
			throw new UnsupportedOperationException("Cannot update uri:" + uri);
		}
		int count = mSqLiteDatabase.update(table, values, selection, null);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, MetaData.DATABASE_NAME, null,
					MetaData.DATABASE_VERSION);
			Log.d(TAG, "DatabaseHelper");
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG, "DatabaseHelper.onCreate");

			// create history table
			db.execSQL("create table " + MetaData.History.TABLE_NAME
					+ " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ MetaData.History.FILE_PATH + " TEXT, "
					+ MetaData.History.FILE_NAME + " TEXT, "
					+ MetaData.History.FILE_SIZE + " LONG, "
					+ MetaData.History.SEND_USERNAME + " TEXT, "
					+ MetaData.History.RECEIVE_USERNAME + " TEXT, "
					+ MetaData.History.PROGRESS + " LONG, "
					+ MetaData.History.DATE + " LONG, "
					+ MetaData.History.STATUS + " INTEGER, "
					+ MetaData.History.MSG_TYPE + " INTEGER, "
					+ MetaData.History.FILE_TYPE + " INTEGER);");

			// create traffic statics rx table.
			db.execSQL("create table " + MetaData.TrafficStaticsRX.TABLE_NAME
					+ " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ MetaData.TrafficStaticsRX.DATE + " TEXT UNIQUE, "
					+ MetaData.TrafficStaticsRX.TOTAL_RX_BYTES + " LONG);");

			// create traffic statics tx table.
			db.execSQL("create table " + MetaData.TrafficStaticsTX.TABLE_NAME
					+ " ( _id INTEGER PRIMARY KEY AUTOINCREMENT, "
					+ MetaData.TrafficStaticsTX.DATE + " TEXT UNIQUE, "
					+ MetaData.TrafficStaticsTX.TOTAL_TX_BYTES + " LONG);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + MetaData.History.TABLE_NAME);
			onCreate(db);
		}

	}

}
