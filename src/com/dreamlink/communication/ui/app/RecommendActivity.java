package com.dreamlink.communication.ui.app;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.TextView;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseActivity;

public class RecommendActivity extends BaseActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setBackgroundDrawable(getResources().getDrawable(R.drawable.title_color));
		actionBar.setIcon(R.drawable.title_tuijian);
		actionBar.setDisplayHomeAsUpEnabled(true);
		
		//modify title text color
		int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
		TextView titleView = (TextView) findViewById(titleId);
		titleView.setTextColor(getResources().getColor(R.color.white));
		
		setTitle(R.string.recommend);
		
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
