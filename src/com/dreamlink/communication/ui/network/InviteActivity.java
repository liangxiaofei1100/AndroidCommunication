package com.dreamlink.communication.ui.network;

import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.server.service.ConnectHelper;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class InviteActivity extends ActionBarActivity implements
		OnClickListener {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_invite);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.title_color));
		actionBar.setIcon(R.drawable.title_tiandi);
		actionBar.setDisplayHomeAsUpEnabled(true);

		// modify title text color
		int titleId = Resources.getSystem().getIdentifier("action_bar_title",
				"id", "android");
		TextView titleView = (TextView) findViewById(titleId);
		titleView.setTextColor(getResources().getColor(R.color.white));

		setTitle(R.string.invite_install);

		initView();
	}

	private void initView() {
		View weixinView = findViewById(R.id.ll_invite_weixin);
		View qqView = findViewById(R.id.ll_invite_qq);
		View bluetoothView = findViewById(R.id.ll_invite_bluetooth);
		View weiboSinaView = findViewById(R.id.ll_invite_weibo_sina);
		View weiboTencentView = findViewById(R.id.ll_invite_weibo_tencent);
		View zeroGprsView = findViewById(R.id.ll_invite_zero_gprs);

		weixinView.setOnClickListener(this);
		qqView.setOnClickListener(this);
		bluetoothView.setOnClickListener(this);
		weiboSinaView.setOnClickListener(this);
		weiboTencentView.setOnClickListener(this);
		zeroGprsView.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		switch (v.getId()) {
		case R.id.ll_invite_weixin:
			break;
		case R.id.ll_invite_qq:
			break;
		case R.id.ll_invite_bluetooth:
			intent.setClass(this, InviteBluetoothActivity.class);
			startActivity(intent);
			break;
		case R.id.ll_invite_weibo_sina:
			break;
		case R.id.ll_invite_weibo_tencent:
			break;
		case R.id.ll_invite_zero_gprs:
			zeroGprsInviteCheck();
			break;
		default:
			break;
		}
	}

	private void zeroGprsInviteCheck() {
		final SocketCommunicationManager manager = SocketCommunicationManager
				.getInstance(getApplicationContext());

		if (manager.isConnected() || manager.isServerAndCreated()) {
			showDisconnectDialog();
		} else {
			launchZeroGprsInvite();
		}
	}

	private void showDisconnectDialog() {
		Builder dialog = new AlertDialog.Builder(this);

		dialog.setTitle(R.string.http_share_open_warning_dialog_title)
				.setMessage(R.string.http_share_open_warning_dialog_message)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								disconnectCurrentNetwork();
								launchZeroGprsInvite();
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.create().show();
	}

	private void launchZeroGprsInvite() {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		intent.setClass(InviteActivity.this, HttpShareActivity.class);
		startActivity(intent);
	}

	private void disconnectCurrentNetwork() {
		ConnectHelper connectHelper = ConnectHelper
				.getInstance(getApplicationContext());
		connectHelper.stopSearch();

		SocketCommunicationManager manager = SocketCommunicationManager
				.getInstance(getApplicationContext());
		manager.closeAllCommunication();
		manager.stopServer();
		UserManager.getInstance().resetLocalUserID();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.finish();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
