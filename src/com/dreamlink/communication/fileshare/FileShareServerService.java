package com.dreamlink.communication.fileshare;

import java.io.File;

import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.util.Log;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;

public class FileShareServerService extends Service implements OnCommunicationListener{
	private static final String TAG = "FileShareServerService";
	private SocketCommunicationManager mCommunicationManager;
	public static boolean serviceIsStart = false;
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mCommunicationManager = SocketCommunicationManager
				.getInstance(this);
		mCommunicationManager.registered(this);
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onReceiveMessage(byte[] msg, SocketCommunication ip) {
		Log.d(TAG, "onReceiveMessage");
		serviceIsStart = true;
		
		String cmdMsg = new String(msg);

		// split msg
		String[] splitMsg = cmdMsg.split(Command.AITE);

		// command analyse
		if (Command.LS.equals(splitMsg[0])) {
			//ls command
			String path = "";
			if (Command.ROOT_PATH.equals(splitMsg[1])) {
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					path = Environment.getExternalStorageDirectory().getAbsolutePath();
				} else {
					//sdcard is not mount,do nothing
					return;
				}
			} else {
				//get the ls path
				path = splitMsg[1];
			}

			File file = new File(path);

			if (!file.exists()) {
				return;
			} else if (file.isDirectory()) {
				// send return msg
				String retMsg = "";
				
				// add head flag
				retMsg = Command.LSRETN + Command.ENTER;
				//add current path 
				retMsg += file.getPath() + Command.ENTER;
				//add parent path
				retMsg += file.getParentFile().getPath() + Command.ENTER;

				File[] files = file.listFiles();
				Log.e(TAG, "files.length=" + files.length);
				for (int i = 0; i < files.length; i++) {
					if (files[i].isHidden()) {
						//hidden file do not show
					} else {
						if (files[i].isDirectory()) {
							//directory
							// [最后修改时间],[目录标识],[目录大小],[路径][目录名称]
							retMsg += files[i].lastModified() + Command.SEPARTOR + Command.DIR_FLAG + Command.SEPARTOR + Command.DIR_SIZE
									+ Command.SEPARTOR + files[i].getAbsolutePath() + Command.SEPARTOR + files[i].getName() + Command.ENTER;
						} else {
							//file
							// [最后修改时间],[文件标识],[文件大小],[路径],[文件名称]
							retMsg += files[i].lastModified() + Command.SEPARTOR + Command.FILE_FLGA + Command.SEPARTOR + files[i].length()
									+ Command.SEPARTOR + files[i].getAbsolutePath() + Command.SEPARTOR + files[i].getName() + Command.ENTER;
						}
					}
				}
				//add end flag
				retMsg += Command.END_FLAG;
				
				//Log.d(TAG, "retMsg=" + retMsg);
				mCommunicationManager.sendMessage(retMsg.getBytes(), 0);
			} else {
				// not folder,can not enter here
				return;
			}
		} else if (Command.COPY.equals(splitMsg[0])) {
			// copy file command
			Log.d(TAG, "OK,I got copy command");
			String copypath = splitMsg[1];
			File file = new File(copypath);
			mCommunicationManager.sendMessage(file, 0);

		} else {
			// another
			return;
		}
	}

	@Override
	public void onSendResult(byte[] msg) {
		// TODO Auto-generated method stub
	}

	@Override
	public void notifyConnectChanged() {
		// TODO Auto-generated method stub
	}
	
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		serviceIsStart = false;
		
		mCommunicationManager.unregistered(this);
		mCommunicationManager.closeCommunication();
	}

}
