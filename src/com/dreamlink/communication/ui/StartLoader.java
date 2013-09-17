package com.dreamlink.communication.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.db.MetaData;
import com.dreamlink.communication.ui.service.FileTransferService;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

/**
 * This is the first ui to show logo, initialize application and load resource.
 *
 */
public class StartLoader extends Activity {
	private static final String TAG = "StartLoader";
	/** The minimum time(ms) of loading page. */
	private static final int MIN_LOADING_TIME = 1500;
	
	private static final String DB_PATH = "/data"
			+ Environment.getDataDirectory().getAbsolutePath()
			+ "/com.dreamlink.communication" + "/databases";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_loader);

		LoadAsyncTask loadAsyncTask = new LoadAsyncTask();
		loadAsyncTask.execute();
	}

	/**
	 * Do application initialize and load resources. This does not run in ui
	 * thread.
	 */
	private void load() {
		Log.d(TAG, "Load start");
		createFileSaveFolder();
		initMountManager();
		// Do not use game DB now.
		// importGameKeyDb();
		
		startService();
		Log.d(TAG, "Load end");
	}

	/**
	 * Load is finished
	 */
	private void loadFinished() {
		launchLogin();
		finish();
		overridePendingTransition(R.anim.push_right_in, R.anim.push_left_out);
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

	private void createFileSaveFolder() {
		File file = new File(DreamConstant.DEFAULT_SAVE_FOLDER);
		if (!file.exists()) {
			file.mkdirs();
		}
	}
	
	private void initMountManager(){
		// get sdcards
		MountManager mountManager = new MountManager();
		mountManager.init();
	}

	class LoadAsyncTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			long start = System.currentTimeMillis();
			// Do load.
			load();
			long end = System.currentTimeMillis();
			if (end - start < MIN_LOADING_TIME) {
				try {
					Thread.sleep(MIN_LOADING_TIME - (end - start));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			loadFinished();
		}
	};

	public void launchLogin() {
		Intent intent = new Intent();
		intent.setClass(this, LoginActivity.class);
		startActivity(intent);
	}
	
	public void startService(){
		Intent intent = new Intent();
		intent.setClass(this, FileTransferService.class);
		startService(intent);
	}
}
