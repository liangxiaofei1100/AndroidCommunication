package com.dreamlink.communication.ui.common;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.lib.util.Notice;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.os.Bundle;

public class FileSendUtil {
	private Context context;
	
	public static final int TYPE_APK = 0;
	public static final int TYPE_IMAGE = 1;
	public static final int TYPE_MEDIA = 2;
	public static final int TYPE_FILE = 2;
	
	private UserManager mUserManager = null;
	private Notice mNotice = null;
	
	public FileSendUtil(Context context){
		this.context = context;
		mUserManager = UserManager.getInstance();
		mNotice = new Notice(context);
	}
	
	/**
	 * send file to others
	 * @param fileTransferInfo
	 */
	public void sendFile(FileTransferInfo fileTransferInfo){
		ArrayList<String> userNameList = mUserManager.getAllUserNameList();
		if (userNameList.size() == 0) {
			mNotice.showToast(R.string.connect_first);
			return;
		}else if (userNameList.size() == 1) {
			//if only one user.send directory
			ArrayList<User> userList = new ArrayList<User>();
			User user = mUserManager.getUser(userNameList.get(0));
			userList.add(user);
			System.out.println("before send.fileinfo.filepath=" + fileTransferInfo.getFilePath());
			doSend(userList, fileTransferInfo);
		}else {
			//if there are two or more user,need show dialog for user choose
			showUserChooseDialog(userNameList, fileTransferInfo);
		}
	}
	
	public void showUserChooseDialog(List<String> data, final FileTransferInfo fileInfo){
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
					doSend(userList, fileInfo);
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.create().show();
	}
	
	/**
	 * notify HistoryActivity that send file
	 * @param list the send user list
	 * @param fileInfo the file that send
	 */
	public void doSend(ArrayList<User> list, FileTransferInfo fileTransferInfo){
		Intent intent = new Intent();
		intent.setAction(DreamConstant.SEND_FILE_ACTION);
		Bundle bundle = new Bundle();
		bundle.putParcelable(Extra.SEND_FILE, fileTransferInfo);
		bundle.putParcelableArrayList(Extra.SEND_USER, list);
		intent.putExtras(bundle);
		context.sendBroadcast(intent);
	}
}
