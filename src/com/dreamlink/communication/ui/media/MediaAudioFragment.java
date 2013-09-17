package com.dreamlink.communication.ui.media;

import java.io.File;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MediaAudioFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener, OnClickListener {
	private static final String TAG = "MediaAudioFragment";
	private ListView mListView;
	private MediaAudioAdapter mAdapter;
	private List<MediaInfo> mAudioLists = new ArrayList<MediaInfo>();
	private MediaInfoManager mScanMgr;
	private ProgressBar mLoadingBar;
	private FileInfoManager mFileInfoManager = null;
	
	private Context mContext;
	
	private GetAudiosTask mAudiosTask = null;
	
	//title views
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;
	
	class AudioContent extends ContentObserver{
		public AudioContent(Handler handler) {
			super(handler);
			// TODO Auto-generated constructor stub
		}
		
		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			//when audio db changed,refresh list
			GetAudiosTask getAudiosTask = new GetAudiosTask();
			getAudiosTask.execute();
		}
	}
	
	private static final int MSG_UPDATE_UI = 0;
	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_UPDATE_UI:
				int size = msg.arg1;
				mTitleNum.setText("(" + size + ")");
				break;
			default:
				break;
			}
		};
	};
	
	private int mAppId = -1;
	
	/**
	 * Create a new instance of MediaAudioFragment, providing "appid" as an
	 * argument.
	 */
	public static MediaAudioFragment newInstance(int appid) {
		MediaAudioFragment f = new MediaAudioFragment();

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
		View rootView = inflater.inflate(R.layout.ui_media_audio, container, false);
		mContext = getActivity();
		
		mListView = (ListView) rootView.findViewById(R.id.audio_listview);
		mLoadingBar = (ProgressBar) rootView.findViewById(R.id.audio_progressbar);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		
		initTitleVIews(rootView);
		
		mFileInfoManager = new FileInfoManager(mContext);
		mScanMgr = new MediaInfoManager(mContext);
		
		if (null != mAudiosTask && mAudiosTask.getStatus() == AsyncTask.Status.RUNNING) {
		}else {
			mAudiosTask = new GetAudiosTask();
			mAudiosTask.execute();
		}
		
		AudioContent audioContent = new AudioContent(new Handler());
		getActivity().getContentResolver().registerContentObserver(mScanMgr.audioUri, true, audioContent);
		Log.d(TAG, "onCreate end");
		return rootView;
	}
	
	private void initTitleVIews(View view){
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		//title icon
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_audio);
		// refresh button
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		// go to history button
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		// title name
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.audio);
		// show current page's item num
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText(getResources().getString(R.string.num_format, 0));
		mRefreshView.setOnClickListener(this);
		mHistoryView.setOnClickListener(this);
	}
	
	public  class GetAudiosTask extends AsyncTask<Void, String, Integer>{

		@Override
		protected Integer doInBackground(Void... params) {
			mAudioLists = mScanMgr.getAudioInfo();
			return mAudioLists.size();
		}
		
		@Override
		protected void onPreExecute() {
			mLoadingBar.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			mLoadingBar.setVisibility(View.GONE);
			if (result <= 0) {
				result = 0;
			}else {
				mAdapter = new MediaAudioAdapter(mContext, mAudioLists);
				mListView.setAdapter(mAdapter);
			}
			updateUI(result);
		}
		
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//open audio
		mFileInfoManager.openFile(mAudioLists.get(position).getUrl());
	} 
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
		final MediaInfo mediaInfo = mAudioLists.get(position);
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
						String info = getAudioInfo(mediaInfo);
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
			mAudioLists.remove(position);
			mAdapter.notifyDataSetChanged();
			
			updateUI(mAudioLists.size());
		}
	}
	
	 public String getAudioInfo(MediaInfo mediaInfo){
	    	String result = "";
			result = "名称:" + mediaInfo.getDisplayName()+ DreamConstant.ENTER
					+ "类型:" + "音频" + DreamConstant.ENTER
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
