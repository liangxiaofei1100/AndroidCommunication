package com.dreamlink.communication.ui.image;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;

import com.dreamlink.communication.R;
import com.dreamlink.communication.util.Log;

public class CameraFragment extends BaseImageFragment implements OnItemClickListener {
	private static final String TAG = CameraFragment.class.getName();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setImageList(ImageFragmentActivity.mCamearLists);
		
//		setEmptyViewText(R.string.camera_content_null);
	}
	
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
//		setContextMenu();
		setEmptyViewText(R.string.camera_content_null);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
}
