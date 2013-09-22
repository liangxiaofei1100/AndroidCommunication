package com.dreamlink.communication.debug;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Show communication manager status. It is used for debug.
 * 
 */
public class CommunicationManagerStatus extends Activity {
	private Context mContext;
	private SocketCommunicationManager mCommunicationManager;
	private TextView mCommunicationListenerTextView;
	private TextView mCommunicationListenerExternalTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debug_communication_manager_status);
		mContext = this;
		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext);

		initView();
		updateCommunicationMangerStatus();
	}

	private void updateCommunicationMangerStatus() {
		mCommunicationListenerTextView
				.setText(getCommunicationListenerStatus());
		mCommunicationListenerExternalTextView
				.setText(getExternalCommunicationListenerStatus());
	}

	private void initView() {
		mCommunicationListenerTextView = (TextView) findViewById(R.id.tv_debug_cm_communication_listener);
		mCommunicationListenerExternalTextView = (TextView) findViewById(R.id.tv_debug_cm_communication_listener_external);
	}
	
	public CommunicationManagerStatus(){
		super();
	}

	public CommunicationManagerStatus(Context context) {
		mContext = context;
		mCommunicationManager = SocketCommunicationManager
				.getInstance(mContext);
	}

	public void show() {
		AlertDialog.Builder alertConfig = new AlertDialog.Builder(mContext);
		alertConfig.setIcon(mContext.getResources().getDrawable(
				R.drawable.app_logo));
		alertConfig.setTitle("Communication manager status");
		alertConfig.setMessage(getStatus());

		alertConfig.create().show();
	}

	private String getCommunicationListenerStatus() {
		return "Communication Listeners:\n"
				+ mCommunicationManager.getOnCommunicationListenerStatus()
				+ "\n";
	}

	private String getExternalCommunicationListenerStatus() {
		return "External Communication Listeners:\n"
				+ mCommunicationManager
						.getOnCommunicationListenerExternalStatus() + "\n";
	}

	private String getStatus() {
		StringBuilder builder = new StringBuilder();
		builder.append(getCommunicationListenerStatus());
		builder.append(getExternalCommunicationListenerStatus());

		return builder.toString();
	}
}
