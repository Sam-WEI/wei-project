package wei.pathmenu;

import android.util.Log;

public class F {

	private final static boolean isDebug = true;
	private static final String ENCODE = "utf-8";

	public static void out(String tag, String content) {
		if (isDebug) {
			Log.d(tag, content);
		}
	}

	public static void d(String tag, String content) {
		out(tag, content);
	}
	
	public static void e(String tag, String content) {
		if (isDebug) {
			Log.e(tag, content);
		}
	} 
}