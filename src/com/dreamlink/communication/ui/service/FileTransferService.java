package com.dreamlink.communication.ui.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.dreamlink.communication.FileReceiver;
import com.dreamlink.communication.FileReceiver.OnReceiveListener;
import com.dreamlink.communication.FileSender.OnFileSendListener;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnFileTransportListener;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.app.AppInfo;
import com.dreamlink.communication.ui.app.AppManager;
import com.dreamlink.communication.ui.common.SingleMediaScanner;
import com.dreamlink.communication.ui.db.AppData;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.history.HistoryInfo;
import com.dreamlink.communication.ui.history.HistoryManager;
import com.dreamlink.communication.util.Log;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

/**
 * 2013年9月16日 该service目前做以下几件事情 1.接受发送文件的广播
 * 2.收到广播后，根据广播中的内容，开始发送文件，然后根据发送的回调更新数据库 3.文件接收：注册文件接收监听器，边接收，边更新数据库
 */
public class FileTransferService extends Service implements
		OnFileTransportListener, OnFileSendListener, OnReceiveListener {
	private static final String TAG = "FileTransferService";

	private Notice mNotice;
	private SocketCommunicationManager mSocketMgr;
	private FileInfoManager mFileInfoManager = null;
	private HistoryManager mHistoryManager = null;
	private UserManager mUserManager = null;
	private PackageManager pm = null;

	private Map<Object, Uri> mTransferMap = new ConcurrentHashMap<Object, Uri>();
	private int mAppId = -1;

	private Map<Object, SendFileThread> mSendingFileThreadMap = new ConcurrentHashMap<Object, SendFileThread>();
	private Queue<SendFileThread> mSendQueue = new ConcurrentLinkedQueue<SendFileThread>();

	/** Thread pool */
	private ExecutorService mExecutorService = Executors.newCachedThreadPool();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	BroadcastReceiver transferReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.i(TAG, "action = " + action);
			if (DreamConstant.SEND_FILE_ACTION.equals(action)) {
				handleSendFileRequest(intent);
			} else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
				AppManager appManager = new AppManager(FileTransferService.this);
				// get install or uninstall app package name
				String packageName = intent.getData().getSchemeSpecificPart();

				// get installed app
				AppInfo appInfo = null;
				try {
					ApplicationInfo info = pm
							.getApplicationInfo(packageName, 0);
					appInfo = new AppInfo(FileTransferService.this, info);
					appInfo.setPackageName(packageName);
					appInfo.setAppIcon(info.loadIcon(pm));
					appInfo.loadLabel();
					appInfo.loadVersion();
					if (appManager.isMyApp(packageName)) {
						appInfo.setType(AppManager.ZHAOYAN_APP);
					} else if (appManager.isGameApp(packageName)) {
						appInfo.setType(AppManager.GAME_APP);
					} else {
						appInfo.setType(AppManager.NORMAL_APP);
					}
					ContentValues values = appManager
							.getValuesByAppInfo(appInfo);
					getContentResolver()
							.insert(AppData.App.CONTENT_URI, values);
				} catch (NameNotFoundException e) {
					e.printStackTrace();
				}

			} else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
				// get install or uninstall app package name
				String packageName = intent.getData().getSchemeSpecificPart();
				Uri uri = Uri
						.parse(AppData.App.CONTENT_URI + "/" + packageName);
				getContentResolver().delete(uri, null, null);
			}
		}
	};

	@Override
	public void onCreate() {
		Log.d(TAG, "onCreate");
		super.onCreate();
		// register broadcast
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_ADDED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addDataScheme("package");

		IntentFilter filter2 = new IntentFilter(DreamConstant.SEND_FILE_ACTION);

		registerReceiver(transferReceiver, filter);
		registerReceiver(transferReceiver, filter2);

		mNotice = new Notice(this);
		mFileInfoManager = new FileInfoManager(this);
		mHistoryManager = new HistoryManager(this);
		mSocketMgr = SocketCommunicationManager.getInstance(this);
		mAppId = AppUtil.getAppID(this);
		Log.d(TAG, "mappid=" + mAppId);
		mSocketMgr.registerOnFileTransportListener(this, mAppId);

		mUserManager = UserManager.getInstance();
		pm = getPackageManager();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "FileTransferService.onStartCommand()");
		return super.onStartCommand(intent, flags, startId);
	}

	public void handleSendFileRequest(Intent intent) {
		Bundle bundle;
		bundle = intent.getExtras();
		if (null != bundle) {
			List<String> pathLists = bundle
					.getStringArrayList(Extra.SEND_FILES);
			List<User> userList = bundle
					.getParcelableArrayList(Extra.SEND_USERS);
			for (String path : pathLists) {
				sendFile(path, userList);
			}

			SendFileThread thread = mSendQueue.peek();
			if (thread != null && !thread.isSendSending()) {
				mExecutorService.execute(thread);
			}
		}
	}

	public void sendFile(String path, List<User> list) {
		File file = new File(path);
		sendFile(file, list);
	}

	public void sendFile(File file, List<User> list) {
		Log.d(TAG, "sendFile.name:" + file.getName());
		for (int i = 0; i < list.size(); i++) {
			User receiverUser = list.get(i);
			Log.d(TAG, "receiverUser[" + i + "]=" + receiverUser.getUserName());
			SendFileThread sendFileThread = new SendFileThread(file,
					receiverUser);
			mSendQueue.offer(sendFileThread);
		}
	}

	class SendFileThread extends Thread {
		private File file = null;
		private User receiveUser = null;
		private Object key = null;
		private boolean isFileSending = false;

		SendFileThread(File file, User receiveUser) {
			this.file = file;
			this.receiveUser = receiveUser;
			addToSendQueue();
		}

		@Override
		public void run() {
			isFileSending = true;
			mSendingFileThreadMap.put(key, this);
			startSendFile();
		}

		private void addToSendQueue() {
			HistoryInfo historyInfo = new HistoryInfo();
			historyInfo.setFile(file);
			historyInfo.setFileSize(file.length());
			historyInfo.setReceiveUser(receiveUser);
			historyInfo.setSendUserName(getResources().getString(R.string.me));
			historyInfo.setMsgType(HistoryManager.TYPE_SEND);
			historyInfo.setDate(System.currentTimeMillis());
			historyInfo.setStatus(HistoryManager.STATUS_PRE_SEND);
			historyInfo = mFileInfoManager.getHistoryInfo(historyInfo);

			ContentValues values = mHistoryManager.getInsertValues(historyInfo);
			Uri uri = getContentResolver().insert(MetaData.History.CONTENT_URI,
					values);
			key = new Object();
			// save file & uri map
			mTransferMap.put(key, uri);
		}

		private void startSendFile() {
			// 当有一个文件要发送的时候，先将其插入到数据库表中
			ContentValues values = new ContentValues();
			values.put(MetaData.History.STATUS, HistoryManager.STATUS_PRE_SEND);
			getContentResolver().update(getFileUri(key), values, null, null);
			mSocketMgr.sendFile(file, FileTransferService.this, receiveUser,
					mAppId, key);
		}

		public boolean isSendSending() {
			return isFileSending;
		}

		public void setSendFinished() {
			isFileSending = false;
		}
	}

	@Override
	public void onReceiveFile(FileReceiver fileReceiver) {
		String fileName = fileReceiver.getFileTransferInfo().getFileName();
		String sendUserName = fileReceiver.getSendUser().getUserName();
		File file = null;
		long fileSize = fileReceiver.getFileTransferInfo().getFileSize();
		Log.d(TAG, "onReceiveFile:" + fileName + "," + sendUserName + ",size="
				+ fileSize);
		// define a file to save the receive file
		File fileDir = new File(DreamConstant.DREAMLINK_FOLDER);
		if (!fileDir.exists()) {
			fileDir.mkdirs();
		}

		String filePath = DreamConstant.DREAMLINK_FOLDER + File.separator
				+ fileName;
		file = new File(filePath);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				Log.e(TAG, "create file error:" + e.toString());
				mNotice.showToast("can not create the file:" + fileName);
				return;
			}
		} else {
			// if file is exist,auto rename
			fileName = FileInfoManager.autoRename(fileName);
			while (new File(DreamConstant.DREAMLINK_FOLDER + File.separator
					+ fileName).exists()) {
				fileName = FileInfoManager.autoRename(fileName);
			}
			filePath = DreamConstant.DREAMLINK_FOLDER + File.separator
					+ fileName;
			file = new File(filePath);
		}

		HistoryInfo historyInfo = new HistoryInfo();
		historyInfo.setFile(file);
		historyInfo.setFileSize(fileSize);
		historyInfo.setSendUserName(sendUserName);
		historyInfo.setReceiveUser(mUserManager.getLocalUser());
		historyInfo.setMsgType(HistoryManager.TYPE_RECEIVE);
		historyInfo.setDate(System.currentTimeMillis());
		historyInfo.setStatus(HistoryManager.STATUS_PRE_RECEIVE);
		historyInfo = mFileInfoManager.getHistoryInfo(historyInfo);

		ContentValues values = mHistoryManager.getInsertValues(historyInfo);
		Uri uri = getContentResolver().insert(MetaData.History.CONTENT_URI,
				values);
		Object key = new Object();
		mTransferMap.put(key, uri);
		Log.d(TAG, "onReceiveFile.mTransferMap.size=" + mTransferMap.size());

		fileReceiver.receiveFile(file, FileTransferService.this, key);
	}

	@Override
	public void onSendProgress(long sentBytes, File file, Object key) {
		ContentValues values = new ContentValues();
		values.put(MetaData.History.STATUS, HistoryManager.STATUS_SENDING);
		values.put(MetaData.History.PROGRESS, sentBytes);

		getContentResolver().update(getFileUri(key), values, null, null);
	}

	@Override
	public void onSendFinished(boolean success, File file, Object key) {
		int status;
		if (success) {
			status = HistoryManager.STATUS_SEND_SUCCESS;
		} else {
			status = HistoryManager.STATUS_SEND_FAIL;
		}

		ContentValues values = new ContentValues();
		values.put(MetaData.History.STATUS, status);
		getContentResolver().update(getFileUri(key), values, null, null);

		SendFileThread thread = mSendingFileThreadMap.get(key);
		if (thread != null) {
			thread.setSendFinished();
			mSendingFileThreadMap.remove(key);
			mSendQueue.remove();
		}

		thread = mSendQueue.peek();
		if (thread != null) {
			mExecutorService.execute(thread);
		}
	}

	/***
	 * get the transfer file'uri that in the db
	 * 
	 * @return
	 */
	public Uri getFileUri(Object key) {
		return mTransferMap.get(key);
	}

	@Override
	public void onReceiveProgress(long receivedBytes, File file, Object key) {
		ContentValues values = new ContentValues();
		values.put(MetaData.History.STATUS, HistoryManager.STATUS_SENDING);
		values.put(MetaData.History.PROGRESS, receivedBytes);
		getContentResolver().update(getFileUri(key), values, null, null);
	}

	@Override
	public void onReceiveFinished(boolean success, File file, Object key) {
		int status;
		if (success) {
			status = HistoryManager.STATUS_SEND_SUCCESS;
			new SingleMediaScanner(getApplicationContext(), file);
		} else {
			status = HistoryManager.STATUS_SEND_FAIL;
			if (file.exists()) {
				file.delete();
			}
		}

		ContentValues values = new ContentValues();
		values.put(MetaData.History.STATUS, status);
		getContentResolver().update(getFileUri(key), values, null, null);

		mTransferMap.remove(key);
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
		mSocketMgr.unregisterOnFileTransportListener(this);
		unregisterReceiver(transferReceiver);

		mSendingFileThreadMap.clear();
		mSendQueue.clear();
	}

}
