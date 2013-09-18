package com.dreamlink.communication.ui;


import com.dreamlink.communication.R;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.debug.NetworkStatus;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.notification.NotificationMgr;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.file.RemoteShareActivity;
import com.dreamlink.communication.ui.history.HistoryManager;
import com.dreamlink.communication.ui.service.FileTransferService;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Win8 style main ui
 * 
 * @author yuri
 * @date 2013年9月11日16:50:57
 */
public class MainUIFrame extends Activity implements OnClickListener,
		OnItemClickListener, OnItemLongClickListener {
	private static final String TAG = "MainUIFrame";
	private Context mContext;

	private GridView mGridView;
	private MainUIAdapter mAdapter;
	private NotificationMgr mNotificationMgr = null;

	private SocketCommunicationManager mSocketComMgr;

	private ImageView mTransferView, mSettingView, mHelpView;
	private ImageView mUserIconView;
	private TextView mUserNameView;
	private TextView mNetWorkStatusView;

	private UserManager mUserManager = null;
	private User mLocalUser;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_mainframe);

		UserHelper userHelper = new UserHelper(this);
		mLocalUser = userHelper.loadUser();

		initView();

		mNotificationMgr = new NotificationMgr(MainUIFrame.this);
		mNotificationMgr.showNotificaiton(NotificationMgr.STATUS_UNCONNECTED);

		mSocketComMgr = SocketCommunicationManager.getInstance(mContext);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		if (mSocketComMgr.getCommunications().isEmpty()) {
			mNetWorkStatusView.setText("未连接");
		} else {
			mNetWorkStatusView.setText("已连接");
		}
	}

	public void initView() {
		mUserIconView = (ImageView) findViewById(R.id.iv_usericon);
		mTransferView = (ImageView) findViewById(R.id.iv_filetransfer);
		mSettingView = (ImageView) findViewById(R.id.iv_setting);
		mHelpView = (ImageView) findViewById(R.id.iv_help);
		mUserNameView = (TextView) findViewById(R.id.tv_username);
		mUserNameView.setText(mLocalUser.getUserName());
		mNetWorkStatusView = (TextView) findViewById(R.id.tv_network_status);
		mUserIconView.setOnClickListener(this);
		// for test
		mUserIconView.setOnLongClickListener(new OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				NetworkStatus status = new NetworkStatus(mContext);
				status.show();
				return true;
			}
		});
		// for test
		mTransferView.setOnClickListener(this);
		mSettingView.setOnClickListener(this);
		mHelpView.setOnClickListener(this);

		mGridView = (GridView) findViewById(R.id.gv_main_menu);
		mAdapter = new MainUIAdapter(this, mGridView);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(this);
		mGridView.setOnItemLongClickListener(this);
	}

	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		switch (v.getId()) {
		case R.id.iv_usericon:
			intent.setClass(MainUIFrame.this, UserInfoSetting.class);
			startActivityForResult(intent,
					DreamConstant.REQUEST_FOR_MODIFY_NAME);
			break;
		case R.id.iv_filetransfer:
			intent.setClass(this, MainFragmentActivity.class);
			intent.putExtra("position", 8);
			startActivity(intent);
			break;
		case R.id.iv_setting:
			intent.setClass(this, MainFragmentActivity.class);
			intent.putExtra("position", 9);
			startActivity(intent);
			break;
		case R.id.iv_help:
			intent.setClass(this, MainFragmentActivity.class);
			intent.putExtra("position", 10);
			startActivity(intent);
			break;
		default:
			break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		System.out.println("onItemClick");
		mAdapter.setClickPosition(position);
		mAdapter.notifyDataSetChanged();
		Intent intent = new Intent(MainUIFrame.this, MainFragmentActivity.class);
		intent.putExtra("position", position);
		startActivity(intent);
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		// TODO Auto-generated method stub
		System.out.println("onItemLongClick");
//		mAdapter.setStatus(position, true);
		mAdapter.setClickPosition(position);
		mAdapter.notifyDataSetChanged();
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {// when create server ,set result ok
			if (DreamConstant.REQUEST_FOR_MODIFY_NAME == requestCode) {
				String name = data.getStringExtra("user");
				mUserNameView.setText(name);
			}
		}
	}

	/** options menu */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		menu.add(0, 2, 0, "远程共享");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 2:
			Intent shareIntent = new Intent(MainUIFrame.this,
					RemoteShareActivity.class);
			// 如果这个activity已经启动了，就不产生新的activity，而只是把这个activity实例加到栈顶来就可以了。
			shareIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(shareIntent);
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		//stop file transfer service
		stopTransferService();
		//modify history db
		modifyHistoryDb();
		// when finish，cloase all connect
		User tem = UserManager.getInstance().getLocalUser();
		tem.setUserID(0);
		mSocketComMgr.closeAllCommunication();
		// Disable wifi AP.
		NetWorkUtil.setWifiAPEnabled(mContext, null, false);
		// Clear wifi connect history.
		NetWorkUtil.clearWifiConnectHistory(mContext);
		// Stop record log and close log file.
		Log.stopAndSave();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showExitDialog();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void showExitDialog() {
		new AlertDialog.Builder(this)
				.setTitle(R.string.exit_app)
				.setMessage(R.string.confirm_exit)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								mNotificationMgr.cancelNotification();

								MainUIFrame.this.finish();
							}
						})
				.setNeutralButton(R.string.hide,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								moveTaskToBack(true);
								mNotificationMgr
										.updateNotification(NotificationMgr.STATUS_DEFAULT);
							}
						}).setNegativeButton(android.R.string.cancel, null)
				.create().show();
	}
	
	/**
	 * stop file transfer service
	 */
	public void stopTransferService(){
		Intent intent = new Intent(mContext, FileTransferService.class);
		stopService(intent);
	}
	
	/**
	 * when app finish,modfiy all pre(pre_send/pre_receive) status to fail status that file transfer history in history stable
	 */
	public void modifyHistoryDb(){
		try {
			ContentValues values = new ContentValues();
			//更新两次，第一次将pre_send,改为send_fail
			values.put(MetaData.History.STATUS, HistoryManager.STATUS_SEND_FAIL);
			getContentResolver().update(MetaData.History.CONTENT_URI, values, 
					MetaData.History.STATUS + "=" + HistoryManager.STATUS_PRE_SEND, null);
			
			//第二次，将pre_receive,改为receive_fail
			values.put(MetaData.History.STATUS, HistoryManager.STATUS_RECEIVE_FAIL);
			getContentResolver().update(MetaData.History.CONTENT_URI, values, 
					MetaData.History.STATUS + "=" + HistoryManager.STATUS_PRE_RECEIVE, null);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
}
