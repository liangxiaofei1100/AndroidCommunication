package com.dreamlink.communication.ui.file;

import android.support.v4.app.FragmentActivity;
import android.view.Window;

public class FileTransferActivity extends FragmentActivity {
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new FileBrowserFragment()).commit();
	};
}
