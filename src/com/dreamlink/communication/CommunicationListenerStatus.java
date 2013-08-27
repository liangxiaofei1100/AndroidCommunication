package com.dreamlink.communication;

import android.app.AlertDialog;
import android.content.Context;

/**
 * Show local communication listener status dialog. It is used for debug.
 * 
 */
public class CommunicationListenerStatus {
	private Context mContext;
	private SocketCommunicationManager mCommunicationManager;

	public CommunicationListenerStatus(Context context) {
		mContext = context;
		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext);
	}

	public void show() {
		AlertDialog.Builder alertConfig = new AlertDialog.Builder(mContext);
		alertConfig.setIcon(mContext.getResources().getDrawable(
				R.drawable.ic_launcher));
		alertConfig.setTitle("Communication listeners status");
		alertConfig.setMessage(getStatus());

		alertConfig.create().show();
	}

	private String getStatus() {
		StringBuilder builder = new StringBuilder();
		builder.append("Listeners:\n");
		builder.append(mCommunicationManager.getOnCommunicationListenerStatus()
				+ "\n");
		builder.append("External listeners: \n");
		builder.append(mCommunicationManager
				.getOnCommunicationListenerExternalStatus() + "\n");

		return builder.toString();
	}
}
