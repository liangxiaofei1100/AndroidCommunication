package com.dreamlink.communication.debug;

import android.os.Bundle;

public class DebugActivity extends AppListActivity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setAction("com.dreamlink.communication.action.debug");
		super.onCreate(savedInstanceState);
		setTitle("Debug");
		
	}
	
	
}
