package com.dreamlink.communication.ui.image;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.AdapterView.OnItemClickListener;

import com.dreamlink.communication.ui.ListContextMenu;
import com.dreamlink.communication.util.Log;

public class CameraFragment extends BaseImageFragment implements OnItemClickListener {
	private static final String TAG = CameraFragment.class.getName();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setImageList(ImageFragmentActivity.mCamearLists);
//		setContextMenu();
	}
	
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		setContextMenu();
	}
	
//	@Override
//	public boolean onContextItemSelected(MenuItem item) {
//		// TODO Auto-generated method stub
//		System.out.println("==================");
//		return super.onContextItemSelected(item);
//	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
}
