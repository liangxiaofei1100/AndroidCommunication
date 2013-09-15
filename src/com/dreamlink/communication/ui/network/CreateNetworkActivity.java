package com.dreamlink.communication.ui.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import com.dreamlink.communication.aidl.User;
import com.dreamlink.communication.R;
import com.dreamlink.communication.UserManager;
import com.dreamlink.communication.data.UserHelper;
import com.dreamlink.communication.lib.util.Notice;
import com.dreamlink.communication.search.SearchProtocol.OnSearchListener;
import com.dreamlink.communication.search.Search;
import com.dreamlink.communication.server.SocketServer;
import com.dreamlink.communication.server.service.ConnectHelper;
import com.dreamlink.communication.server.service.ServerInfo;
import com.dreamlink.communication.ui.ConnectFriendActivity;
import com.dreamlink.communication.ui.MainUIFrame;
import com.dreamlink.communication.ui.ServerAdapter;
import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

/***
 * use this class to search server and connect
 */
@TargetApi(14)
public class CreateNetworkActivity extends Activity implements OnClickListener,
		OnSearchListener {
	private static final String TAG = "CreateNetworkActivity";
	// Title
	private ImageView mTitleIcon;
	private TextView mTitleView;
	private TextView mTitleNum;
	private ImageView mRefreshView;
	private ImageView mHistoryView;

	private Notice mNotice;
	private Context mContext;
	private boolean sever_flag = false;

	private UserManager mUserManager;

	private Timer mTimeoutTimer = null;

	private ConnectHelper connectHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.ui_create_network);
		mContext = this;
		mNotice = new Notice(mContext);
		connectHelper = ConnectHelper.getInstance(getApplicationContext());
		initTitle();
		initViews();

		mUserManager = UserManager.getInstance();
		UserHelper userHelper = new UserHelper(mContext);
		User localUser = userHelper.loadUser();
		mUserManager.setLocalUser(localUser);

		createServer();
	}

	private void initTitle() {
		// Title icon
		mTitleIcon = (ImageView) findViewById(R.id.iv_title_icon);
		mTitleIcon.setImageResource(R.drawable.network_neighborhood_create);
		// Title text
		mTitleView = (TextView) findViewById(R.id.tv_title_name);
		mTitleView.setText(R.string.create_network);
		// Title number
		mTitleNum = (TextView) findViewById(R.id.tv_title_num);
		mTitleNum.setVisibility(View.GONE);
		// Refresh icon
		mRefreshView = (ImageView) findViewById(R.id.iv_refresh);
		mRefreshView.setVisibility(View.GONE);
		// History icon
		mHistoryView = (ImageView) findViewById(R.id.iv_history);
		mHistoryView.setVisibility(View.GONE);
	}

	private void initViews() {

	}

	public void createServer() {
		LayoutInflater inflater = LayoutInflater.from(mContext);
		View view = inflater.inflate(R.layout.ui_create_server, null);
		final RadioButton wifiButton = (RadioButton) view
				.findViewById(R.id.radio_wifi);
		final RadioButton wifiApButton = (RadioButton) view
				.findViewById(R.id.radio_wifi_ap);
		final RadioButton wifiDirectButton = (RadioButton) view
				.findViewById(R.id.radio_wifi_direct);
		if (!mContext.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_WIFI_DIRECT)) {
			wifiDirectButton.setVisibility(View.GONE);
		}
		new AlertDialog.Builder(CreateNetworkActivity.this)
				.setTitle("Please choose the server type")
				.setView(view)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								String msg = "";
								if (wifiButton.isChecked()) {
									msg = "To Create Wifi Server";
									connectHelper.createServer("wifi",
											CreateNetworkActivity.this);
								} else if (wifiApButton.isChecked()) {
									msg = "To Create Wifi AP Server";
									connectHelper.createServer("wifi-ap",
											CreateNetworkActivity.this);
								} else {
									connectHelper.createServer("wifi-direct",
											CreateNetworkActivity.this);
									msg = "To Create Wifi Direct Server";
								}
								Toast.makeText(CreateNetworkActivity.this, msg,
										Toast.LENGTH_SHORT).show();
								Intent intent = new Intent();
								// data
								intent.putExtra("status", MainUIFrame.CREATING);
								setResult(RESULT_OK, intent);
								sever_flag = true;
								if (null != mTimeoutTimer) {
									mTimeoutTimer.cancel();
								}
								finish();

							}
						})
				.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								setResult(RESULT_CANCELED);
							}
						}).create().show();
	}

	@Override
	public void finish() {
		super.finish();
		if (null != mTimeoutTimer) {
			mTimeoutTimer.cancel();
		}
		if (!sever_flag) {
			connectHelper.stopSearch();
		}
	}

	@Override
	public void onSearchSuccess(String serverIP, String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSearchSuccess(ServerInfo serverInfo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSearchStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}
}
