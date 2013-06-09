package com.dreamlink.communication.client;

import java.util.ArrayList;

import com.dreamlink.communication.R;
import com.dreamlink.communication.Search;
import com.dreamlink.communication.client.SearchSever.OnSearchListener;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;
import com.dreamlink.communication.util.Notice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class ServerListActivity extends Activity implements OnSearchListener,
		OnItemClickListener {
	private static final String TAG = "ServerListActivity";
	private Context mContext;

	private ArrayAdapter<String> mAdapter;
	private ArrayList<String> mServer = new ArrayList<String>();
	private ListView mListView;

	private Notice mNotice;
	private SearchSever mSearchServer;
	private SearchSeverLanAndroidAP mSeverAPMode;

	private static final int MSG_SEARCH_SUCCESS = 1;
	private static final int MSG_SEARCH_FAIL = 2;

	private Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_SEARCH_SUCCESS:
				mServer.add((String) msg.obj);
				mAdapter.notifyDataSetChanged();
				break;
			case MSG_SEARCH_FAIL:

				break;

			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client_list);
		mContext = this;
		mNotice = new Notice(mContext);

		initView();

		mSearchServer = SearchSever.getInstance(this);
		mSearchServer.setOnSearchListener(this);
		mSearchServer.startSearch();

		mNotice.showToast("Start Search");
	}

	private void initView() {
		mListView = (ListView) findViewById(R.id.list_client);
		mAdapter = new ArrayAdapter<String>(mContext,
				android.R.layout.simple_list_item_1, mServer);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mSearchServer != null) {
			mSearchServer.stopSearch();
		}
		if (mSeverAPMode != null) {
			mSeverAPMode.stopSearch();
		}
	}

	@Override
	public void onSearchSuccess(String serverIP) {
		if (!mServer.contains(serverIP)) {
			Message message = mHandler.obtainMessage(MSG_SEARCH_SUCCESS);
			message.obj = serverIP;
			mHandler.sendMessage(message);
		}
	}

	@Override
	public void onSearchFail() {
		Message message = mHandler.obtainMessage(MSG_SEARCH_FAIL);
		mHandler.sendMessage(message);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View view, int position,
			long arg3) {
		Intent data = new Intent();
		data.putExtra(Search.EXTRA_IP, mServer.get(position));
		setResult(RESULT_OK, data);
		finish();
	}

}
