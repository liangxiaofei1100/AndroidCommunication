package com.dreamlink.communication.ui.image;

import android.os.Bundle;
import android.widget.AdapterView.OnItemClickListener;

import com.dreamlink.communication.ui.ListContextMenu;
import com.dreamlink.communication.util.Log;

public class GalleryFragment extends BaseImageFragment implements OnItemClickListener {
	private static final String TAG = "GalleryFragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate");
		setImageList(ImageFragmentActivity.mGalleryLists);
//		setContextMenu();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
}
