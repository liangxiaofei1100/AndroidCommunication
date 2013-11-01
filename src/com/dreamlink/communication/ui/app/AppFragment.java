package com.dreamlink.communication.ui.app;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.MenuTabManager;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.MainFragmentActivity;
import com.dreamlink.communication.ui.MenuTabManager.onMenuItemClickListener;
import com.dreamlink.communication.ui.app.AppCursorAdapter.ViewHolder;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.common.FileTransferUtil.TransportCallback;
import com.dreamlink.communication.ui.db.AppData;
import com.dreamlink.communication.ui.dialog.MyDialog;
import com.dreamlink.communication.ui.media.ActionMenu;
import com.dreamlink.communication.ui.media.ActionMenu.ActionMenuItem;
import com.dreamlink.communication.util.Log;

/**
 * use this to load app
 */
public class AppFragment extends AppBaseFragment implements OnItemClickListener, OnItemLongClickListener, onMenuItemClickListener {
	private static final String TAG = "AppFragment";
	
	private AppReceiver mAppReceiver;
	private QueryHandler mQueryHandler;
	
	/**
	 * Create a new instance of AppFragment, providing "appid" as an
	 * argument.
	 */
	public static AppFragment newInstance(int appid) {
		AppFragment f = new AppFragment();

		Bundle args = new Bundle();
		args.putInt(Extra.APP_ID, appid);
		f.setArguments(args);

		return f;
	}
	
	private static final int MSG_UPDATE_UI = 0;
	private static final int MSG_UPDATE_LIST= 1;
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int size = msg.arg1;
				count = size;
				if (isAdded()) {
					mFragmentActivity.setTitleNum(MainFragmentActivity.APP, size);
				}
				break;
			case MSG_UPDATE_LIST:
				Intent intent = new Intent(AppManager.ACTION_REFRESH_APP);
				mContext.sendBroadcast(intent);
				break;

			default:
				break;
			}
		};
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID) : 1;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume");
		mFragmentActivity.addObject(MainFragmentActivity.APP, (BaseFragment)this);
		mFragmentActivity.setTitleName(MainFragmentActivity.APP);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.ui_app, container, false);
		
		mGridView = (GridView) rootView.findViewById(R.id.app_normal_gridview);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.app_progressbar);
		
		mMenuBottomView = rootView.findViewById(R.id.menubar_bottom);
		mMenuBottomView.setVisibility(View.GONE);
		mMenuHolder = (LinearLayout) rootView.findViewById(R.id.ll_menutabs_holder);
		
		//register broadcast
		mAppReceiver = new AppReceiver();
		IntentFilter filter = new IntentFilter(AppManager.ACTION_REFRESH_APP);
		getActivity().registerReceiver(mAppReceiver, filter);
		
		mQueryHandler = new QueryHandler(getActivity().getContentResolver());

		mGridView.setOnItemClickListener(this);
		mGridView.setOnItemLongClickListener(this);
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		query();
		super.onActivityCreated(savedInstanceState);
	}
	
	private static final String[] PROJECTION = {
		AppData.App._ID,AppData.App.PKG_NAME,AppData.App.LABEL,
		AppData.App.APP_SIZE,AppData.App.VERSION,AppData.App.DATE,
		AppData.App.TYPE,AppData.App.ICON,AppData.App.PATH
	};
	
	public void query(){
		mLoadingBar.setVisibility(View.VISIBLE);
		//查询类型为应用的所有数据
		String selectionString = AppData.App.TYPE + "=?" ;
    	String args[] = {"" + AppManager.NORMAL_APP};
		mQueryHandler.startQuery(11, null, AppData.App.CONTENT_URI, PROJECTION, selectionString, args, AppData.App.SORT_ORDER_LABEL);
	}
	
	//query db
	private class QueryHandler extends AsyncQueryHandler {

		public QueryHandler(ContentResolver cr) {
			super(cr);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			Log.d(TAG, "onQueryComplete");
			mLoadingBar.setVisibility(View.INVISIBLE);
			Message message = mHandler.obtainMessage();
			if (null != cursor && cursor.getCount() > 0) {
				Log.d(TAG, "onQueryComplete.count=" + cursor.getCount());
				mAdapter = new AppCursorAdapter(mContext);
				mAdapter.changeCursor(cursor);
				mGridView.setAdapter(mAdapter);
				mAdapter.selectAll(false);
				message.arg1 = cursor.getCount();
			} else {
				message.arg1 = 0;
			}

			message.what = MSG_UPDATE_UI;
			message.sendToTarget();
		}

	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return super.getCount();
	}
	

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if (mAdapter.getMode() == DreamConstant.MENU_MODE_EDIT) {
			mAdapter.setSelected(position);
			mAdapter.notifyDataSetChanged();
			
			int selectedCount = mAdapter.getSelectedItemsCount();
			updateActionMenuTitle(selectedCount);
			updateMenuBar();
			mMenuManager.refreshMenus(mActionMenu);
		} else {
			Cursor cursor = mAdapter.getCursor();
			cursor.moveToPosition(position);
			String packagename = cursor.getString(cursor
					.getColumnIndex(AppData.App.PKG_NAME));
			if (DreamConstant.PACKAGE_NAME.equals(packagename)) {
				mNotice.showToast(R.string.app_has_started);
				return;
			}

			Intent intent = pm.getLaunchIntentForPackage(packagename);
			if (null != intent) {
				startActivity(intent);
			} else {
				mNotice.showToast(R.string.cannot_start_app);
				return;
			}
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			final int position, long id) {
		int mode = mAdapter.getMode();
		if (DreamConstant.MENU_MODE_EDIT == mode) {
			doSelectAll();
			return true;
		}else {
			mAdapter.changeMode(DreamConstant.MENU_MODE_EDIT);
			updateActionMenuTitle(1);
		}
		boolean isSelected = mAdapter.isSelected(position);
		mAdapter.setSelected(position, !isSelected);
		mAdapter.notifyDataSetChanged();
		
		mActionMenu = new ActionMenu(mContext);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_SEND, R.drawable.ic_action_send, R.string.menu_send);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_UNINSTALL,R.drawable.ic_aciton_uninstall,R.string.menu_uninstall);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_MOVE_TO_GAME,R.drawable.ic_action_move_game,R.string.menu_move_to_game);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_INFO,R.drawable.ic_action_app_info,R.string.menu_app_info);
		mActionMenu.addItem(ActionMenu.ACTION_MENU_SELECT, R.drawable.ic_aciton_select, R.string.select_all);

		mMenuManager = new MenuTabManager(mContext, mMenuHolder);
		showMenuBar(true);
		mMenuManager.refreshMenus(mActionMenu);
		mMenuManager.setOnMenuItemClickListener(this);
		return true;
	}
	
	//recevier that can update ui
	private class AppReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "get receiver:" + action);
			if (AppManager.ACTION_REFRESH_APP.equals(action)) {
				reQuery();
			}
		}
	}
    
    public void notifyUpdateUI(){
		Message message = mHandler.obtainMessage();
		message.arg1 = mAdapter.getCount();
		message.what = MSG_UPDATE_UI;
		message.sendToTarget();
	}
    
	@Override
	public void onDestroyView() {
		if (mContext != null && mAppReceiver != null) {
			mContext.unregisterReceiver(mAppReceiver);
			mAppReceiver = null;
		}
		super.onDestroyView();
	}

	public void reQuery(){
		mAdapter.getCursor().requery();
		notifyUpdateUI();
	}
	
	public boolean onBackPressed(){
		if (mAdapter.getMode() == DreamConstant.MENU_MODE_EDIT) {
			showMenuBar(false);
			return false;
		}
		return true;
	}

	@Override
	public void onMenuClick(ActionMenuItem item) {
		switch (item.getItemId()) {
		case ActionMenu.ACTION_MENU_SEND:
			ArrayList<String> selectedList = (ArrayList<String>) mAdapter.getSelectItemPathList();
			//send
			FileTransferUtil fileTransferUtil = new FileTransferUtil(getActivity());
			fileTransferUtil.sendFiles(selectedList, new TransportCallback() {
				@Override
				public void onTransportSuccess() {
					int first = mGridView.getFirstVisiblePosition();
					int last = mGridView.getLastVisiblePosition();
					List<Integer> checkedItems = mAdapter.getSelectedItemPos();
					ArrayList<ImageView> icons = new ArrayList<ImageView>();
					for(int id : checkedItems) {
						if (id >= first && id <= last) {
							View view = mGridView.getChildAt(id - first);
							if (view != null) {
								ViewHolder viewHolder = (ViewHolder)view.getTag();
								icons.add(viewHolder.iconView);
							}
						}
					}
//					
					if (icons.size() > 0) {
						ImageView[] imageViews = new ImageView[0];
						showTransportAnimation(icons.toArray(imageViews));
					}
				}
				
				@Override
				public void onTransportFail() {
				}
			});
			showMenuBar(false);
			break;
		case ActionMenu.ACTION_MENU_UNINSTALL:
			mUninstallList = mAdapter.getSelectedPkgList();
			mUninstallDialog = new MyDialog(mContext, mUninstallList.size());
			mUninstallDialog.setTitle(R.string.handling);
			mUninstallDialog.setOnCancelListener(new OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					if (null != mUninstallList) {
						mUninstallList.clear();
						mUninstallList = null;
					}
				}
			});
			mUninstallDialog.show();
			uninstallApp();
			showMenuBar(false);
			break;
		case ActionMenu.ACTION_MENU_MOVE_TO_GAME:
			showMoveDialog();
			break;
		case ActionMenu.ACTION_MENU_INFO:
			String packageName = mAdapter.getSelectedPkgList().get(0);
			mAppManager.showInstalledAppDetails(packageName);
			showMenuBar(false);
			break;
		case ActionMenu.ACTION_MENU_SELECT:
			doSelectAll();
			break;

		default:
			break;
		}
	}
	
	public void showMoveDialog(){
		final List<String> packageList = mAdapter.getSelectedPkgList();
		new MoveAsyncTask(packageList).execute();
		showMenuBar(false);
	}
	
	private class MoveAsyncTask extends AsyncTask<Void, Void, Void>{
		List<String> pkgList = new ArrayList<String>();
		MyDialog dialog;
		
		MoveAsyncTask(List<String> list){
			pkgList = list;
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (null == dialog) {
				dialog = new MyDialog(mContext, pkgList.size());
				dialog.setTitle(R.string.handling);
				dialog.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						cancel(true);
					}
				});
				dialog.show();
			}
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			String label = null;
			for (int i = 0; i < pkgList.size(); i++) {
				label = mAppManager.getAppLabel(pkgList.get(i));
				dialog.setProgress(i + 1, label);
				moveToGame(pkgList.get(i));
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (null != dialog) {
				dialog.cancel();
				dialog = null;
			}
			mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_LIST));
		}
	}
	
	private void moveToGame(String packageName){
		Log.d(TAG, "moveToGame:" + packageName);
		//move to game
		//1，将该记录的type设置为game
		//2，将数据插入到game表中
		//3，通知GameFragment
		//4，重新查询数据库
		ContentResolver contentResolver = getActivity().getContentResolver();
		ContentValues values = null;
		
		values = new ContentValues();
		values.put(AppData.App.TYPE, AppManager.GAME_APP);
		contentResolver.update(AppData.App.CONTENT_URI, values, 
				AppData.App.PKG_NAME + "='" + packageName + "'", null);
		
		//insert to db
		values = new ContentValues();
		values.put(AppData.App.PKG_NAME, packageName);
		contentResolver.insert(AppData.AppGame.CONTENT_URI, values);
	}
	
	/**
	 * do select all items or unselect all items
	 */
	public void doSelectAll(){
		int selectedCount1 = mAdapter.getSelectedItemsCount();
		if (mAdapter.getCount() != selectedCount1) {
			mAdapter.selectAll(true);
		} else {
			mAdapter.selectAll(false);
		}
		updateMenuBar();
		mMenuManager.refreshMenus(mActionMenu);
		mAdapter.notifyDataSetChanged();
	}
	
	/**
	 * set menubar visible or gone
	 * @param show
	 */
	public void showMenuBar(boolean show){
		if (show) {
			mMenuBottomView.setVisibility(View.VISIBLE);
		}else {
			mMenuBottomView.setVisibility(View.GONE);
			updateActionMenuTitle(-1);
			onActionMenuDone();
		}
	}
	
	
	public void onActionMenuDone() {
		mAdapter.changeMode(DreamConstant.MENU_MODE_NORMAL);
		mAdapter.selectAll(false);
		mAdapter.notifyDataSetChanged();
	}
	
	/**
	 * update menu bar item icon and text color,enable or disable
	 */
	public void updateMenuBar(){
		int selectCount = mAdapter.getSelectedItemsCount();
		updateActionMenuTitle(selectCount);
		
		if (mAdapter.getCount() == selectCount) {
			mActionMenu.findItem(ActionMenu.ACTION_MENU_SELECT).setTitle(R.string.unselect_all);
		}else {
			mActionMenu.findItem(ActionMenu.ACTION_MENU_SELECT).setTitle(R.string.select_all);
		}
		
		if (0==selectCount) {
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_SEND).setEnable(false);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_UNINSTALL).setEnable(false);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_MOVE_TO_GAME).setEnable(false);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_INFO).setEnable(false);
		} else if (1 == selectCount) {
			mActionMenu.findItem(ActionMenu.ACTION_MENU_SEND).setEnable(true);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_UNINSTALL).setEnable(true);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_MOVE_TO_GAME).setEnable(true);
			mActionMenu.findItem(ActionMenu.ACTION_MENU_INFO).setEnable(true);
		} else {
			mActionMenu.findItem(ActionMenu.ACTION_MENU_SEND).setEnable(true);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_UNINSTALL).setEnable(true);
        	mActionMenu.findItem(ActionMenu.ACTION_MENU_MOVE_TO_GAME).setEnable(true);
			mActionMenu.findItem(ActionMenu.ACTION_MENU_INFO).setEnable(false);
		}
	}
	
	/**
	 * update main title 
	 * @param selectCount
	 */
	public void updateActionMenuTitle(int selectCount){
		mFragmentActivity.updateTitleSelectNum(selectCount, count);
	}
	
	@Override
	public int getSelectedCount() {
		return mAdapter.getSelectedItemsCount();
	}
	
	@Override
	public int getMenuMode() {
		return mAdapter.getMode();
	}
}
