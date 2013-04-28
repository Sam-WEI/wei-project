package wei.pathmenu;

import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.Interpolator;

public class AnimationTimer {

	private static final int FPS = 50;
	private static final int FRAME_TIME = 1000/FPS;
	private int mDuration;
	private long mBaseTime;
	private boolean mRunning;
	private AnimCallback mCallback;
	private Interpolator mInterpolator;
	private Handler mHandler;
	private float mValue = 0f;
	private int number;
	
	private AnimRunnable mTick;
	private Runnable mDelayedTask;
	
	public AnimationTimer(int duration, AnimCallback cb) {
		this(duration, null, cb);
	}

	public AnimationTimer(int duration, Interpolator i, AnimCallback cb) {
		this.mDuration = duration;
		setInterpolator(i);
		mCallback = cb;
		mHandler = new Handler();
		mTick = new AnimRunnable();
	}

	public AnimationTimer setNumber(int i){
		number = i;
		return this;
	}
	
	public void setInterpolator(Interpolator i){
		this.mInterpolator = i;
	}
	
	public void setAnimCallback(AnimCallback cb){
		mCallback = cb;
	}
	
	public void start(){
		if(!mRunning){
			mBaseTime = SystemClock.uptimeMillis();
			mCallback.onAnimStarted();
			mRunning = true;
			long next = SystemClock.uptimeMillis() + FRAME_TIME;
			
			mHandler.postAtTime(mTick, next);
		}
	}
	
	public void startDelayed(int delayMillis){
		
		mDelayedTask = new Runnable() {
			@Override
			public void run() {
				start();
			}
		};
		
		mHandler.postDelayed(mDelayedTask, delayMillis);
	}
	
	public void interrupt() {
		mRunning = false;
		if (mDelayedTask != null) {
			mHandler.removeCallbacks(mDelayedTask);
		}
	}
	
	public boolean isRunning(){
		return mRunning;
	}
	
	private class AnimRunnable implements Runnable {

		@Override
		public void run() {
			long base = mBaseTime;
			long now = SystemClock.uptimeMillis();
			long diff = now - base;
			int duration = mDuration;
			float val = diff / (float) duration;

			if (mInterpolator != null) {
				float tmp = val;
				val = mInterpolator.getInterpolation(val);
//				F.d(tmp+"--->"+val);
			}

//			if (val > 1.0f) {
//				val = 1.0f;
//			}

			float old = mValue;
			mValue = val;
			mCallback.onAnimValueChanged(val, old, number);
			
			int frame = (int)(diff / FRAME_TIME);
            long next = base + ((frame+1)*FRAME_TIME);
            if (mRunning && diff < duration) {
                mHandler.postAtTime(this, next);
            }
            if (diff >= duration) {
            	mCallback.onAnimFinished();
                mRunning = false;
            }
		}
	}
	
	
	
	public interface AnimCallback{
		void onAnimStarted();
		void onAnimFinished();
		void onAnimValueChanged(float newValue, float oldValue, int number);
	}
}
