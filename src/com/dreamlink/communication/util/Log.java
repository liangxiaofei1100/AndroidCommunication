package com.dreamlink.communication.util;

public class Log {
	public static final boolean isDebug = true;
	public static final boolean isWriteToFile = true;
	public static final boolean isSaveLogcat = true;
	private static final String TAG = "dmLink/";
	private static LogFile mLogFile;
	private static LogcatSaver mLogcatSaver;
	static {
		if (isWriteToFile) {
			mLogFile = new LogFile(TimeUtil.getCurrentTime() + ".txt");
			mLogFile.open();
			mLogFile.writeLog("**********Start Writing Log at time "
					+ TimeUtil.getCurrentTime() + "**********\n");
		}

		if (isSaveLogcat) {
			mLogcatSaver = new LogcatSaver(TimeUtil.getCurrentTime()
					+ "_logcat.txt");
			mLogcatSaver.start();
		}
	}

	public static void i(String tag, String message) {
		if (isDebug) {
			android.util.Log.i(TAG + tag, message);
		}
		if (isWriteToFile && mLogFile != null) {
			mLogFile.writeLog(getLogString("I", tag, message));
		}
	}

	public static void d(String tag, String message) {
		if (isDebug) {
			android.util.Log.d(TAG + tag, message);
		}
		if (isWriteToFile && mLogFile != null) {
			mLogFile.writeLog(getLogString("D", tag, message));
		}
	}

	public static void w(String tag, String message) {
		if (isDebug) {
			android.util.Log.w(TAG + tag, message);
		}
		if (isWriteToFile && mLogFile != null) {
			mLogFile.writeLog(getLogString("W", tag, message));
		}
	}

	public static void e(String tag, String message) {
		if (isDebug) {
			android.util.Log.e(TAG + tag, message);
		}
		if (isWriteToFile && mLogFile != null) {
			mLogFile.writeLog(getLogString("E", tag, message));
		}
	}

	public static void stopAndSave() {
		Log.d(TAG, "closeLogFile");
		if (isWriteToFile && mLogFile != null) {
			mLogFile.close();
		}
		if (isSaveLogcat && mLogcatSaver != null) {
			mLogcatSaver.stop();
		}
	}

	private static String getLogString(String level, String tag, String message) {
		return level + " " + TimeUtil.getCurrentTime() + "  " + tag + "  "
				+ message + "\n";
	}
}
