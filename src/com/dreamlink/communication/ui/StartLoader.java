package com.dreamlink.communication.ui;

import java.io.File;

import com.dreamlink.communication.R;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class StartLoader extends Activity {
	private static final String TAG = "StartLoader";
	/** The minimum time(ms) of loading page. */
	private static final int MIN_LOADING_TIME = 1500;

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

	private void createFileSaveFolder() {
		File file = new File(DreamConstant.DEFAULT_SAVE_FOLDER);
		if (!file.exists()) {
			file.mkdirs();
		}
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
}
