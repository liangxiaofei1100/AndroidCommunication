package com.dreamlink.communication.ui.service;

import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.util.Log;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

public class FileManagerService extends Service {
	private static final String TAG = FileManagerService.class.getName();

	private IBinder mBinder = new ServiceBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	private static class FileManagerActivityInfo {
//        private BaseAsyncTask mTask = null;
        private FileInfoManager mFileInfoManager = null;
        private int mFilterType = 0;

//        public void setTask(BaseAsyncTask task) {
//            this.mTask = task;
//        }

        public void setFileInfoManager(FileInfoManager fileInfoManager) {
            this.mFileInfoManager = fileInfoManager;
        }

        public void setFilterType(int filterType) {
            this.mFilterType = filterType;
        }

//        BaseAsyncTask getTask() {
//            return mTask;
//        }

        FileInfoManager getFileInfoManager() {
            return mFileInfoManager;
        }

        int getFilterType() {
            return mFilterType;
        }
    }
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	public class ServiceBinder extends Binder{
		public FileManagerService getServiceInstance(){
			return FileManagerService.this;
		}
	}
	
	public void disconnected(String activiyName){
		Log.d(TAG, "disconnected:" + activiyName);
	}

}
