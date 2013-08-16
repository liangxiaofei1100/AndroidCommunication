package com.dreamlink.communication.fileshare;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;

import com.dreamlink.aidl.OnCommunicationListenerExternal;
import com.dreamlink.aidl.User;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.SocketCommunicationManager.OnCommunicationListener;
import com.dreamlink.communication.util.AppUtil;
import com.dreamlink.communication.util.Log;

@SuppressLint("NewApi")
public class FileShareServerService extends Service implements OnCommunicationListener, OnCommunicationListenerExternal{
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
		Log.d(TAG, "onStartCommand");
		mCommunicationManager = SocketCommunicationManager
				.getInstance(this);
		mCommunicationManager.registered(this);
		mCommunicationManager.registerOnCommunicationListenerExternal(this, AppUtil.getAppID(getApplication()));
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
				// TODO need to implement appID.
				mCommunicationManager.sendMessageToAll(retMsg.getBytes(), 0);
			} else {
				// not folder,can not enter here
				return;
			}
		} else if (Command.COPY.equals(splitMsg[0])) {
			// copy file command
			Log.d(TAG, "OK,I got copy command");
			String copypath = splitMsg[1];
			File file = new File(copypath);

		} else {
			// another
			return;
		}
	}
	
	/**
	 * send file
	 * 
	 * @param file
	 */
	public void sendFile(File file) {
		Log.d(TAG, "sendFile-->" + file.getName());
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new BufferedInputStream(
					new FileInputStream(file)));
			Log.d(TAG, "open file ok.");
				Log.d(TAG, "Connection is ok");
				int bufferSize = 4 * 1024;
				byte[] buf = new byte[bufferSize];
				int read_len = 0;
				while ((read_len = dis.read(buf)) != -1) {
					Log.d(TAG, "read_len = " + read_len);
					if (read_len < bufferSize) {
						Log.d(TAG, "send file: " + read_len);
						// read length less than buff.
						mCommunicationManager.sendMessageToAll(Arrays.copyOfRange(buf, 0, read_len), 0);
					} else {
						Log.d(TAG, "send file: " + read_len);
						// read length less than buff.
						mCommunicationManager.sendMessageToAll(buf,0);
					}
				}
				Log.d(TAG, "read_len11 = " + read_len);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found exception:" + e.toString());
		} catch (IOException e) {
			Log.e(TAG, "IO exception:" + e.toString());
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
	}

	@Override
	public IBinder asBinder() {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public void onReceiveMessage(byte[] msg, User sendUser)
			throws RemoteException {
		// TODO Auto-generated method stub
		System.out.println("=============================");
	}

	@Override
	public void onUserConnected(User user) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUserDisconnected(User user) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

}
