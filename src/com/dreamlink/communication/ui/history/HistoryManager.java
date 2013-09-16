package com.dreamlink.communication.ui.history;

import java.text.NumberFormat;
import java.util.Comparator;

import com.dreamlink.communication.ui.db.MetaData;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

public class HistoryManager {
		//status
		public static final int STATUS_DEFAULT = 0;
		public static final int STATUS_PRE_SEND = 10;
		public static final int STATUS_SENDING = 11;
		public static final int STATUS_SEND_SUCCESS = 12;
		public static final int STATUS_SEND_FAIL = 13;
		
		public static final int STATUS_PRE_RECEIVE = 21;
		public static final int STATUS_RECEIVING = 22;
		public static final int STATUS_RECEIVE_SUCCESS = 23;
		public static final int STATUS_RECEIVE_FAIL = 24;
		
		public static final NumberFormat nf = NumberFormat.getPercentInstance();
		
		public static final int TYPE_SEND = 0;
		public static final int TYPE_RECEIVE = 1;
		
		public static final String ME = "ME";
		
		private Context mContext;
		
		/**
	     * Perform alphabetical comparison of application entry objects.
	     */
	    public static final Comparator<HistoryInfo> DATE_COMPARATOR = new Comparator<HistoryInfo>() {
	        @Override
	        public int compare(HistoryInfo object1, HistoryInfo object2) {
	        	long date1 = object1.getDate();
				long date2 = object2.getDate();
				if (date1 > date2) {
					return -1;
				} else if (date1 == date2) {
					return 0;
				} else {
					return 1;
				}
	        }
	    };
	    
	    public HistoryManager(Context context){
	    	mContext = context;
	    }
	    
	    public  synchronized void insertToDb(HistoryInfo historyInfo){
	    	InsertThread insertThread = new InsertThread(historyInfo);
	    	insertThread.start();
	    }
	    
	    public void deleteItemFromDb(int id){
	    	Uri uri = Uri.parse(MetaData.History.CONTENT_URI + "/" + id);
			mContext.getContentResolver().delete(uri, null, null);
	    }
	    
	    class InsertThread extends Thread{
	    	HistoryInfo historyInfo = null;
	    	
	    	InsertThread(HistoryInfo historyInfo){
	    		this.historyInfo = historyInfo;
	    	}
	    	
	    	@Override
	    	public void run() {
	    		ContentValues values = new ContentValues();
	    		values.put(MetaData.History.FILE_PATH, historyInfo.getFile().getAbsolutePath());
	    		values.put(MetaData.History.FILE_NAME, historyInfo.getFile().getName());
	    		values.put(MetaData.History.FILE_SIZE, historyInfo.getFile().length());
	    		values.put(MetaData.History.SEND_USERNAME, historyInfo.getSendUserName());
	    		values.put(MetaData.History.RECEIVE_USERNAME, historyInfo.getReceiveUser().getUserName());
	    		values.put(MetaData.History.PROGRESS, historyInfo.getProgress());
	    		values.put(MetaData.History.DATE, historyInfo.getDate());
	    		values.put(MetaData.History.STATUS, historyInfo.getStatus());
	    		values.put(MetaData.History.MSG_TYPE, historyInfo.getMsgType());
	    		
	    		mContext.getContentResolver().insert(MetaData.History.CONTENT_URI, values);
	    	}
	    }
	    
	    public ContentValues getInsertValues(HistoryInfo historyInfo){
	    	ContentValues values = new ContentValues();
	    	values.put(MetaData.History.FILE_PATH, historyInfo.getFile().getAbsolutePath());
    		values.put(MetaData.History.FILE_NAME, historyInfo.getFile().getName());
    		values.put(MetaData.History.FILE_SIZE, historyInfo.getFileSize());
    		values.put(MetaData.History.SEND_USERNAME, historyInfo.getSendUserName());
    		values.put(MetaData.History.RECEIVE_USERNAME, historyInfo.getReceiveUser().getUserName());
    		values.put(MetaData.History.PROGRESS, historyInfo.getProgress());
    		values.put(MetaData.History.DATE, historyInfo.getDate());
    		values.put(MetaData.History.STATUS, historyInfo.getStatus());
    		values.put(MetaData.History.MSG_TYPE, historyInfo.getMsgType());
    		values.put(MetaData.History.FILE_TYPE, historyInfo.getFileType());
    		
    		return values;
	    }
}
