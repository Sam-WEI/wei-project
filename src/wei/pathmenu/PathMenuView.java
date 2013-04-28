package wei.pathmenu;



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class PathMenuView extends View {
	private final String TAG = "PathMenuView";
	
	private Context context;
	
	private final int CHILDREN_COUNT = 5;
	private final int CHILD_ANGLE = 90 / (CHILDREN_COUNT - 1);

	private Matrix[] mChildrenMatrixes;
	
	private int RADIUS = 150;
	
	private AnimationTimerWith2Interpolator[] timers = new AnimationTimerWith2Interpolator[CHILDREN_COUNT];
	private Bitmap bmCenterNl;
//	private Bitmap bmCenterHl;
	
	private Bitmap[] bmSons;

	private final int SCALE_MAX_TIMES = 5;//放大倍数

	private static final int STATE_STOP = 0;
	private static final int STATE_RADIATING = 1;
	private static final int STATE_RADIATED = 2;
	private static final int STATE_SHRINGKING = 3;
	private static final int STATE_HIGHLIGHTINT_CHILD = 4;
	private static final int STATE_CENTER_FADING_OUT = 5;
	
	
	private int state = STATE_STOP;
	
	private static final long FADING_DELAY = 3000;//中间圆圈渐隐等待时间

	private int clickedChildIndex = -1;
	private int oldHighlightedIndex = -2;
	private int oldSelectedChildIndex = -3;
	private int selectedChildIndex = -4;

	private int mWidthPixels;
	private int mHeightPixels;

	private int unit;// child缩放到SCALE_MAX大小后的长度
	private int VIEW_HEIGHT = 0;
	private int VIEW_WIDTH = 0;
	private float density;
	private Paint paint;
	private Paint paintSon;

	private String[] mTags;
	private int[] mTagWidth = new int[CHILDREN_COUNT];

	private int heartX;
	private int heartY;
	
	private int[][] pivotWhenRadiated = new int[CHILDREN_COUNT][2];//存储子按钮发散到最大位置时的中心点
	
	private onPathMenuClickListener listener = new onPathMenuClickListener() {
		
		@Override
		public void onClick(int index) {
			// TODO Auto-generated method stub
			
		}
	};
	
	public PathMenuView(Context context) {
		super(context);
		init();
	}

	public PathMenuView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public PathMenuView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public void setOnPathMenuClickListener(onPathMenuClickListener l){
		listener = l;
	}
	
	private void init() {

		density = getResources().getDisplayMetrics().density;

		mWidthPixels = getResources().getDisplayMetrics().widthPixels;
		mHeightPixels = getResources().getDisplayMetrics().heightPixels;
		
		bmSons = new Bitmap[CHILDREN_COUNT];
		bmSons[0] = BitmapFactory.decodeResource(getResources(), R.drawable.menuitem_fav_small);
		bmSons[1] = BitmapFactory.decodeResource(getResources(), R.drawable.menuitem_share_small);
		bmSons[2] = BitmapFactory.decodeResource(getResources(), R.drawable.menuitem_sleep_small);
		bmSons[3] = BitmapFactory.decodeResource(getResources(), R.drawable.menuitem_setting_small);
		bmSons[4] = BitmapFactory.decodeResource(getResources(), R.drawable.menuitem_image_small);
		
		bmCenterNl = BitmapFactory.decodeResource(getResources(), R.drawable.main_menu_bg);
		
		RADIUS *= density;
		
		VIEW_WIDTH = (int) ((RADIUS + bmCenterNl.getHeight()/2 + bmSons[0].getHeight()*SCALE_MAX_TIMES / 2.0f) * density);
		VIEW_HEIGHT = (int) ((RADIUS + bmCenterNl.getWidth()/2 + bmSons[0].getWidth()*SCALE_MAX_TIMES / 2.0f) * density);;

		heartX = bmCenterNl.getWidth()/2;
		heartY = VIEW_HEIGHT - bmCenterNl.getHeight()/2;
		
		paint = new Paint();
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.parseColor("#555555"));
		paint.setStyle(Style.STROKE);
		paint.setTextSize((int)Math.round(13 * density));
		
		paintSon = new Paint(Paint.ANTI_ALIAS_FLAG);
		

		mChildrenMatrixes = new Matrix[CHILDREN_COUNT];
		
		initChildrenMatrixes();
		setBackgroundColor(Color.WHITE);
		
	}
	
	private void initChildrenMatrixes(){
		for (int i = 0; i < CHILDREN_COUNT; i++) {
			mChildrenMatrixes[i] = new Matrix();
			mChildrenMatrixes[i].setTranslate(
					heartX - bmSons[i].getWidth()/2,
					heartY - bmSons[i].getHeight()/2);
		}
	}
	
	private void resetChildMatrix(int i){
		mChildrenMatrixes[i].reset();
		mChildrenMatrixes[i].setTranslate(
				heartX - bmSons[i].getWidth()/2,
				heartY - bmSons[i].getHeight()/2);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(VIEW_WIDTH, VIEW_HEIGHT);
	}

	private void updateView() {
		switch (state) {
		case STATE_RADIATING:
			initChildrenMatrixes();
			for (int i = 0; i < CHILDREN_COUNT; i++) {
				
				timers[i] = new AnimationTimerWith2Interpolator(250,
						new OvershootInterpolator(1),
						new DecelerateInterpolator(),
						new AnimationTimerWith2Interpolator.AnimCallback() {

							@Override
							public void onAnimValueChanged(float newValue1,
									float newValue2, int number) {
								final float R = RADIUS * newValue1;
								
								int deltaX = (int) Math.round((R * Math.cos(getAngleInRad(number))));
								int deltaY = (int) Math.round((R * Math.sin(getAngleInRad(number))));
								
								int newX = heartX - bmSons[number].getWidth()/2 - deltaX;
								int newY = heartY - bmSons[number].getHeight()/2 - deltaY;
								
								mChildrenMatrixes[number].setTranslate(newX, newY);
//								mChildrenMatrixes[number].postRotate(360*newValue1, newX/2, newY/2);//甩出来
								
								mChildrenMatrixes[number].postRotate(-360 * newValue2, 
										newX + bmSons[number].getWidth()/2, 
										newY + bmSons[number].getHeight()/2);//规则转
								
								invalidate();
								
							}

							@Override
							public void onAnimStarted() {

							}

							@Override
							public void onAnimFinished(int number) {
								if(number == CHILDREN_COUNT - 1){
									state = STATE_RADIATED;
								}
								pivotWhenRadiated[number][0] 
										= (int) (getXInMatrix(mChildrenMatrixes[number]) 
										+ bmSons[number].getWidth()/2);
								
								pivotWhenRadiated[number][1] 
										= (int) (getYInMatrix(mChildrenMatrixes[number]) 
										+ bmSons[number].getHeight()/2);
								
							}
						}).setNumber(i);

				timers[i].startDelayed(i * 50);
			}
			
			break;

		case STATE_SHRINGKING:

			for (int i = 0; i < CHILDREN_COUNT; i++) {

				timers[i] = new AnimationTimerWith2Interpolator(400,
						new AnticipateInterpolator(2),
						new AccelerateInterpolator(),
						new AnimationTimerWith2Interpolator.AnimCallback() {

							@Override
							public void onAnimValueChanged(float newValue1,
									float newValue2, int number) {
								final float R = RADIUS * (1 - newValue1);
								
								
								int deltaX = (int) Math.round((R * Math.cos(getAngleInRad(number))));
								int deltaY = (int) Math.round((R * Math.sin(getAngleInRad(number))));
								
								int newX = heartX - bmSons[number].getWidth()/2 - deltaX;
								int newY = heartY - bmSons[number].getHeight()/2 - deltaY;
								
								mChildrenMatrixes[number].setTranslate(newX, newY);
								
								mChildrenMatrixes[number].postRotate(360 * newValue2, 
										newX + bmSons[number].getWidth()/2, 
										newY + bmSons[number].getHeight()/2);//规则转
								
//								if(R < 0){
//									resetChildMatrix(number);
//								}
								
								invalidate();
							}

							@Override
							public void onAnimStarted() {

							}

							@Override
							public void onAnimFinished(int number) {
								if(number == CHILDREN_COUNT - 1){
									state = STATE_STOP;
								}
							}
						}).setNumber(i);

				timers[i].startDelayed(i * 30);
			}
			break;
			
		case STATE_HIGHLIGHTINT_CHILD:
			for (int i = 0; i < CHILDREN_COUNT; i++) {
				if(i == clickedChildIndex){//被选中的son不缩小
					continue;
				}
				timers[i] = new AnimationTimerWith2Interpolator(200,
						new AccelerateInterpolator(),
						null,
						new AnimationTimerWith2Interpolator.AnimCallback() {
							@Override
							public void onAnimValueChanged(float newValue1,
									float newValue2, int number) {
								
								mChildrenMatrixes[number].setTranslate(
										pivotWhenRadiated[number][0] - bmSons[number].getWidth()/2,
										pivotWhenRadiated[number][1] - bmSons[number].getHeight()/2);
//								
//								mChildrenMatrixes[number].postRotate(-360 * newValue2, 
//										newX + bmSons[number].getWidth()/2, 
//										newY + bmSons[number].getHeight()/2);//规则转
								mChildrenMatrixes[number].postScale(
										1 - newValue1, 
										1 - newValue1, 
										pivotWhenRadiated[number][0],
										pivotWhenRadiated[number][1]);
								
								if(newValue1 > 0.999){
									resetChildMatrix(number);
								}
								
								invalidate();
							}

							@Override
							public void onAnimStarted() {
							}

							@Override
							public void onAnimFinished(int number) {
									state = STATE_STOP;
							}
						}).setNumber(i);

				timers[i].start();
			}
			
			//被选中的son的动画
			timers[clickedChildIndex] = new AnimationTimerWith2Interpolator(300,
					new AccelerateDecelerateInterpolator(),
					null,
					new AnimationTimerWith2Interpolator.AnimCallback() {
						@Override
						public void onAnimValueChanged(float newValue1,
								float newValue2, int number) {
//							final float R = RADIUS * newValue1;
//							
//							int deltaX = (int) Math.round((R * Math.cos(getAngleInRad(number))));
//							int deltaY = (int) Math.round((R * Math.sin(getAngleInRad(number))));
//							
//							int newX = heartX - bmSons[number].getWidth()/2 - deltaX;
//							int newY = heartY - bmSons[number].getHeight()/2 - deltaY;
//							
//							mChildrenMatrixes[number].setTranslate(newX, newY);
//							
//							mChildrenMatrixes[number].postRotate(-360 * newValue2, 
//									newX + bmSons[number].getWidth()/2, 
//									newY + bmSons[number].getHeight()/2);//规则转
							mChildrenMatrixes[number].setTranslate(
									pivotWhenRadiated[number][0] - bmSons[number].getWidth()/2,
									pivotWhenRadiated[number][1] - bmSons[number].getHeight()/2);
							
							mChildrenMatrixes[number].postScale(
									1 + (SCALE_MAX_TIMES - 1) * newValue1, 
									1 + (SCALE_MAX_TIMES - 1) * newValue1, 
									pivotWhenRadiated[number][0],
									pivotWhenRadiated[number][1]);
							
//							mChildrenMatrixes[number].postRotate(-360 * newValue2, 
//									pivotWhenRadiated[number][0],
//									pivotWhenRadiated[number][1]);
							
							int alpha = (int) (255 - 255 * newValue1);
							if(alpha < 0){
								alpha = 0;
							}
							
							paintSon.setAlpha(alpha);
							
							invalidate();
							
						}

						@Override
						public void onAnimStarted() {
						}

						@Override
						public void onAnimFinished(int number) {
								state = STATE_STOP;
						}
					}).setNumber(clickedChildIndex);

			timers[clickedChildIndex].start();
			
			break;
		}

	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		for (int i = 0; i < CHILDREN_COUNT; i++) {
			canvas.drawBitmap(bmSons[i], mChildrenMatrixes[i], paintSon);
		}
		canvas.drawBitmap(bmCenterNl, 0, VIEW_HEIGHT - bmCenterNl.getHeight(), paint);
		
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			int x1 = (int) event.getX();
			int y1 = (int) event.getY();
			
			if(touchedMainMenu(x1, y1)){
				if(state == STATE_RADIATED){
					state = STATE_SHRINGKING;
					updateView();
					
				}else if(state == STATE_STOP){
					paintSon.setAlpha(255);
					state = STATE_RADIATING;
					updateView();
				}
			}
			
			if(state == STATE_RADIATED){
				int which = touchedWhichSon(x1, y1);
				if(which != -1){
					if(listener != null){
						F.d(TAG, "touched "+which);
						listener.onClick(which);
						clickedChildIndex = which;
						state = STATE_HIGHLIGHTINT_CHILD;
						updateView();
					}
				}else{
					F.d(TAG, "touched -1");
					state = STATE_SHRINGKING;
					updateView();
				}
			}
			
			return true;

		case MotionEvent.ACTION_UP:
			
			return false;

		case MotionEvent.ACTION_MOVE:

			return false;
		}

		return super.onTouchEvent(event);
	}
	
	private boolean touchedMainMenu(int x, int y){
		return x >= 0 && x <= bmCenterNl.getWidth()  
				   && y >= VIEW_HEIGHT - bmCenterNl.getHeight()
				   && y <= VIEW_HEIGHT ;
	}
	
	/**
	 * @return 获得第index个孩子的角度
	 */
	private float getAngle(int index){
		return 90 + index * CHILD_ANGLE;
	}
	
	/**
	 * @return 获得第index个孩子的弧度
	 */
	private float getAngleInRad(int index){
		return (float) Math.toRadians(getAngle(index));
	}
	
	private int touchedWhichSon(int x, int y){
		int width = 0, height = 0;
		float top = 0, left = 0;
		for(int i = 0; i < CHILDREN_COUNT; i++){
			top = getYInMatrix(mChildrenMatrixes[i]);
			left = getXInMatrix(mChildrenMatrixes[i]);
			width = bmSons[i].getWidth();
			height = bmSons[i].getHeight();
			
			if(x >= left && x <= left + width
					&& y >= top && y <= top + height){
				return i;
			}
		}
		return -1;
	}

	private float getXInMatrix(Matrix m){
		float[] values = new float[9];
		m.getValues(values);
		return values[Matrix.MTRANS_X];
	}
	
	private float getYInMatrix(Matrix m){
		float[] values = new float[9];
		m.getValues(values);
		return values[Matrix.MTRANS_Y];
	}
	
	public interface onPathMenuClickListener{
		public void onClick(int index);
	}
}
