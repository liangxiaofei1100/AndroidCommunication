package com.dreamlink.communication;

import android.app.AlertDialog;
import android.content.Context;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.util.NetWorkUtil;

/**
 * Show Network Status dialog. It is used for debug.
 * 
 */
public class NetworkStatus {
	private Context mContext;
	private SocketCommunicationManager mCommunicationManager;

	public NetworkStatus(Context context) {
		mContext = context;
		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext);
	}

	public void show() {
		AlertDialog.Builder alertConfig = new AlertDialog.Builder(mContext);
		alertConfig.setIcon(mContext.getResources().getDrawable(
				R.drawable.ic_launcher));
		alertConfig.setTitle("Network status");
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
