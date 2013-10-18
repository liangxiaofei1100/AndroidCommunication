package com.dreamlink.communication.ui.media;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.ui.DreamUtil;
import com.dreamlink.communication.ui.MainFragmentActivity;
import com.dreamlink.communication.ui.common.FileTransferUtil;
import com.dreamlink.communication.ui.common.FileTransferUtil.TransportCallback;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog;
import com.dreamlink.communication.ui.dialog.FileDeleteDialog.OnDelClickListener;
import com.dreamlink.communication.ui.file.FileInfoManager;
import com.dreamlink.communication.ui.media.AudioCursorAdapter.ViewHolder;
import com.dreamlink.communication.util.Log;

public class AudioFragment extends BaseFragment implements OnItemClickListener, OnItemLongClickListener {
	private static final String TAG = "AudioFragment";
	private ListView mListView;
	private AudioCursorAdapter mAdapter;
	private ProgressBar mLoadingBar;
	private FileInfoManager mFileInfoManager = null;
	
	private QueryHandler mQueryHandler = null;
	
	private Context mContext;
	
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
	};
	
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
		//open audio
		Cursor cursor = mAdapter.getCursor();
		cursor.moveToPosition(position);
		String url = cursor.getString(cursor
				.getColumnIndex(MediaStore.Audio.Media.DATA)); // 文件路径
		mFileInfoManager.openFile(url);
	} 
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, final View view, final int position, long id) {
		final Cursor cursor = mAdapter.getCursor();
		cursor.moveToPosition(position);
		final String title = cursor.getString((cursor
				.getColumnIndex(MediaStore.Audio.Media.TITLE))); // 音乐标题
		final String url = cursor.getString(cursor
				.getColumnIndex(MediaStore.Audio.Media.DATA)); // 文件路径
		new AlertDialog.Builder(mContext)
		.setTitle(title)
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
						fileSendUtil.sendFile(url, new TransportCallback() {
							
							@Override
							public void onTransportSuccess() {
								ViewHolder viewHolder = (ViewHolder) view.getTag();
								showTransportAnimation(viewHolder.iconView);
							}
							
							@Override
							public void onTransportFail() {
								
							}
						});
						break;
					case 2:
						//delete
						showDeleteDialog(position, url);
						break;
					case 3:
						//info
						String info = getAudioInfo(cursor);
						DreamUtil.showInfoDialog(mContext, title, info);
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
}
