package com.dreamlink.communication.ui.history;

import java.text.NumberFormat;
import java.util.Comparator;

public class HistoryManager {
		//status
		public static final int STATUS_DEFAULT = 0;
		public static final int STATUS_PRE_SEND = 10;
		public static final int STATUS_SENDING = 11;
		public static final int STATUS_SEND_SUCCESS = 12;
		public static final int STATUS_SEND_FAIL = 13;
		
		public static final int STATUS_PRE_RECEIVE = 21;
		public static final int STATUS_RECEIVING = 22;
		public static final int STATUS_RECEIVE_SUCCESS = 23;
		public static final int STATUS_RECEIVE_FAIL = 24;
		
		public static final NumberFormat nf = NumberFormat.getPercentInstance();
		
		public static final int TYPE_SEND = 0;
		public static final int TYPE_RECEIVE = 1;
		
		public static final String ME = "ME";
		
		/**
	     * Perform alphabetical comparison of application entry objects.
	     */
	    public static final Comparator<HistoryInfo> DATE_COMPARATOR = new Comparator<HistoryInfo>() {
	        @Override
	        public int compare(HistoryInfo object1, HistoryInfo object2) {
	        	long date1 = object1.getDate();
				long date2 = object2.getDate();
				if (date1 > date2) {
					return -1;
				} else if (date1 == date2) {
					return 0;
				} else {
					return 1;
				}
	        }
	    };
}
