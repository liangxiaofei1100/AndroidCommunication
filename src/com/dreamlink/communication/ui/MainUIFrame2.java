package com.dreamlink.communication.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.dreamlink.communication.AllowLoginDialog;
import com.dreamlink.communication.MainActivity;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * Win8 style main ui
 * 
 * @author yuri
 * @date 2013年9月11日16:50:57
 */
public class MainUIFrame2 extends Activity implements OnClickListener,
		OnItemClickListener {
	private static final String TAG = "MainUIFrame2";
	private Context mContext;

	private GridView mGridView;
	private MainUIAdapter mAdapter;
	private static final String DB_PATH = "/data"
			+ Environment.getDataDirectory().getAbsolutePath()
			+ "/com.dreamlink.communication" + "/databases";
	private NotificationMgr mNotificationMgr = null;

	private ImageView mUpLoadView, mSettingView, mHelpView;
	
	private SocketCommunicationManager mSocketComMgr;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_main_new);

		initView();

		importGameKeyDb();

		mNotificationMgr = new NotificationMgr(MainUIFrame2.this);
		mNotificationMgr.showNotificaiton(NotificationMgr.STATUS_UNCONNECTED);

		// get sdcards
		MountManager mountManager = new MountManager();
		mountManager.init();
		
		mSocketComMgr = SocketCommunicationManager.getInstance(mContext);
	}

	public void initView() {
		mUpLoadView = (ImageView) findViewById(R.id.iv_filetransfer);
		mSettingView = (ImageView) findViewById(R.id.iv_setting);
		mHelpView = (ImageView) findViewById(R.id.iv_help);
		mUpLoadView.setOnClickListener(this);
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
		case R.id.iv_filetransfer:
			Intent intent = new Intent(MainUIFrame2.this,
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
		Intent intent = new Intent(MainUIFrame2.this,
				MainFragmentActivity.class);
		intent.putExtra("position", position);
		startActivity(intent);
	}
	
	/**options menu*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
//		menu.add(0, 1, 0, "Share");
		menu.add(0,123,0,"旧入口");
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 123:
			Intent intent2 = new Intent(mContext, MainUIFrame.class);
			startActivity(intent2);
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
		mSocketComMgr.closeAllCommunication();
		// Disable wifi AP.
		NetWorkUtil.setWifiAPEnabled(mContext, null, false);
		// Clear wifi connect history.
		NetWorkUtil.clearWifiConnectHistory(mContext);
		// Stop record log and close log file.
		Log.stopAndSave();
	}
}
