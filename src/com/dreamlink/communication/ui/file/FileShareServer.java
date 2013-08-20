package com.dreamlink.communication.ui.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dreamlink.aidl.OnCommunicationListenerExternal;
import com.dreamlink.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.SocketCommunicationManager;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class FileShareServer extends Activity implements OnCommunicationListenerExternal {

	private ListView mListView;
	private SimpleAdapter mAdapter;
	private List<HashMap<String, String>> dataList = new ArrayList<HashMap<String,String>>();
	private SocketCommunicationManager scm = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ui_fileshare_server);
		
		mListView = (ListView) findViewById(R.id.listview);
		
		scm = SocketCommunicationManager.getInstance(getApplicationContext());
		int appid = getIntent().getIntExtra("app_id", 0);
		System.out.println("fileshareServer.appid=" + appid);
		scm.registerOnCommunicationListenerExternal(this, appid);
		
		//init
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("name", "Test");
		dataList.add(map);
		
		mAdapter = new SimpleAdapter(getApplicationContext(), dataList, android.R.layout.simple_list_item_1, new String[]{"name"}, new int[]{android.R.id.text1});
		mListView.setAdapter(mAdapter);
	}
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("name", msg.obj.toString());
			dataList.add(map);
			mAdapter.notifyDataSetChanged();
		};
	};
	
	@Override
	public IBinder asBinder() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void onReceiveMessage(byte[] msg, User sendUser) throws RemoteException {
		// TODO Auto-generated method stub
		String str= new String(msg);
		System.out.println("fileshareServer:onReceiveMesasge:" + str);
		Message message = mHandler.obtainMessage();
		message.obj = str;
		message.sendToTarget();
	}
	@Override
	public void onUserConnected(User user) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onUserDisconnected(User user) throws RemoteException {
		// TODO Auto-generated method stub
		
	}
}
