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
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.ui.DreamConstant.Cmd;
import com.dreamlink.communication.util.Log;

/**
 * This is file share server. </br>
 * 
 * 1. Client send commands to server, server respond to the command.</br>
 * 
 * 2. Server does not send any command to client.
 * 
 */
@SuppressLint("NewApi")
public class RemoteShareServerService extends Service implements
		OnCommunicationListenerExternal {
	private static final String TAG = "RemoteShareService";
	private SocketCommunicationManager mCommunicationManager;
	public static boolean mIsStarted = false;
	private int mAppId = 0;
	private static final String SDCARD_PATH = Environment
			.getExternalStorageDirectory().getAbsolutePath();
	private boolean mIsStopSendFile = false;
	private static final int MAX_SEND_LENGTH = 20;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
		mIsStarted = true;
		mCommunicationManager = SocketCommunicationManager.getInstance(this);
		mAppId = AppUtil.getAppID(this);
		Log.d(TAG, "oncreate, appid:" + mAppId);
		mCommunicationManager.registerOnCommunicationListenerExternal(this,
				mAppId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy");
		mIsStarted = false;

		mCommunicationManager.unregisterOnCommunicationListenerExternal(this);
	}

	@Override
	public IBinder asBinder() {
		return null;
	}

	@Override
	public void onReceiveMessage(byte[] msg, User sendUser) {
		String cmdMsg = new String(msg);
		Log.d(TAG,
				"onReceiveMessage:" + cmdMsg + ",sendUser:"
						+ sendUser.getUserName());
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

	private void doCommand(int cmd, String path) {
		Log.d(TAG, "cmd=" + cmd + ":" + path);
		switch (cmd) {
		case Cmd.LS:
			processCommandLS(path);
			break;
		case Cmd.COPY:
			processCommandCOPY(path);
			break;
		case Cmd.STOP_SEND_FILE:
			mIsStopSendFile = true;
			break;
		case Cmd.GET_REMOTE_SHARE_SERVICE:
			String return_cmd = Cmd.RETURN_REMOTE_SHARE_SERVICE + "";
			mCommunicationManager.sendMessageToAll(return_cmd.getBytes(),
					mAppId);
			break;

		default:
			break;
		}
	}

	/**
	 * Copy file command.
	 * 
	 * @param path
	 */
	private void processCommandCOPY(String path) {
		Log.d(TAG, "processCommandCOPY:" + path);
		mIsStopSendFile = false;
		File copyfile = new File(path);
		SendFileThread sendFileThread = new SendFileThread(copyfile);
		sendFileThread.start();
	}

	/**
	 * Send file use SocketCommunicationManager.
	 *
	 */
	private class SendFileThread extends Thread {
		private File mFile;

		public SendFileThread(File file) {
			mFile = file;
		}

		@Override
		public void run() {
			DataInputStream dis = null;
			try {
				dis = new DataInputStream(new BufferedInputStream(
						new FileInputStream(mFile)));
				Log.d(TAG, "open file ok:file.size=" + mFile.length());
				int bufferSize = 4 * 1024;
				byte[] buf = new byte[bufferSize];
				int read_len = 0;

				int countsize = 0;
				Log.d(TAG, "Start send file.");
				while (!mIsStopSendFile && ((read_len = dis.read(buf)) != -1)) {
					countsize += read_len;
					if (read_len < bufferSize) {
						mCommunicationManager.sendMessageToAll(
								Arrays.copyOfRange(buf, 0, read_len), mAppId);
					} else {
						mCommunicationManager.sendMessageToAll(buf, mAppId);
					}
					Log.v(TAG, countsize + "/" + mFile.length());
				}
				mIsStopSendFile = true;
				Log.d(TAG, "Send file finished.");
			} catch (FileNotFoundException e) {
				Log.e(TAG, "File not found exception:" + e.toString());
			} catch (IOException e) {
				Log.e(TAG, "IO exception:" + e.toString());
			}
		}
	}

	/**
	 * LS command.
	 * 
	 * @param path
	 */
	private void processCommandLS(String path) {
		String filePath = "";
		if (Command.ROOT_PATH.equals(path)) {
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				filePath = SDCARD_PATH;
			} else {
				// sdcard is not mount,do nothing
				Log.d(TAG, "processCommandLS sdcard is not mount.");
				return;
			}
		} else {
			// get the ls path
			filePath = path;
		}

		File file = new File(filePath);
		if (!file.exists()) {
			Log.d(TAG, "processCommandLS file is not exist. " + path);
			return;
		} else if (file.isDirectory()) {
			List<String> fileList = processLSCommandGetFileList(file, path);

			sendCommandMsg(fileList);
		} else {
			Log.e(TAG, "not folder,can not enter here");
			return;
		}
	}

	/**
	 * [第一行]表示显示文件列表返回标识:lsretn
	 * [第二行]表示当前目录的绝对路径:如,/mnt/sdcard,/mnt/sdcard/Android/data
	 * [第三行]表示当前目录的父目录,如果当前目录已是sdcard的根目录，则设定父目录为空
	 * [第四行...第N行]表示当前目录中的内容，或文件夹，或文件，每一行表示一个文件夹，或文件
	 * 文件夹的格式规则：[最后修改时间],[文件夹标识<DIR],[文件夹大小(0)],文件夹名称]
	 * 文件的格式规则：[最后修改时间],[文件标识],[文件大小],[文件名称]
	 * 所以解析的时候，获得一个文件或文件夹的绝对路径，可以用[第二行]+文件名称/文件夹名称
	 * [最后一行]结束表示符，只有读到改行，即表示当前目录的所有文件信息已经传输完毕，可以显示并更新UI了
	 */
	private List<String> processLSCommandGetFileList(File file, String path) {
		List<String> fileList = new ArrayList<String>();
		fileList.clear();
		String head = Command.LSRETN;
		String currentPath = file.getPath();
		String parentPath;
		File parent = file.getParentFile();
		if (null == parent || path.equals(SDCARD_PATH)) {
			Log.d(TAG, "path=" + path + "  parent is null");
			parentPath = "";
		} else {
			parentPath = parent.getPath();
		}
		fileList.add(head);
		fileList.add(currentPath);
		fileList.add(parentPath);

		File[] files = file.listFiles();
		if (null != files) {
			for (int i = 0; i < files.length; i++) {
				String temp = "";
				if (files[i].isHidden()) {
					// hidden file do not show
				} else {
					// Log.i(TAG, i + ":" + files[i].getName());
					if (files[i].isDirectory()) {
						// directory
						// [最后修改时间],[目录标识],[目录大小],[目录名称]
						temp = files[i].lastModified() + Command.SEPARTOR
								+ Command.DIR_FLAG + Command.SEPARTOR
								+ Command.DIR_SIZE + Command.SEPARTOR
								+ files[i].getName();
					} else {
						// file
						// [最后修改时间],[文件标识],[文件大小],[文件名称]
						temp = files[i].lastModified() + Command.SEPARTOR
								+ Command.FILE_FLGA + Command.SEPARTOR
								+ files[i].length() + Command.SEPARTOR
								+ files[i].getName();
					}
					fileList.add(temp);
				}
			}
		}
		// add end flag
		String endFlag = Command.END_FLAG;
		fileList.add(endFlag);
		return fileList;
	}

	/**
	 * Because the max length of message is 10K. for more see
	 * {@code SocketCommunication}. So we should make the message length shorter
	 * than that length.
	 * 
	 * @param fileList
	 */
	private void sendCommandMsg(List<String> fileList) {
		int size = fileList.size();
		Log.d(TAG, "sendCommandMsg.fileList.size=" + fileList.size());
		while (size >= MAX_SEND_LENGTH) {
			Log.d(TAG, "while.size= " + size);
			String temp = listToArray(fileList, MAX_SEND_LENGTH);
			size = size - MAX_SEND_LENGTH;
			mCommunicationManager.sendMessageToAll(temp.getBytes(), mAppId);
		}

		Log.d(TAG, "out while.size=" + size);
		if (size > 0) {
			String temp = listToArray(fileList, size);
			mCommunicationManager.sendMessageToAll(temp.getBytes(), mAppId);
		}
	}

	private String listToArray(List<String> fileList, int size) {
		String retStr = "";
		for (int i = 0; i < size; i++) {
			retStr += fileList.get(i) + Command.ENTER;
			Log.d(TAG,
					"listToArray.fileLists.get(" + i + ")=" + fileList.get(i));
		}

		for (int i = 0; i < size; i++) {
			fileList.remove(0);
		}
		return retStr;
	}

	@Override
	public void onUserConnected(User user) throws RemoteException {
		Log.d(TAG, "onUserConnected");
	}

	@Override
	public void onUserDisconnected(User user) throws RemoteException {
		Log.d(TAG, "onUserDisconnected");
	}

}
