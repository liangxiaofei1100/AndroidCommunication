package com.dreamlink.communication;

import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;

import com.dreamlink.aidl.User;
import com.dreamlink.communication.util.NetWorkUtil;

/**
 * Show Network Status dialog. It is used for debug.
 * 
 */
public class NetworkStatus {
	private Context mContext;
	private SocketCommunicationManager mCommunicationManager;
	private UserManager mUserManager;

	public NetworkStatus(Context context) {
		mContext = context;
		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext);
		mUserManager = mUserManager.getInstance();
	}

	public void show() {
		AlertDialog.Builder alertConfig = new AlertDialog.Builder(mContext);
		alertConfig.setIcon(mContext.getResources().getDrawable(
				R.drawable.ic_launcher));
		alertConfig.setTitle("Network status");
		alertConfig.setMessage(getStatus());

		alertConfig.create().show();
	}

	private String getAllUserNames() {
		StringBuilder stringBuilder = new StringBuilder();

		Map<Integer, User> users = mUserManager.getAllUser();
		for (Map.Entry<Integer, User> entry : users.entrySet()) {
			stringBuilder.append("User ID = " + entry.getValue().getUserID()
					+ ", name = " + entry.getValue().getUserName() + "\n");
		}
		return stringBuilder.toString();
	}

	private String getAllCommunications() {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("Commnunication info: \n");
		Map<Integer, SocketCommunication> communications = mUserManager
				.getAllCommmunication();
		for (Map.Entry<Integer, SocketCommunication> entry : communications
				.entrySet()) {
			try {
				stringBuilder.append("User ID = " + entry.getKey() + ", ip = "
						+ entry.getValue().getConnectIP().getHostAddress()
						+ "\n");
			} catch (Exception e) {
				// Because Server local communication has no ip. ignore.
			}

		}
		return stringBuilder.toString();
	}

	private String getLocalUserName() {
		StringBuilder stringBuilder = new StringBuilder();

		User localUser = mUserManager.getLocalUser();
		stringBuilder.append("User ID = " + localUser.getUserID() + ", name = "
				+ localUser.getUserName() + "\n");
		return stringBuilder.toString();
	}

	private String getStatus() {
		StringBuilder builder = new StringBuilder();
		builder.append("Total communication number: "
				+ mCommunicationManager.getCommunications().size() + "\n");
		builder.append("IP: " + NetWorkUtil.getLocalIpAddress() + "\n");
		builder.append("Me: " + getLocalUserName());
		builder.append(getAllUserNames() + "\n");
		builder.append(getAllCommunications() + "\n");
		return builder.toString();
	}
}
