package com.dreamlink.communication.fileshare;

import com.dreamlink.communication.debug.AppListActivity;
import com.dreamlink.communication.lib.util.AppUtil;
import com.dreamlink.communication.ui.file.RemoteShareService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

/**
 * Launch file share activity base on server or client mode.
 * 
 */
public class FileShareLauncher extends Activity {
	public static final String EXTRA_APP_ID = "app_id";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (intent != null) {
			launchActivity(intent.getBooleanExtra(
					AppListActivity.EXTRA_IS_SERVER, false));
		}
	}

	private void launchActivity(boolean isServer) {
		Intent intent = new Intent();
		int appId = AppUtil.getAppID(this);
		intent.putExtra(EXTRA_APP_ID, appId);
		if (isServer) {
//			intent.setClass(this, ServerActivity.class);
			//start service 
			intent.setClass(this, RemoteShareService.class);
			startService(intent);
			showDialog();
		} else {
			intent.setClass(this, FileMainUI.class);
			startActivity(intent);
			finish();
		}
	}
	
	private void showDialog(){
		AlertDialog.Builder dialog = new Builder(this);
		dialog.setTitle("Service");
		dialog.setMessage("File share service is starting...");
		dialog.setPositiveButton("Stop", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent();
				intent.setClass(FileShareLauncher.this, RemoteShareService.class);
				stopService(intent);
				FileShareLauncher.this.finish();
			}
		});
		dialog.setCancelable(false);
		dialog.create().show();
	}
}
