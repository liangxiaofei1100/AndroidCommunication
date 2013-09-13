package com.dreamlink.communication.ui.media;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.protocol.FileTransferInfo;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.ListContextMenu;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.common.FileSendUtil;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoManager;
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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
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
	private MediaInfoManager mScan;
	private ProgressBar mScanBar;
	private FileInfoManager mFileInfoManager = null;
	
	private Context mContext;
	
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
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		View rootView = inflater.inflate(R.layout.ui_media_audio, container, false);
		mContext = getActivity();
		
		mListView = (ListView) rootView.findViewById(R.id.audio_listview);
		mListView.setEmptyView( rootView.findViewById(R.id.audio_list_empty));
		mScanBar = (ProgressBar) rootView.findViewById(R.id.audio_progressbar);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		
		getTitleVIews(rootView);
		
		mFileInfoManager = new FileInfoManager(mContext);
		
		mScan = new MediaInfoManager(mContext);
		
		GetAudiosTask getAudiosTask = new GetAudiosTask();
		getAudiosTask.execute();
		
		AudioContent audioContent = new AudioContent(new Handler());
		getActivity().getContentResolver().registerContentObserver(mScan.audioUri, true, audioContent);
		Log.d(TAG, "onCreate end");
		return rootView;
	}
	
	private void getTitleVIews(View view){
		RelativeLayout titleLayout = (RelativeLayout) view.findViewById(R.id.layout_title);
		mTitleIcon = (ImageView) titleLayout.findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.title_audio);
		mRefreshView = (ImageView) titleLayout.findViewById(R.id.iv_refresh);
		mHistoryView = (ImageView) titleLayout.findViewById(R.id.iv_history);
		mTitleView = (TextView) titleLayout.findViewById(R.id.tv_title_name);
		mTitleView.setText("音频");
		mTitleNum = (TextView) titleLayout.findViewById(R.id.tv_title_num);
		mTitleNum.setText("(N)");
		mRefreshView.setOnClickListener(this)	;
		mHistoryView.setOnClickListener(this);
	}
	
	public  class GetAudiosTask extends AsyncTask<Void, String, String>{

		@Override
		protected String doInBackground(Void... params) {
			mAudioLists = mScan.getAudioInfo();
			return null;
		}
		
		@Override
		protected void onPreExecute() {
			mScanBar.setVisibility(View.VISIBLE);
			super.onPreExecute();
		}
		
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			mScanBar.setVisibility(View.GONE);
			mAdapter = new MediaAudioAdapter(mContext, mAudioLists);
			mListView.setAdapter(mAdapter);
			
			Intent intent = new Intent(DreamConstant.MEDIA_AUDIO_ACTION);
			intent.putExtra(Extra.AUDIO_SIZE, mAudioLists.size());
			mContext.sendBroadcast(intent);
			
			Message message = mHandler.obtainMessage();
			message.arg1 = mAudioLists.size();
			message.what = MSG_UPDATE_UI;
			message.sendToTarget();
		}
		
	}

	private int currentposition = -1;
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//20130909 do not use expand menu
		//just single click to open file,double click to open menu
//		if (currentposition == position) {
//			currentposition = -1;
//		}else {
//			currentposition = position;
//		}
//		
//		mAdapter.setPosition(currentposition);
//		mAdapter.notifyDataSetChanged();
		
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
						FileTransferInfo fileTransferInfo = new FileTransferInfo(new File(mediaInfo.getUrl()));

						FileSendUtil fileSendUtil = new FileSendUtil(getActivity());
						fileSendUtil.sendFile(fileTransferInfo);
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
		}
		
		Intent intent = new Intent(DreamConstant.MEDIA_AUDIO_ACTION);
		intent.putExtra(Extra.AUDIO_SIZE, mAudioLists.size());
		mContext.sendBroadcast(intent);
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

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}
}
