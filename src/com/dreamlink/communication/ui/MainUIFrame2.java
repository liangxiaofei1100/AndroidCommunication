package com.dreamlink.communication.ui;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.dreamlink.communication.R;
import com.dreamlink.communication.notification.NotificationMgr;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.file.FileTransferActivity;
import com.dreamlink.communication.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;

/**
 * Win8 style main ui
 * @author yuri
 * @date 2013年9月11日16:50:57
 */
public class MainUIFrame2 extends Activity implements OnClickListener, OnItemClickListener {
	private static final String TAG = "MainUIFrame2";
	
	private GridView mGridView;
	private MainUIAdapter mAdapter;
	private static final String DB_PATH = "/data"
			+ Environment.getDataDirectory().getAbsolutePath()
			+ "/com.dreamlink.communication" + "/databases";
	private NotificationMgr mNotificationMgr = null;
	
	private ImageView mUpLoadView,mSettingView, mHelpView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_main_new);
		
		initView();
		
		importGameKeyDb();
		
		mNotificationMgr = new NotificationMgr(MainUIFrame2.this);
		mNotificationMgr.showNotificaiton(NotificationMgr.STATUS_UNCONNECTED);
		
		//get sdcards
		MountManager mountManager = new MountManager();
		mountManager.init();
	}
	
	public void initView(){
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
	
	//import game key db
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
			Intent intent = new Intent(MainUIFrame2.this, FileTransferActivity.class);
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
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent intent = new Intent(MainUIFrame2.this, MainFragmentActivity.class);
		intent.putExtra("position", position);
		startActivity(intent);
	}
	
}
