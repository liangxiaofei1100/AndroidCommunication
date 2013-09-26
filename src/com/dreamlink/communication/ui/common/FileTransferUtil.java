package com.dreamlink.communication.ui.common;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.lib.util.Notice;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;

public class FileTransferUtil {
	private static final String TAG = "FileSendUtil";
	private Context context;
	
	public static final int TYPE_APK = 0;
	public static final int TYPE_IMAGE = 1;
	public static final int TYPE_MEDIA = 2;
	public static final int TYPE_FILE = 2;
	
	public static final int MAX_TRANSFER_NUM = 5;
	
	private UserManager mUserManager = null;
	private Notice mNotice = null;
	private SocketCommunicationManager mSocketMgr = null;
	
	public FileTransferUtil(Context context){
		this.context = context;
		mUserManager = UserManager.getInstance();
		mNotice = new Notice(context);
		mSocketMgr = SocketCommunicationManager.getInstance(context);
	}
	
	/**
	 * send file to others
	 * @param path file path
	 */
	public void sendFile(String path){
		File file = new File(path);
		sendFile(file);
	}
	
	/**
	 * send file to others
	 * @param file file
	 */
	public void sendFile(File file){
		ArrayList<String> filePathList = new ArrayList<String>();
		filePathList.add(file.getAbsolutePath());
		
		sendFiles(filePathList);
	}
	
	/**
	 * send multi file
	 * @param files the file path list
	 */
	public void sendFiles(ArrayList<String> files){
		if (!mSocketMgr.isConnected()) {
			mNotice.showToast(R.string.connect_first);
			return;
		}
		
		ArrayList<String> userNameList = mUserManager.getAllUserNameList();
		if (userNameList.size() == 1) {
			// if only one user.send directory
			ArrayList<User> userList = new ArrayList<User>();
			User user = mUserManager.getUser(userNameList.get(0));
			userList.add(user);
			doTransferFiles(userList, files);
		} else {
			// if there are two or more user,need show dialog for user choose
			showUserChooseDialog(userNameList, files);
		}
	}
	
	public void showUserChooseDialog(List<String> data, final ArrayList<String> filePathList){
		final String[] items = new String[data.size()];
		final boolean[] checkes = new boolean[data.size()];
		for (int i = 0; i < data.size(); i++) {
			items[i] = data.get(i);
			checkes[i] = true;
		}
		new AlertDialog.Builder(context)
			.setTitle(R.string.user_list)
			.setMultiChoiceItems(items, checkes, new OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					//TODO
				}
			})
			.setPositiveButton(R.string.menu_send, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					ArrayList<User> userList = new ArrayList<User>();
					for (int i = 0; i < checkes.length; i++) {
						if (checkes[i]) {
							User user = mUserManager.getUser(items[i]);
							userList.add(user);
						}
					}
					doTransferFiles(userList, filePathList);
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.create().show();
	}
	
	/**
	 * notify HistoryActivity that send file
	 * @param list the send user list
	 * @param files the file path list that need to transfer
	 */
	public void doTransferFiles(ArrayList<User> userList, ArrayList<String> files){
		Intent intent = new Intent();
		intent.setAction(DreamConstant.SEND_FILE_ACTION);
		Bundle bundle = new Bundle();
		bundle.putStringArrayList(Extra.SEND_FILES, files);
		bundle.putParcelableArrayList(Extra.SEND_USERS, userList);
		intent.putExtras(bundle);
		context.sendBroadcast(intent);
	}
}
