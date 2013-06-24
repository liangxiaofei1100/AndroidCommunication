package com.dreamlink.communication.client;

import android.content.Context;

import com.dreamlink.communication.util.Log;
import com.dreamlink.communication.util.NetWorkUtil;

/**
 * This class is used for search the server in current WiFi network connection.</br>
 * 
 * Before {@link #startSearch()}, make sure the device is already connected wifi network.</br>
 * 
 * It can find STA server, Android WiFi AP server in current network.</br>
 *
 */
public class SearchSever {
	public interface OnSearchListener {
		/**
		 * Search server success and found a server</br>
		 * 
		 * Be careful:</br>
		 * 
		 * This method is not in activity main thread.</br>
		 * 
		 * @param serverIP The server IP address.
		 */
		void onSearchSuccess(String serverIP);

		/**
		 * Search server stop</br>
		 * 
		 * Be careful:</br>
		 * 
		 * This method is not in activity main thread.</br>
		 * 
		 */
		void onSearchStop();
	}

	private static final String TAG = "SearchSever";

	private OnSearchListener mListener;

	private boolean mStarted = false;

	private static SearchSever mInstance;
	private Context mContext;

	private SearchSeverLan mSearchSeverLan;
	private SearchSeverLanAndroidAP mSearchSeverLanAndroidAP;

	public static SearchSever getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new SearchSever(context);
		}
		return mInstance;
	}

	private SearchSever(Context context) {
		mContext = context;
	}

	public void setOnSearchListener(OnSearchListener listener) {
		mListener = listener;
	}

	public void startSearch() {
		Log.d(TAG, "Start search.");
		if (mStarted) {
			Log.d(TAG, "startSearch() ignore, search is already started.");
			return;
		}
		mStarted = true;

		if (NetWorkUtil.isAndroidAPNetwork(mContext)) {
			// Android AP network.
			Log.d(TAG, "Android AP network.");
			mSearchSeverLanAndroidAP = SearchSeverLanAndroidAP
					.getInstance(mContext);
			mSearchSeverLanAndroidAP.setOnSearchListener(mListener);
			mSearchSeverLanAndroidAP.startSearch();
		} else {
			Log.d(TAG, "not Android AP network.");
		}

		if (!NetWorkUtil.isWifiApEnabled(mContext)) {
			Log.d(TAG, "This is not Android AP");
			mSearchSeverLan = SearchSeverLan.getInstance(mContext);
			mSearchSeverLan.setOnSearchListener(mListener);
			mSearchSeverLan.startSearch();
		} else {
			Log.d(TAG, "This is AP");
			// Android AP is enabled
			// Because Android AP can not send or receive Lan
			// multicast/broadcast,So it does not need to listen multicast.
		}
	}

	public void stopSearch() {
		Log.d(TAG, "Stop search");
		mStarted = false;

		if (mSearchSeverLan != null) {
			mSearchSeverLan.stopSearch();
		}
		if (mSearchSeverLanAndroidAP != null) {
			mSearchSeverLanAndroidAP.stopSearch();
		}
		mInstance = null;
	}
}
