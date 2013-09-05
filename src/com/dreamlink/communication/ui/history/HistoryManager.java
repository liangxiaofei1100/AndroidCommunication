package com.dreamlink.communication.ui.history;

import java.text.NumberFormat;
import java.util.Comparator;

public class HistoryManager {
		//status
		public static final int STATUS_SENDING = 0;
		public static final int STATUS_SEND_SUCCESS = 1;
		public static final int STATUS_SEND_FAIL = 2;
		public static final int STATUS_RECEIVING = 3;
		public static final int STATUS_RECEIVE_SUCCESS = 4;
		public static final int STATUS_RECEIVE_FAIL = 5;
		
		public static final NumberFormat nf = NumberFormat.getPercentInstance();
		
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