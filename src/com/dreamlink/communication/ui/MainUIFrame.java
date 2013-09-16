package com.dreamlink.communication.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.dreamlink.communication.R;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.debug.NetworkStatus;
import com.dreamlink.communication.AllowLoginDialog;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunication;
import com.dreamlink.communication.SocketCommunicationManager;
import com.dreamlink.communication.AllowLoginDialog.AllowLoginCallBack;
import com.dreamlink.communication.CallBacks.ILoginRequestCallBack;
import com.dreamlink.communication.CallBacks.ILoginRespondCallback;
import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.notification.NotificationMgr;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.file.FileTransferActivity;
import com.dreamlink.communication.ui.file.RemoteShareActivity;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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
		OnItemClickListener {
	private static final String TAG = "MainUIFrame2";
	private Context mContext;

	private GridView mGridView;
	private MainUIAdapter mAdapter;
	private static final String DB_PATH = "/data"
			+ Environment.getDataDirectory().getAbsolutePath()
			+ "/com.dreamlink.communication" + "/databases";
	private NotificationMgr mNotificationMgr = null;

	
	private SocketCommunicationManager mSocketComMgr;
	
	private ImageView mTransferView,mSettingView, mHelpView;
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
		setContentView(R.layout.ui_main_new);
		
		UserHelper userHelper = new UserHelper(this);
		mLocalUser = userHelper.loadUser();
		
		initView();

		importGameKeyDb();

		mNotificationMgr = new NotificationMgr(MainUIFrame.this);
		mNotificationMgr.showNotificaiton(NotificationMgr.STATUS_UNCONNECTED);

		// get sdcards
		MountManager mountManager = new MountManager();
		mountManager.init();
		
		mSocketComMgr = SocketCommunicationManager.getInstance(mContext);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		if (mSocketComMgr.getCommunications().isEmpty()) {
			mNetWorkStatusView.setText("未连接");
		}else {
			mNetWorkStatusView.setText("已连接");
		}
	}
	
	public void initView(){
		mUserIconView = (ImageView) findViewById(R.id.iv_usericon);
		mTransferView = (ImageView) findViewById(R.id.iv_filetransfer);
		mSettingView = (ImageView) findViewById(R.id.iv_setting);
		mHelpView = (ImageView) findViewById(R.id.iv_help);
		mUserNameView = (TextView) findViewById(R.id.tv_username);
		mUserNameView.setText(mLocalUser.getUserName());
		mNetWorkStatusView = (TextView) findViewById(R.id.tv_network_status);
		mUserIconView.setOnClickListener(this);
		//for test
		mUserIconView.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
				NetworkStatus status = new NetworkStatus(mContext);
				status.show();
				return true;
			}
		});
		//for test
		mTransferView.setOnClickListener(this);
		mSettingView.setOnClickListener(this);
		mHelpView.setOnClickListener(this);

		mGridView = (GridView) findViewById(R.id.gv_main_menu);
		mAdapter = new MainUIAdapter(this, mGridView);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(this);
	}

	// import game key db
	private void importGameKeyDb() {
		// copy game_app.db to database
		if (!new File(DB_PATH).exists()) {
			if (new File(DB_PATH).mkdirs()) {
			} else {
				Log.e(TAG, "can not create " + DB_PATH);
			}
		}

		String dbstr = DB_PATH + "/" + MetaData.DATABASE_NAME;
		File dbFile = new File(dbstr);
		if (dbFile.exists()) {
			return;
		}

		// import
		InputStream is;
		try {
			is = getResources().openRawResource(R.raw.game_app);
			FileOutputStream fos = new FileOutputStream(dbFile);
			byte[] buffer = new byte[4 * 1024];
			int count = 0;
			while ((count = is.read(buffer)) > 0) {
				fos.write(buffer, 0, count);
			}
			fos.close();// 关闭输出流
			is.close();// 关闭输入流
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.iv_usericon:
			Intent userSetIntent = new Intent();
			userSetIntent.setClass(MainUIFrame.this, UserInfoSetting.class);
			startActivityForResult(userSetIntent, DreamConstant.REQUEST_FOR_MODIFY_NAME);
			break;
		case R.id.iv_filetransfer:
			Intent intent = new Intent(MainUIFrame.this,
					FileTransferActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
			break;
		case R.id.iv_setting:
			break;
		case R.id.iv_help:
			break;
		default:
			break;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Intent intent = new Intent(MainUIFrame.this,
				MainFragmentActivity.class);
		intent.putExtra("position", position);
		startActivity(intent);
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
	
	/**options menu*/
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
			Intent shareIntent = new Intent(MainUIFrame.this, RemoteShareActivity.class);
			//如果这个activity已经启动了，就不产生新的activity，而只是把这个activity实例加到栈顶来就可以了。
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
		// when finish，cloase all connect
		User tem=UserManager.getInstance().getLocalUser();
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
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			showExitDialog();
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
	
}
