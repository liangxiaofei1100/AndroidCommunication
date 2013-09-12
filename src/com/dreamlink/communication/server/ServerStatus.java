package com.dreamlink.communication.server;

import android.app.AlertDialog;
import android.content.Context;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.util.NetWorkUtil;

/**
 * Show client status dialog. It is used for debug.
 * 
 */
public class ServerStatus {
	private Context mContext;
	private SocketCommunicationManager mCommunicationManager;

	public ServerStatus(Context context) {
		mContext = context;
		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext);
	}

	public void show() {
		AlertDialog.Builder alertConfig = new AlertDialog.Builder(mContext);
		alertConfig.setIcon(mContext.getResources().getDrawable(
				R.drawable.app_logo));
		alertConfig.setTitle("Server status");
		alertConfig.setMessage(getStatus());

		alertConfig.create().show();
	}

	private String getStatus() {
		StringBuilder builder = new StringBuilder();
		builder.append("Total communication number: "
				+ mCommunicationManager.getCommunications().size() + "\n");
		builder.append("IP: " + NetWorkUtil.getLocalIpAddress() + "\n");
		return builder.toString();
	}
}
