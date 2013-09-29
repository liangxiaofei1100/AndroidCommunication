package com.dreamlink.communication.ui.media;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.history.HistoryActivity;
import com.dreamlink.communication.util.Log;

import android.app.AlertDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;

public class VideoFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener, OnClickListener {
	private static final String TAG = "VideoFragment";
	private GridView mGridView;
	private ProgressBar mLoadingBar;
	
	private VideoCursorAdapter mAdapter;
	private QueryHandler mQueryHandler = null;
	
	private FileInfoManager mFileInfoManager;
	private Context mContext;
	
	//title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	
	private int mAppId = -1;
	
	private static final String[] PROJECTION = new String[] {MediaStore.Video.Media._ID, 
		MediaStore.Video.Media.DURATION, MediaStore.Video.Media.SIZE,
		MediaColumns.DATA, MediaStore.Video.Media.DISPLAY_NAME};
		
	//video contentObserver listener
	class VideoContent extends ContentObserver{
		public VideoContent(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			int count = mAdapter.getCount();
			updateUI(count);
		}
	}
	
	private static final int MSG_UPDATE_UI = 0;
	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				Log.i(TAG, "handleMessage");
				int size = msg.arg1;
				if (isAdded()) {
					mTitleNum.setText(getString(R.string.num_format, size));
				}
				break;
			default:
				break;
			}
		};
	};
	
	/**
	 * Create a new instance of AppFragment, providing "appid" as an
	 * argument.
	 */
	public static VideoFragment newInstance(int appid) {
		VideoFragment f = new VideoFragment();

		Bundle args = new Bundle();
		args.putInt(Extra.APP_ID, appid);
		f.setArguments(args);

		return f;
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAppId = getArguments() != null ? getArguments().getInt(Extra.APP_ID) : 1;
	};
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mContext = getActivity();
		View rootView = inflater.inflate(R.layout.ui_media_video, container, false);
		mGridView = (GridView) rootView.findViewById(R.id.video_gridview);
		mGridView.setOnItemClickListener(this);
		mGridView.setOnItemLongClickListener(this);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.bar_video_loading);
		mAdapter = new VideoCursorAdapter(mContext);
		mGridView.setAdapter(mAdapter);
		
		initTitleVIews(rootView);
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		mFileInfoManager = new FileInfoManager(mContext);
		mQueryHandler = new QueryHandler(getActivity().getContentResolver());
				
		query();
		VideoContent videoContent  = new VideoContent(new Handler());
		getActivity().getContentResolver().registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, videoContent);
	}
	
	public void query() {
		mQueryHandler.startQuery(0, null, DreamConstant.VIDEO_URI,
				PROJECTION, null, null, MediaStore.Video.Media.DEFAULT_SORT_ORDER);
	}
	
	private void initTitleVIews(View view){
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		//title icon
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_video);
		//refresh button
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		//go to history button
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		//title name
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.video);
		//show current page's item num
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText(getResources().getString(R.string.num_format, 0));
		mRefreshView.setOnClickListener(this)	;
		mHistoryView.setOnClickListener(this);
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
		Cursor cursor = mAdapter.getCursor();
		cursor.moveToPosition(position);
		String url = cursor.getString(cursor
				.getColumnIndex(MediaStore.Video.Media.DATA)); // 文件路径
		mFileInfoManager.openFile(url);
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		final Cursor cursor = mAdapter.getCursor();
		cursor.moveToPosition(position);
//		final long videoId = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media._ID));
		final String url = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)); // 文件路径
		final String displayName = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
			
			
		new AlertDialog.Builder(mContext)
		.setTitle(displayName)
		.setItems(R.array.media_menu, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case 0:
						//open
						mFileInfoManager.openFile(url);
						break;
					case 1:
						//send
						FileTransferUtil fileSendUtil = new FileTransferUtil(getActivity());
						fileSendUtil.sendFile(url);
						break;
					case 2:
						//delete
						showDeleteDialog(position, url);
						break;
					case 3:
						//info
						String info = getVideoInfo(cursor);
						DreamUtil.showInfoDialog(mContext, displayName, info);
						break;

					default:
						break;
					}
			}
		}).create().show();
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
		boolean ret = mFileInfoManager.deleteFileInMediaStore(DreamConstant.VIDEO_URI, path);
		if (!ret) {
			mNotice.showToast(R.string.delete_fail);
			Log.e(TAG, path + " delete failed");
		}else {
			int num = mAdapter.getCount();
			updateUI(num);
		}
	}
    
    public String getVideoInfo(Cursor cursor){
    	long duration = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION)); // 时长
		long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media.SIZE)); // 文件大小
		String url = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA)); // 文件路径
    	String result = "";
		result = "类型:" + "视频" + DreamConstant.ENTER
				+ "位置:" + DreamUtil.getParentPath(url) + DreamConstant.ENTER
				+ "大小:" + DreamUtil.getFormatSize(size) + DreamConstant.ENTER
				+ "时长:" + DreamUtil.mediaTimeFormat(duration);
		return result;
    }
    
    public void updateUI(int num){
		Message message = mHandler.obtainMessage();
		message.arg1 = num;
		message.what = MSG_UPDATE_UI;
		message.sendToTarget();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_refresh:
			query();
			break;
			
		case R.id.iv_history:
			Intent intent = new Intent();
			intent.setClass(mContext, HistoryActivity.class);
			startActivity(intent);
			break;

		default:
			break;
		}
	}
	
	@Override
	public void onDestroy() {
		Cursor cursor = mAdapter.getCursor();
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		super.onDestroy();
	}
	
}
