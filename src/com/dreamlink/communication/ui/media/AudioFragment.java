package com.dreamlink.communication.ui.media;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.MenuTabManager;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.MainFragmentActivity;
import com.dreamlink.communication.ui.MenuTabManager.onMenuItemClickListener;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.common.FileTransferUtil.TransportCallback;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.media.AudioCursorAdapter.ViewHolder;
import com.dreamlink.communication.ui.media.MyMenu.MyMenuItem;
import com.dreamlink.communication.util.Log;

public class AudioFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener, onMenuItemClickListener, OnClickListener, OnMenuItemClickListener {
	private static final String TAG = "AudioFragment";
	private ListView mListView;
	private AudioCursorAdapter mAdapter;
	private ProgressBar mLoadingBar;
	private FileInfoManager mFileInfoManager = null;
	private MenuTabManager mMenuManager;
	
	private QueryHandler mQueryHandler = null;
	
	private Context mContext;
	private View mMenuBottomView;
	private LinearLayout mMenuHolder;
	private View mMenuTopView;
	private View mDoneView;
	private Button mSelectBtn;
	private PopupMenu mSelectPopupMenu;
	
	private static final String[] PROJECTION = {
		MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
		MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
		MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION,
		MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DATA,
		MediaStore.Audio.Media.IS_MUSIC, MediaStore.Audio.Media.DATE_MODIFIED
	};
	
	private class AudioContent extends ContentObserver{
		public AudioContent(Handler handler) {
			super(handler);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			//when audio db changed,update num
			updateUI(mAdapter.getCount());
		}
	}
	
	private static final int MSG_UPDATE_UI = 0;
	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int size = msg.arg1;
				count = size;
				if (isAdded()) {
					mFragmentActivity.setTitleNum(MainFragmentActivity.AUDIO, size);
				}
				break;
			default:
				break;
			}
		};
	};
	
	private int mAppId = -1;
	
	/**
	 * Create a new instance of AudioFragment, providing "appid" as an
	 * argument.
	 */
	public static AudioFragment newInstance(int appid) {
		AudioFragment f = new AudioFragment();

		Bundle args = new Bundle();
		args.putInt(Extra.APP_ID, appid);
		f.setArguments(args);

		return f;
	}
	
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
		mFragmentActivity.addObject(MainFragmentActivity.AUDIO, (BaseFragment)this);
		mFragmentActivity.setTitleName(MainFragmentActivity.AUDIO);
	}
	
	private MyMenu myMenu;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.ui_media_audio, container, false);
		mContext = getActivity();
		
		mListView = (ListView) rootView.findViewById(R.id.audio_listview);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.audio_progressbar);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		mAdapter = new AudioCursorAdapter(mContext);
		mListView.setAdapter(mAdapter);
		
		mMenuBottomView = rootView.findViewById(R.id.menubar_bottom);
		mMenuBottomView.setVisibility(View.GONE);
		mMenuHolder = (LinearLayout) rootView.findViewById(R.id.ll_menutabs_holder);
		mMenuTopView = rootView.findViewById(R.id.menubar_top);
		mMenuTopView.setVisibility(View.GONE);
		mDoneView = rootView.findViewById(R.id.ll_menubar_done);
		mDoneView.setOnClickListener(this);
		mSelectBtn = (Button) rootView.findViewById(R.id.btn_select);
		mSelectBtn.setOnClickListener(this);
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mFileInfoManager = new FileInfoManager(mContext);
		
		mQueryHandler = new QueryHandler(getActivity().getContentResolver());
		query();
		AudioContent audioContent = new AudioContent(new Handler());
		getActivity().getContentResolver().registerContentObserver(DreamConstant.AUDIO_URI, true, audioContent);
		
	}
	
	public void query() {
		mQueryHandler.startQuery(0, null, DreamConstant.AUDIO_URI,
				PROJECTION, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
	}
	
	// query db
	private class QueryHandler extends AsyncQueryHandler {

		public QueryHandler(ContentResolver cr) {
			super(cr);
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			Log.d(TAG, "onQueryComplete");
			mLoadingBar.setVisibility(View.INVISIBLE);
			int num = 0;
			if (null != cursor) {
				Log.d(TAG, "onQueryComplete.count=" + cursor.getCount());
				mAdapter.swapCursor(cursor);
				num = cursor.getCount();
			}
			updateUI(num);
		}

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		int mode = mAdapter.getMode();
		Log.d(TAG, "onItemClicl.mode=" + mode);
		if (DreamConstant.MENU_MODE_NORMAL == mode) {
			//open audio
			Cursor cursor = mAdapter.getCursor();
			cursor.moveToPosition(position);
			String url = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DATA)); // 文件路径
			mFileInfoManager.openFile(url);
		}else {
			mAdapter.setSelected(position);
			mAdapter.notifyDataSetChanged();
			
			int selectedCount = mAdapter.getSelectedItemsCount();
			updateActionMenuTitle(selectedCount);
			if (0 == selectedCount) {
				showMenuBar(false);
				onActionMenuDone();
			}
		}
	} 
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, final View view, final int position, long id) {
		int mode = mAdapter.getMode();
		if (DreamConstant.MENU_MODE_EDIT == mode) {
			return true;
		}else {
			mAdapter.changeMode(DreamConstant.MENU_MODE_EDIT);
			updateActionMenuTitle(1);
		}
		boolean isSelected = mAdapter.isSelected(position);
		mAdapter.setSelected(position, !isSelected);
		mAdapter.notifyDataSetChanged();
		
		myMenu = new MyMenu();
		myMenu.addItem(MyMenu.ACTION_MENU_SEND, R.drawable.ic_action_send, "Send");
		myMenu.addItem(MyMenu.ACTION_MENU_DELETE,R.drawable.ic_action_delete_enable,"Delete");
		myMenu.addItem(MyMenu.ACTION_MENU_INFO,R.drawable.ic_action_info,"Info");
		
		mMenuManager = new MenuTabManager(mContext, mMenuHolder);
		showMenuBar(true);
		mMenuManager.refreshMenus(myMenu);
		mMenuManager.setOnMenuItemClickListener(this);
//						//delete
//						showDeleteDialog(position, url);
//						break;
		return true;
	}
	
	/**
     * show confrim dialog
     * @param path file path
     */
    public void showDeleteDialog(final int pos, final String path) {
    	final FileDeleteDialog deleteDialog = new FileDeleteDialog(mContext, R.style.TransferDialog, path);
		deleteDialog.setOnClickListener(new OnDelClickListener() {
			@Override
			public void onClick(View view, String path) {
				switch (view.getId()) {
				case R.id.left_button:
					doDelete(pos, path);
					break;
				default:
					break;
				}
			}
		});
		deleteDialog.show();
    }
    
    private void doDelete(int position, String path) {
		boolean ret = mFileInfoManager.deleteFileInMediaStore(DreamConstant.AUDIO_URI, path);
		if (!ret) {
			mNotice.showToast(R.string.delete_fail);
			Log.e(TAG, path + " delete failed");
		}else {
			int num = mAdapter.getCount();
			updateUI(num);
		}
	}
	
	public String getAudioInfo(Cursor cursor) {
		long size = cursor.getLong(cursor
				.getColumnIndex(MediaStore.Audio.Media.SIZE)); // 文件大小
		String url = cursor.getString(cursor
				.getColumnIndex(MediaStore.Audio.Media.DATA)); // 文件路径
		long date = cursor.getLong(cursor
				.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED));
		String result = "";
		result = "类型:" + "音频" + DreamConstant.ENTER
				+ "位置:" + DreamUtil.getParentPath(url) + DreamConstant.ENTER
				+ "大小:" + DreamUtil.getFormatSize(size) + DreamConstant.ENTER
				+ "修改日期:" + DreamUtil.getFormatDate(date);
		return result;
	}
	 
	public void updateUI(int num) {
		Message message = mHandler.obtainMessage();
		message.arg1 = num;
		message.what = MSG_UPDATE_UI;
		message.sendToTarget();
	}
	
	public void onActionMenuDone() {
		mAdapter.changeMode(DreamConstant.MENU_MODE_NORMAL);
		mAdapter.selectAll(false);
		mAdapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onBackPressed() {
		int mode = mAdapter.getMode();
		Log.d(TAG, "onBackPressed.mode="+ mode);
		if (DreamConstant.MENU_MODE_EDIT == mode) {
			showMenuBar(false);
			onActionMenuDone();
			return false;
		}else {
			return true;
		}
	}

	@Override
	public void onMenuClick(MyMenuItem item) {
		switch (item.getItemId()) {
		case MyMenu.ACTION_MENU_SEND:
			ArrayList<String> selectedList = (ArrayList<String>) mAdapter.getSelectItemList();
			//send
			FileTransferUtil fileTransferUtil = new FileTransferUtil(getActivity());
			fileTransferUtil.sendFiles(selectedList, new TransportCallback() {
				@Override
				public void onTransportSuccess() {
					int first = mListView.getFirstVisiblePosition();
					int last = mListView.getLastVisiblePosition();
					List<Integer> checkedItems = mAdapter.getSelectedItemPos();
					ArrayList<ImageView> icons = new ArrayList<ImageView>();
					for(int id : checkedItems) {
						if (id >= first && id <= last) {
							View view = mListView.getChildAt(id - first);
							if (view != null) {
								ViewHolder viewHolder = (ViewHolder) view.getTag();
								icons.add(viewHolder.iconView);
							}
						}
					}
					
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
			onActionMenuDone();
			break;
		case MyMenu.ACTION_MENU_DELETE:
			//delete
			break;
		case MyMenu.ACTION_MENU_INFO:
			Cursor cursor = mAdapter.getCursor();
			List<Integer> list = mAdapter.getSelectedItemPos();
			if (1 == list.size()) {
				cursor.moveToPosition(list.get(0));
				String info = getAudioInfo(cursor);
				DreamUtil.showInfoDialog(mContext, "属性", info);
			}else {
				mNotice.showToast("shuxing");
			}
			//info
			break;

		default:
			break;
		}
	}
	
	/**
	 * create popup view for select button,select all/unselect all
	 * @param anchorView
	 * @return
	 */
	private PopupMenu createSelectPopupMenu(View anchorView) {
        final PopupMenu popupMenu = new PopupMenu(mContext, anchorView);
        popupMenu.inflate(R.menu.select_popup_menu);
        popupMenu.setOnMenuItemClickListener(this);
        return popupMenu;
    }
	
	private void updateSelectPopupMenu(){
        if (mSelectPopupMenu == null) {
            mSelectPopupMenu = createSelectPopupMenu(mSelectBtn);
            return;
        }
        final Menu menu = mSelectPopupMenu.getMenu();
        int selectedCount = mAdapter.getSelectedItemsCount();
        updateMenuBar(selectedCount);
        if (mAdapter.getCount() == 0) {
            menu.findItem(R.id.menu_select).setEnabled(false);
        } else {
            menu.findItem(R.id.menu_select).setEnabled(true);
            if (mAdapter.getCount() != selectedCount) {
                menu.findItem(R.id.menu_select).setTitle(R.string.select_all);
                mIsSelectAll = true;
            } else {
                menu.findItem(R.id.menu_select).setTitle(R.string.unselect_all);
                mIsSelectAll = false;
            }
        }
	}
	
	/**
	 * set menubar visible or gone
	 * @param show
	 */
	public void showMenuBar(boolean show){
		if (show) {
			mMenuTopView.setVisibility(View.VISIBLE);
			mMenuBottomView.setVisibility(View.VISIBLE);
		}else {
			mMenuTopView.setVisibility(View.GONE);
			mMenuBottomView.setVisibility(View.GONE);
		}
	}
	
	public void updateMenuBar(int selectCount){
		 if (0==selectCount) {
        	 disableMenuBar();
		}
	}
	
	public void disableMenuBar(){
		mMenuManager.refresh(0);
   	 	mMenuManager.refresh(1);
   	 	mMenuManager.refresh(2);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.ll_menubar_done:
			showMenuBar(false);
			onActionMenuDone();
			break;
		case R.id.btn_select:
			if (null == mSelectPopupMenu) {
				mSelectPopupMenu = createSelectPopupMenu(mSelectBtn);
			}
			updateSelectPopupMenu();
			mSelectPopupMenu.show();
			break;

		default:
			break;
		}
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.menu_select:
			mAdapter.selectAll(mIsSelectAll);
			mAdapter.notifyDataSetChanged();
			int selectcout = mAdapter.getSelectedItemsCount();
			updateActionMenuTitle(selectcout);
			updateSelectPopupMenu();
			break;

		default:
			break;
		}
		return false;
	}
	
	public void updateActionMenuTitle(int count){
		mSelectBtn.setText(getResources().getString(R.string.select_msg, count));
	}
}
