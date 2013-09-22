package com.dreamlink.communication.debug;

import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.util.NetWorkUtil;

/**
 * Show Network Status. It is used for debug.
 * 
 */
public class NetworkStatus extends Activity {
	private Context mContext;
	private SocketCommunicationManager mCommunicationManager;
	private UserManager mUserManager;

	private TextView mIpTextView;
	private TextView mUserTextView;
	private TextView mCommunicationTextView;

	public NetworkStatus() {
		super();
	}

	public NetworkStatus(Context context) {
		mContext = context;
		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext);
		mUserManager = UserManager.getInstance();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debug_network_status);
		mContext = this;
		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext);
		mUserManager = UserManager.getInstance();

		initView();
		updateNetworkStatus();
	}

	private void updateNetworkStatus() {
		mIpTextView.setText(getIpStatus());
		mUserTextView.setText(getUserStatus());
		mCommunicationTextView.setText(getCommunicationStatus());
	}

	private void initView() {
		mIpTextView = (TextView) findViewById(R.id.tv_debug_network_ip);
		mUserTextView = (TextView) findViewById(R.id.tv_debug_network_users);
		mCommunicationTextView = (TextView) findViewById(R.id.tv_debug_network_communication);
	}

	/**
	 * show as dialog.
	 */
	public void showDialog() {
		AlertDialog.Builder alertConfig = new AlertDialog.Builder(mContext);
		alertConfig.setIcon(mContext.getResources().getDrawable(
				R.drawable.app_logo));
		alertConfig.setTitle("Network status");
		alertConfig.setMessage(getStatus());

		alertConfig.create().show();
	}

	private String getUserStatus() {
		StringBuilder stringBuilder = new StringBuilder();

		User localUser = mUserManager.getLocalUser();
		stringBuilder.append("My User ID = " + localUser.getUserID()
				+ ", name = " + localUser.getUserName() + "\n");

		Map<Integer, User> users = mUserManager.getAllUser();
		for (Map.Entry<Integer, User> entry : users.entrySet()) {
			stringBuilder.append("User ID = " + entry.getValue().getUserID()
					+ ", name = " + entry.getValue().getUserName() + "\n");
		}
		return stringBuilder.toString();
	}

	private String getCommunicationStatus() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Total communication number: "
				+ mCommunicationManager.getCommunications().size()
				+ ", All commnunication info: \n");
		Map<Integer, SocketCommunication> communications = mUserManager
				.getAllCommmunication();
		for (Map.Entry<Integer, SocketCommunication> entry : communications
				.entrySet()) {
			try {
				stringBuilder.append("User ID = "
						+ entry.getKey()
						+ ", ip = "
						+ entry.getValue().getConnectedAddress()
								.getHostAddress() + "\n");
			} catch (Exception e) {
				// Because Server local communication has no ip. ignore.
			}

		}
		return stringBuilder.toString();
	}

	private String getIpStatus() {
		return "IP: " + NetWorkUtil.getLocalIpAddress();
	}

	private String getStatus() {
		StringBuilder builder = new StringBuilder();
		builder.append(getIpStatus() + "\n");
		builder.append(getUserStatus() + "\n");
		builder.append(getCommunicationStatus() + "\n");
		return builder.toString();
	}
}
