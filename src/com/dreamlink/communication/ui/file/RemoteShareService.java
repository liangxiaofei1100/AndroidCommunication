package com.dreamlink.communication.ui.file;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;

import com.dreamlink.communication.aidl.OnCommunicationListenerExternal;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.ui.Command;
import com.dreamlink.communication.ui.DreamConstant.Cmd;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.LogFile;

@SuppressLint("NewApi")
public class RemoteShareService extends Service implements OnCommunicationListenerExternal{
	private static final String TAG = "RemoteShareService";
	private SocketCommunicationManager mCommunicationManager;
	public static boolean serviceIsStart = false;
	private int mAppId = 0;
	//save file info
	/**
	 * use this list for save file info
	 */
	private List<String> fileLists = new ArrayList<String>();
	
	private static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
	private boolean mIsStopSendFile = false;
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	LogFile logFile;
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		mCommunicationManager = SocketCommunicationManager
				.getInstance(this);
		if (null != intent) {
			mAppId = intent.getIntExtra("app_id", 0);
		}
		Log.d(TAG, "onStartCommand.appid:" + mAppId);
		mCommunicationManager.registerOnCommunicationListenerExternal(this, mAppId);
		logFile = new LogFile(getApplicationContext(), "log_server.txt");
		logFile.open();
		return super.onStartCommand(intent, flags, startId);
	}

	
	/**
	 * send file
	 * 
	 * @param file
	 */
	public void sendFile(final File file) {
		Log.d(TAG, "sendFile-->" + file.getName());
		new Thread(new Runnable() {
			@Override
			public void run() {
				DataInputStream dis = null;
				try {
					dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
					Log.d(TAG, "open file ok:file.size=" + file.length());
					Log.d(TAG, "Connection is ok");
					int bufferSize = 4 * 1024;
					byte[] buf = new byte[bufferSize];
					int read_len = 0;

					int countsize = 0;
					while (!mIsStopSendFile && ((read_len = dis.read(buf)) != -1)) {
						Log.d(TAG, "mIsStopSendFile:" + mIsStopSendFile);
						countsize += read_len;
						// Log.d(TAG, "read_len = " + read_len);
						if (read_len < bufferSize) {
							// Log.d(TAG, "send file: " + read_len);
							// read length less than buff.
							mCommunicationManager.sendMessageToAll(Arrays.copyOfRange(buf, 0, read_len), mAppId);
						} else {
							// Log.d(TAG, "send file: " + read_len);
							// read length less than buff.
							mCommunicationManager.sendMessageToAll(buf, mAppId);
						}
						Log.i(TAG, countsize + "/" + file.length());
					}
					mIsStopSendFile = true;
					// Log.d(TAG, "read_len11 = " + read_len);
				} catch (FileNotFoundException e) {
					Log.e(TAG, "File not found exception:" + e.toString());
				} catch (IOException e) {
					Log.e(TAG, "IO exception:" + e.toString());
				}
			}
		}).start();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		serviceIsStart = false;
		
		mCommunicationManager.unregisterOnCommunicationListenerExternal(this);
	}

	@Override
	public IBinder asBinder() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * send command to remote connected server
	 * @param cmd command
	 * @param path file path
	 */
	public void sendCommandMsg(String cmdMsg){
		Log.d(TAG, "sendCommandMsg=>" + cmdMsg);
		if ("".equals(cmdMsg)) {
			return;
		}
		// TODO need to implement appID.
		mCommunicationManager.sendMessageToAll(cmdMsg.getBytes(), mAppId);
	}
	
	private static final int MAX_SEND_LENGTH = 20;
	public void sendCommandMsg(){
		// TODO need to implement appID.
		int size = fileLists.size();
		Log.d(TAG, "sendCommandMsg.fileList.size=" + fileLists.size());
		while (size >= MAX_SEND_LENGTH) {
			Log.d(TAG, "while.size= " + size);
			String temp = listToArray(MAX_SEND_LENGTH);
			size = size - MAX_SEND_LENGTH;
//			logFile.writeLog(temp);
//			logFile.writeLog("\n=============\n");
			logFile.writeLog(temp.getBytes());
			mCommunicationManager.sendMessageToAll(temp.getBytes(), mAppId);
		}
		
		Log.d(TAG, "out while.size=" + size);
		if (size > 0) {
			String temp = listToArray(size);
//			logFile.writeLog(temp);
//			logFile.writeLog("\n=============\n");
			logFile.writeLog(temp.getBytes());
			mCommunicationManager.sendMessageToAll(temp.getBytes(), mAppId);
		}
	}
	
	public String listToArray(int size){
		String retStr = "";
		for (int i = 0; i < size; i++) {
			retStr += fileLists.get(i) + Command.ENTER;
			Log.d(TAG, "listToArray.fileLists.get(" + i + ")=" + fileLists.get(i));
		}
		
		for (int i = 0; i < size; i++) {
			fileLists.remove(0);
		}
		return retStr;
	}
	
	@Override
	public void onReceiveMessage(byte[] msg, User sendUser) {
		serviceIsStart = true;

		String cmdMsg = new String(msg);
		Log.d(TAG, "onReceiveMessage:" + cmdMsg + ",sendUser:" + sendUser.getUserName());
		int cmd = -1;
		try {
			cmd = Integer.parseInt(cmdMsg.substring(0, 3));
		} catch (NumberFormatException e) {
			Log.e(TAG, "get cmd error:" + e.toString());
			return;
		}
		
		String path = cmdMsg.substring(3);
		doCommand(cmd, path);
	}
	
	private void doCommand(int cmd, String path){
		Log.d(TAG, "cmd=" + cmd + ":" + path);
		switch (cmd) {
		case Cmd.LS:
			// ls command
			String filePath = "";
			if (Command.ROOT_PATH.equals(path)) {
				if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					filePath = SDCARD_PATH;
				} else {
					// sdcard is not mount,do nothing
					return;
				}
			} else {
				// get the ls path
				filePath = path;
			}

			File file = new File(filePath);
			if (!file.exists()) {
				return;
			} else if (file.isDirectory()) {
				fileLists.clear();
				/*
				 * [第一行]表示显示文件列表返回标识:lsretn
				 * [第二行]表示当前目录的绝对路径:如,/mnt/sdcard,/mnt/sdcard/Android/data
				 * [第三行]表示当前目录的父目录,如果当前目录已是sdcard的根目录，则设定父目录为空
				 * [第四行...第N行]表示当前目录中的内容，或文件夹，或文件，每一行表示一个文件夹，或文件
				 * 文件夹的格式规则：[最后修改时间],[文件夹标识<DIR],[文件夹大小(0)],文件夹名称]
				 * 文件的格式规则：[最后修改时间],[文件标识],[文件大小],[文件名称]
				 * 所以解析的时候，获得一个文件或文件夹的绝对路径，可以用[第二行]+文件名称/文件夹名称
				 * [最后一行]结束表示符，只有读到改行，即表示当前目录的所有文件信息已经传输完毕，可以显示并更新UI了
				 * */
				String head = Command.LSRETN;
//				int head = Cmd.LSRETN;
				String currentPath = file.getPath();
				String parentPath;
				File parent = file.getParentFile();
				if (null == parent || path.equals(SDCARD_PATH)) {
					Log.d(TAG, "path=" + path + "  parent is null");
					parentPath = "";
				}else {
					parentPath = parent.getPath();
				}
				fileLists.add(head);
				fileLists.add(currentPath);
				fileLists.add(parentPath);

				File[] files = file.listFiles();
				if (null != files) {
					for (int i = 0; i < files.length; i++) {
						String temp = "";
						if (files[i].isHidden()) {
							// hidden file do not show
						} else {
//							Log.i(TAG, i + ":" + files[i].getName());
							if (files[i].isDirectory()) {
								// directory
								// [最后修改时间],[目录标识],[目录大小],[目录名称]
								temp = files[i].lastModified() + Command.SEPARTOR + Command.DIR_FLAG + Command.SEPARTOR + Command.DIR_SIZE
										+ Command.SEPARTOR + files[i].getName();
							} else {
								// file
								// [最后修改时间],[文件标识],[文件大小],[文件名称]
								temp = files[i].lastModified() + Command.SEPARTOR + Command.FILE_FLGA + Command.SEPARTOR + files[i].length()
										+ Command.SEPARTOR +  files[i].getName();
							}
							fileLists.add(temp);
						}
					}
				}
				// add end flag
				String endFlag = Command.END_FLAG;
				fileLists.add(endFlag);
				sendCommandMsg();
			} else {
				Log.e(TAG, "not folder,can not enter here");
				return;
			}
			break;
		case Cmd.COPY:
			mIsStopSendFile = false;
			// copy file command
			Log.d(TAG, "OK,I got copy command:" + path);
			File copyfile = new File(path);
			sendFile(copyfile);
			break;
		case Cmd.STOP_SEND_FILE:
			Log.d(TAG, "ok,you say stop,i jiu stop");
			mIsStopSendFile = true;
			//应该不需要反馈吧，有失败的情况吗？，暂时先这样吧
//			String stopRetn = Cmd.STOP_RETN + Command.STOP_SUCCESS;
//			sendCommandMsg(stopRetn);
			break;
			
		case Cmd.GET_REMOTE_SHARE_SERVICE:
			String return_cmd  = Cmd.RETURN_REMOTE_SHARE_SERVICE + "";
			sendCommandMsg(return_cmd);
			break;

		default:
			break;
		}
	}

	@Override
	public void onUserConnected(User user) throws RemoteException {
		// TODO Auto-generated method stub
		Log.d(TAG, "onUserConnected");
	}

	@Override
	public void onUserDisconnected(User user) throws RemoteException {
		// TODO Auto-generated method stub
		Log.d(TAG, "onUserDisconnected");
	}

}
