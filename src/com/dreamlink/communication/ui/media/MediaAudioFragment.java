package com.dreamlink.communication.ui.media;

import java.util.ArrayList;
import java.util.List;

import com.dreamlink.communication.R;
import com.dreamlink.communication.ui.BaseFragment;
import com.dreamlink.communication.ui.DreamConstant;
import com.dreamlink.communication.ui.DreamConstant.Extra;
import com.dreamlink.communication.util.Log;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ProgressBar;

public class MediaAudioFragment extends BaseFragment implements OnItemClickListener {
	private static final String TAG = "MediaAudioFragment";
	private ListView mListView;
	private MediaAudioAdapter mAdapter;
	private List<MediaInfo> mLists = new ArrayList<MediaInfo>();
	private MediaInfoManager mScan;
	private ProgressBar mScanBar;
	
	private Context mContext;
	
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
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		View rootView = inflater.inflate(R.layout.ui_media_audio, container, false);
		mContext = getActivity();
		
		mListView = (ListView) rootView.findViewById(R.id.audio_listview);
		mListView.setEmptyView( rootView.findViewById(R.id.audio_list_empty));
		mScanBar = (ProgressBar) rootView.findViewById(R.id.audio_progressbar);
		
		mScan = new MediaInfoManager(mContext);
		
		GetAudiosTask getAudiosTask = new GetAudiosTask();
		getAudiosTask.execute();
		
		AudioContent audioContent = new AudioContent(new Handler());
		getActivity().getContentResolver().registerContentObserver(mScan.audioUri, true, audioContent);
		Log.d(TAG, "onCreate end");
		return rootView;
	}
	
	public  class GetAudiosTask extends AsyncTask<Void, String, String>{

		@Override
		protected String doInBackground(Void... params) {
			mLists = mScan.getAudioInfo();
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
			mAdapter = new MediaAudioAdapter(mContext, mLists);
			mListView.setAdapter(mAdapter);
			
			Intent intent = new Intent(DreamConstant.MEDIA_AUDIO_ACTION);
			intent.putExtra(Extra.AUDIO_SIZE, mLists.size());
			mContext.sendBroadcast(intent);
		}
		
	}

	private int currentposition = -1;
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		
		if (currentposition == position) {
			currentposition = -1;
		}else {
			currentposition = position;
		}
		
		mAdapter.setPosition(currentposition);
		mAdapter.notifyDataSetChanged();
	} 
}
