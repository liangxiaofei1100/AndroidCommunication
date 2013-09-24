package com.dreamlink.communication.debug;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.file.RemoteShareServerService;
import com.dreamlink.communication.ui.service.FileTransferService;
import com.dreamlink.communication.util.ServiceUtil;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

public class ServiceStatus extends Activity {
	private Context mContext;
	private TextView mFileTransferServiceStatusTextView;
	private TextView mRemoteShareServiceStatusTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		setContentView(R.layout.debug_service_status);

		initView();
		updateView();
	}

	private void updateView() {
		mFileTransferServiceStatusTextView
				.setText(getFileTransferServiceStatus());
		mRemoteShareServiceStatusTextView.setText(getRemoteShareServiceStatus());
	}

	private void initView() {
		mFileTransferServiceStatusTextView = (TextView) findViewById(R.id.tv_debug_service_file_transfer);
		mRemoteShareServiceStatusTextView = (TextView) findViewById(R.id.tv_debug_service_remote_share);
	}

	private String getFileTransferServiceStatus() {
		return FileTransferService.class.getName()
				+ " status:\n"
				+ "Running: "
				+ ServiceUtil.isServiceRunning(mContext,
						FileTransferService.class.getName()) + ".\n";
	}

	private String getRemoteShareServiceStatus() {
		return RemoteShareServerService.class.getName()
				+ " status:\n"
				+ "Running: "
				+ ServiceUtil.isServiceRunning(mContext,
						RemoteShareServerService.class.getName()) + ".\n";
	}
}
