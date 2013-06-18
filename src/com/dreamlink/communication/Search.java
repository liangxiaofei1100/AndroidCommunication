package com.dreamlink.communication;

public class Search {
	public static final String MULTICAST_IP = "239.3.3.3";
	/** timeout:10 seconds */
	public static final int TIME_OUT = 10 * 1000;

	public static final int MULTICAST_SEND_PORT = 40006;
	public static final int MULTICAST_RECEIVE_PORT = 40007;
	public static final long MULTICAST_DELAY_TIME = 2000;

	public static final String EXTRA_IP = "ip";

	public static final String ANDROID_AP_ADDRESS = "192.168.43.1";
	public static final String ANDROID_STA_ADDRESS_START = "192.168.43.";
	/** This message is only used when AP is server. */
	public static final String ANDROID_AP_CLIENT_REQUEST = "Are you server?";
	/** This message is only used when AP is server. */
	public static final String ANDROID_AP_SERVER_RESPOND = "Yes, AP is server.";
	/** This message is only used when AP is client. */
	public static final String ANDROID_AP_SERVER_REQUEST = "I am server. IP: ";
	public static final int ANDROID_AP_SEND_PORT = 55556;
	public static final int ANDROID_AP_RECEIVE_PORT = 55557;
	/** Search request send delay time (second) */
	public static final long ANDROID_AP_SEARCH_DELAY = 2000;

	/** Set AP name to specific name for search */
	public static final String WIFI_AP_NAME = "DreamlinkAP";
}
