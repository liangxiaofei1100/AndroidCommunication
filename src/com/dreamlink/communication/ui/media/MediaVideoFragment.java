package com.dreamlink.communication.ui.media;

import java.util.ArrayList;
import java.util.List;

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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

/**
 * @unuse 先观察几天再删除，如果VideoFragment没有什么问题，就可以删除了
 * */
public class MediaVideoFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener, OnClickListener {
	private static final String TAG = "MediaVideoFragment";
	private GridView mGridView;
	private ProgressBar mLoadingBar;
	
	private MediaVideoAdapter mAdapter;
	private List<MediaInfo> mVideoLists = new ArrayList<MediaInfo>();
	
	private GetVideosTask mVideosTask = null;
	
	private MediaInfoManager mScan;
	
	private FileInfoManager mFileInfoManager;
	private Context mContext;
	
	//title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
		
	//video contentObserver listener
	class VideoContent extends ContentObserver{
		public VideoContent(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
//			GetVideosTask getVideosTask = new GetVideosTask();
//			getVideosTask.execute();
		}
	}
	
	private static final int MSG_UPDATE_UI = 0;
	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int size = msg.arg1;
				mTitleNum.setText(getResources().getString(R.string.num_format, size));
				break;
			default:
				break;
			}
		};
	};
	
	private int mAppId = -1;
	
	/**
	 * Create a new instance of AppFragment, providing "appid" as an
	 * argument.
	 */
	public static MediaVideoFragment newInstance(int appid) {
		MediaVideoFragment f = new MediaVideoFragment();

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
		Log.d(TAG, "onCreate begin");
		mContext = getActivity();
		View rootView = inflater.inflate(R.layout.ui_media_video, container, false);
		mGridView = (GridView) rootView.findViewById(R.id.video_gridview);
		mGridView.setOnItemClickListener(this);
		mGridView.setOnItemLongClickListener(this);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.bar_video_loading);
		
		initTitleVIews(rootView);
		
		Log.d(TAG, "onCreate end");
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mScan = new MediaInfoManager(mContext);
		
		if (null != mVideosTask && mVideosTask.getStatus() == AsyncTask.Status.RUNNING) {
		}else {
			mVideosTask = new GetVideosTask();
			mVideosTask.execute();
		}
		
		mFileInfoManager = new FileInfoManager(mContext);
				
		VideoContent videoContent  = new VideoContent(new Handler());
		getActivity().getContentResolver().registerContentObserver(DreamConstant.VIDEO_URI, true, videoContent);
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
	
	public  class GetVideosTask extends AsyncTask<Void, String, Integer>{

		@Override
		protected Integer doInBackground(Void... params) {
			Log.d(TAG, "start get video src:" + System.currentTimeMillis());
			mVideoLists = mScan.getVideoInfo();
			Log.d(TAG, "end get video src:" + System.currentTimeMillis());
			return mVideoLists.size();
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mLoadingBar.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			mLoadingBar.setVisibility(View.GONE);
			if (result <= 0) {
				result= 0;
			}else {
				mAdapter = new MediaVideoAdapter(mContext, mVideoLists);
				mGridView.setAdapter(mAdapter);
			}
			
			updateUI(result);
		}
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		MediaInfo mediaInfo = mVideoLists.get(position);
		mFileInfoManager.openFile(mediaInfo.getUrl());
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		final MediaInfo mediaInfo = mVideoLists.get(position);
		new AlertDialog.Builder(mContext)
		.setTitle(mediaInfo.getDisplayName())
		.setItems(R.array.media_menu, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case 0:
						//open
						mFileInfoManager.openFile(mediaInfo.getUrl());
						break;
					case 1:
						//send
						FileTransferUtil fileSendUtil = new FileTransferUtil(getActivity());
						fileSendUtil.sendFile(mediaInfo.getUrl());
						break;
					case 2:
						//delete
						showDeleteDialog(position, mediaInfo.getUrl());
						break;
					case 3:
						//info
						String info = getVideoInfo(mediaInfo);
						DreamUtil.showInfoDialog(mContext, info);
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
			mVideoLists.remove(position);
			mAdapter.notifyDataSetChanged();
			
			Message message = mHandler.obtainMessage();
			message.arg1 = mVideoLists.size();
			message.what = MSG_UPDATE_UI;
			message.sendToTarget();
		}
	}
    
    public String getVideoInfo(MediaInfo mediaInfo){
    	String result = "";
		result = "名称:" + mediaInfo.getDisplayName()+ DreamConstant.ENTER
				+ "类型:" + "视频" + DreamConstant.ENTER
				+ "位置:" + mediaInfo.getUrl() + DreamConstant.ENTER
				+ "大小:" + mediaInfo.getFormatSize()+ DreamConstant.ENTER
				+ "修改日期:" + mediaInfo.getFormatDate();
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
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.iv_refresh:
			
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
	
}
