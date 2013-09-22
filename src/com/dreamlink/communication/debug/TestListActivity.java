package com.dreamlink.communication.debug;

import android.os.Bundle;

public class TestListActivity extends AppListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setAction("com.dreamlink.communication.action.testapp");
		super.onCreate(savedInstanceState);
	}
	
	
}
