package com.dreamlink.communication.ui.app;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;

import com.dreamlink.communication.ui.BaseActivity;

public class RecommendActivity extends BaseActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getSupportFragmentManager().beginTransaction().replace(
				android.R.id.content, new RecommendFragment()).commit();
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
