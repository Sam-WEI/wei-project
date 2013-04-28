package wei.pathmenu;

import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.Interpolator;

public class AnimationTimerWith2Interpolator {

	private static final int FPS = 50;
	private static final int FRAME_TIME = 1000/FPS;
	private int mDuration;
	private long mBaseTime;
	private boolean mRunning;
	private AnimCallback mCallback;
	private Interpolator mInterpolator1;
	private Interpolator mInterpolator2;
	private Handler mHandler;
	private float mValue = 0f;
	private int number;
	
	private AnimRunnable mTick;
	private Runnable mDelayedTask;
	
	public AnimationTimerWith2Interpolator(int duration, AnimCallback cb) {
		this(duration, null, null, cb);
	}

	public AnimationTimerWith2Interpolator(int duration, Interpolator i, Interpolator i2, AnimCallback cb) {
		this.mDuration = duration;
		setInterpolator(i, i2);
		mCallback = cb;
		mHandler = new Handler();
		mTick = new AnimRunnable();
	}

	public AnimationTimerWith2Interpolator setNumber(int i){
		number = i;
		return this;
	}
	
	public void setInterpolator(Interpolator i, Interpolator i2){
		this.mInterpolator1 = i;
		this.mInterpolator2 = i2;
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
			float val1 = diff / (float) duration;
			float val2 = diff / (float) duration;

			if (mInterpolator1 != null) {
				val1 = mInterpolator1.getInterpolation(val1);
			}
			
			if (mInterpolator2 != null) {
				val2 = mInterpolator2.getInterpolation(val2);
			}
			

//			if (val > 1.0f) {
//				val = 1.0f;
//			}
			
			float old = mValue;
			mValue = val1;
			
			if(diff >= duration){
				val1 = 1.0f;
				val2 = 1.0f;
			}
			
			mCallback.onAnimValueChanged(val1, val2, number);
			
			int frame = (int)(diff / FRAME_TIME);
			
            long next = base + ((frame+1)*FRAME_TIME);
            if (mRunning && diff < duration) {
                mHandler.postAtTime(this, next);
            }
            if (diff >= duration) {
            	mCallback.onAnimFinished(number);
                mRunning = false;
            }
		}
	}
	
	
	
	public interface AnimCallback{
		void onAnimStarted();
		void onAnimFinished(int number);
		void onAnimValueChanged(float newValue1, float newValue2, int number);
	}
}
