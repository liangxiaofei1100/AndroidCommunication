package com.dreamlink.communication.fileshare;

import com.dreamlink.communication.R;
import com.dreamlink.communication.lib.util.Notice;

import android.content.Context;
import android.view.ActionMode;
import android.view.ActionMode.Callback;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * @deprecated use RemoteActionModeCallBack instead
 * @author yuri
 */
public class ActionModeCallBack implements Callback{
	private Notice mNotice = null;
	
	public ActionModeCallBack(Context context){
		mNotice = new Notice(context);
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.file_paste:
			//start copy
			mNotice.showToast("You click paste menu:" + LocalFileFragment.mCurrentPath);
			
			break;
			
		case R.id.menu_cancel:
			mode.finish();
			break;
			
		default:
			break;
		}
		return true;
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		MenuInflater menuInflater = mode.getMenuInflater();
        menuInflater.inflate(R.menu.file_menu, menu);
		return true;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		// TODO Auto-generated method stub
		return true;
	}

}
