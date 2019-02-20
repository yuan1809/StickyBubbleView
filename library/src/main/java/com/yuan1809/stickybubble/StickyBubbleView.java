package com.yuan1809.stickybubble;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * Created by yuanye on 2019/1/10.
 */
public class StickyBubbleView extends View {

    private WindowManager mWindowManager;
    private ViewGroup originalParent;
    private ViewGroup.LayoutParams originalLayoutParams; // view原始layout
    private int originalWidth;
    private int originalHeight;
    private int[] originalLocation; // view原始的location，在屏幕上的位置，计算View的相对位置时要减去状态栏高度
    private int originalIndexInParent = -1;
    private float mOriginalRadius;//初始半径
    private Drawable originalBackground;

    private Paint mPaint;
    private int pathColor = Color.RED;//拖拽粘性路径颜色

    private Paint textPaint;
    private String mText =  "999";
    private int textSize = 50; //文字大小
    private int textColor = Color.WHITE; //文字颜色

    private PointF mFixedCenterP;//固定圆心坐标
//    private float mFixedCenterX;//固定圆X坐标
//    private float mFixedCenterY;//固定圆Y坐标
    private PointF mDraggedCenterP; //拖拽圆心坐标
//    private float mDraggedCenterX;//拖拽圆中心X坐标
//    private float mDraggedCenterY;//拖拽圆中心Y坐标

    private float mFixedCircleRadius;//固定圆半径，随拖拽圆距离变化而变化
    private float mDraggedCircleRadius;//拖拽圆半径

    /* 两圆圆心的间距 */
    private float distance;
    private float maxDistance = 300; //两圆间最大距离，超过该距离，启动消失动画，清除
    private Path mBezierPath; //拖拽时两圆间连线path

    private volatile boolean isDragging = false; //拖拽中
    private volatile boolean isAttachWindow = false;//添加到Window中，全屏
    private volatile boolean init = false; //初始化原始View信息
//    private volatile boolean isDragDismissed = false; //拖拽后消失，清除数据

    private volatile boolean isStartDismissBubbleAnim = false;//开始绘制拖拽消失动画
    private int dismissBubbleAnimIndex = -1;

    private StickyListener mStickyListener;
    private AnimatorFactory mAnimatorFactory;

    public StickyBubbleView(Context context) {
        this(context, null);
    }

    public StickyBubbleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickyBubbleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StickyBubbleView,defStyleAttr, 0);
        pathColor = a.getColor(R.styleable.StickyBubbleView_sbvPathColor, Color.RED);
        textColor = a.getColor(R.styleable.StickyBubbleView_sbvTextColor, Color.WHITE);
        textSize = a.getDimensionPixelSize(R.styleable.StickyBubbleView_sbvTextSize, 12);
        originalBackground = a.getDrawable(R.styleable.StickyBubbleView_sbvBackground);
//        if(originalBackground == null){
//            originalBackground = getContext().getResources().getDrawable(R.drawable.bg);
//        }
        a.recycle();

        initPaint(context);
        mBezierPath = new Path();
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDraggedCenterP = new PointF();
        mFixedCenterP = new PointF();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = onMeasureR(0, widthMeasureSpec);
        int h = onMeasureR(1, heightMeasureSpec);
        super.setMeasuredDimension(w, h);
        initData(w, h);
    }

    private int onMeasureR(int attr, int measureSpec){
        int newSize = 0;
        int mode = MeasureSpec.getMode(measureSpec);
        int oldSize = MeasureSpec.getSize(measureSpec);
        switch (mode) {
            case MeasureSpec.EXACTLY:
                newSize = oldSize;
                break;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                float value;
                if (attr == 0) {
                    value = textPaint.measureText(mText);
                    // 控件的宽度
                    newSize = (int) (getPaddingLeft() + value + getPaddingRight());
                } else if (attr == 1) {
                     Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
                     value = Math.abs((fontMetrics.descent - fontMetrics.ascent));
                    // 控件的高度
                    newSize = (int) (getPaddingTop() + value + getPaddingBottom());

                }
                break;
        }

        return newSize;
    }

    private void initData(int w, int h) {

        if(init){
            return;
        }else{
            init = true;
        }

        //设置圆心坐标
        mDraggedCenterP.x = w / 2;
        mDraggedCenterP.y = h / 2;
        mFixedCenterP.set(mDraggedCenterP);

        mOriginalRadius = Math.min(w, h) / 2;
        mFixedCircleRadius = mOriginalRadius;
        mDraggedCircleRadius = mOriginalRadius;

        originalParent = (ViewGroup) getParent();

        // 记录view原始layout参数
        originalLayoutParams = getLayoutParams();

        originalLocation = new int[2];
        originalWidth = w;
        originalHeight = h;

    }

    @Override
    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
        if(isAttachWindow){
            drawFixedCircle(canvas);
            drawDraggedCircle(canvas);
            drawDragPath(canvas);
//            drawTextBitmap(canvas);
            drawDismissAnimatorBitmaps(canvas);
        }
        drawBackground(canvas);
    }

    private void initPaint(Context context) {
        mPaint = new Paint();
        mPaint.setColor(pathColor);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(3);

        textPaint = new Paint();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
    }

    private void drawFixedCircle(Canvas canvas){
        //两圆距离为0时，只需要画可拖拽的圆
        if(distance == 0){
            return;
        }
        //两圆距离超过最大距离时，只需要画可拖拽的圆
        if(distance > maxDistance){
            return;
        }
        if(mFixedCenterP == null){
            return;
        }
        canvas.drawCircle(mFixedCenterP.x, mFixedCenterP.y, mFixedCircleRadius, mPaint);

        //test
//        canvas.drawRect(originalLocation[0], originalLocation[1] - Utils.getStatusBarHeight(getContext()),
//                originalLocation[0] + mOriginalRadius * 2, originalLocation[1] - Utils.getStatusBarHeight(getContext()) + mOriginalRadius * 2, mPaint);
    }

    private void drawDraggedCircle(Canvas canvas){
        //已经开始绘制消失动画不绘制拖拽圆
        if(isStartDismissBubbleAnim){
            return;
        }
        if(mDraggedCenterP == null){
            return;
        }
//        Log.e("TAG", "mDraggedCenterX x = " + mDraggedCenterP.x + "  mDraggedCenterY y = " + mDraggedCenterP.y);
        canvas.drawCircle(mDraggedCenterP.x, mDraggedCenterP.y, mDraggedCircleRadius, mPaint);
    }

    private void drawDragPath(Canvas canvas){

        //两圆距离为0时，只需要画可拖拽的圆
        if(distance == 0){
            return;
        }
        //两圆距离超过最大距离时，只需要画可拖拽的圆
        if(distance > maxDistance){
            return;
        }

        if(mDraggedCenterP == null || mFixedCenterP == null){
            return;
        }

        //计算两条二阶贝塞尔曲线的起点和终点
        float sin = (mDraggedCenterP.y - mFixedCenterP.y) / distance;
        float cos = (mDraggedCenterP.x - mFixedCenterP.x) / distance;
        float fixedStartX = mFixedCenterP.x - mFixedCircleRadius * sin;
        float fixedStartY = mFixedCenterP.y + mFixedCircleRadius * cos;
        float fixedEndX = mFixedCenterP.x + mFixedCircleRadius * sin;
        float fixedEndY = mFixedCenterP.y - mFixedCircleRadius * cos;

        float bubbleStartX = mDraggedCenterP.x + mDraggedCircleRadius * sin;
        float bubbleStartY = mDraggedCenterP.y - mDraggedCircleRadius * cos;
        float bubbleEndX = mDraggedCenterP.x - mDraggedCircleRadius * sin;
        float bubbleEndY = mDraggedCenterP.y + mDraggedCircleRadius * cos;

        //计算控制点坐标，为两圆圆心连线的中点
        float mControlX = (mDraggedCenterP.x + mFixedCenterP.x) / 2;
        float mControlY = (mDraggedCenterP.y + mFixedCenterP.y) / 2;


        //画二阶贝赛尔曲线
        mBezierPath.reset();
        mBezierPath.moveTo(fixedStartX, fixedStartY);
        mBezierPath.quadTo(mControlX, mControlY, bubbleEndX, bubbleEndY);
        mBezierPath.lineTo(bubbleStartX, bubbleStartY);
        mBezierPath.quadTo(mControlX, mControlY, fixedEndX, fixedEndY);
        mBezierPath.close();
        canvas.drawPath(mBezierPath, mPaint);
    }

    private void drawDismissAnimatorBitmaps(Canvas canvas){
        if(!isStartDismissBubbleAnim){
            return;
        }
        if(mAnimatorFactory == null){
            return;
        }
        Bitmap[] bitmaps = mAnimatorFactory.getDismissAnimatorBitmaps(getContext());
        if(bitmaps == null){
            return;
        }
        if(dismissBubbleAnimIndex < 0 && dismissBubbleAnimIndex > bitmaps.length){
            return;
        }
        canvas.drawBitmap(bitmaps[dismissBubbleAnimIndex], mDraggedCenterP.x - mOriginalRadius, mDraggedCenterP.y - mOriginalRadius, mPaint);
    }

    private void drawText(Canvas canvas){
        if(TextUtils.isEmpty(mText)){
            return;
        }
        //文字居中绘制
        Paint.FontMetricsInt fm = textPaint.getFontMetricsInt();
        int baseLine = originalHeight / 2 - fm.descent + (fm.descent - fm.ascent) / 2;
        canvas.drawText(mText, originalWidth / 2,
                baseLine, textPaint);
    }

    private void drawBackground(Canvas canvas){
        if(originalBackground != null && !isStartDismissBubbleAnim){
            Log.e("TAG", "mDraggedCenterX x = " + mDraggedCenterP.x + "  mDraggedCenterY y = " + mDraggedCenterP.y);
            //根据拖拽点移动画布
            canvas.translate(mDraggedCenterP.x - originalWidth / 2,
                    mDraggedCenterP.y - originalHeight / 2);
            originalBackground.setBounds(new Rect(0, 0, originalWidth, originalHeight));
            originalBackground.draw(canvas);
            drawText(canvas);
            canvas.translate(0, 0);
        }
    }

    /**
     * 通过继承TextView，可以使用TextView设置背景文字等属性，拖动时将TextView生成Bitmap绘制拖动，
     * 但是无法解决动画结束后闪一下？？
     * 绘制原TextView生成的Bitmap
     * @param canvas
     */
    private void drawTextBitmap(Canvas canvas){
//        if(textBitmap == null || isStartDismissBubbleAnim){
//            return;
//        }
//        canvas.drawBitmap(textBitmap, mDraggedCenterP.x - originalWidth / 2,
//                mDraggedCenterP.y - originalHeight / 2, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
//                isDragging = true;
                attachToWindow();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    mDraggedCenterP.x = event.getRawX();
                    mDraggedCenterP.y = event.getRawY() - Utils.getStatusBarHeight(getContext());
//                    mDraggedCenterP.x = event.getX();
//                    mDraggedCenterP.y = event.getY();
                    //计算气泡圆心与黏连小球圆心的间距
                    distance = (float) Math.hypot(mDraggedCenterP.x - mFixedCenterP.x, mDraggedCenterP.y - mFixedCenterP.y);
                    mFixedCircleRadius = Math.max(mOriginalRadius / 3, mOriginalRadius * (maxDistance - distance) / maxDistance);
                    mDraggedCircleRadius = mFixedCircleRadius;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                //如果在移动间距没有超过最大距离，我们认为用户不想取消该气泡
                if (distance < maxDistance) {//那么气泡恢复原来位置并颤动一下
                    restoreBubble();
                } else {//气泡消失
                    dismissBubble();
                }
                break;
        }
        return true;
    }

    /*
     * 恢复原有数据
     */
    private void restoreData(){
        mDraggedCenterP.x = originalWidth / 2;
        mDraggedCenterP.y = originalHeight / 2;
        mDraggedCircleRadius = mFixedCircleRadius = mOriginalRadius;
        mFixedCenterP.set(mDraggedCenterP);
        distance = 0;
        isDragging = false;
    }

    /**
     * 恢复原始状态
     */
    private void restoreBubble(){
        //播放动画前从全屏模式下退出，添加到原父布局，解决动画播放后添加闪一下
        detachFromWindow();
        //计算晃动起始坐标
        PointF startPoint = new PointF(mDraggedCenterP.x - originalLocation[0], mDraggedCenterP.y - originalLocation[1] + Utils.getStatusBarHeight(getContext()));
        PointF endPoint = new PointF(originalWidth / 2, originalHeight / 2); //以中心点为基准点
        if(mAnimatorFactory == null){
            restoreData();
            return;
        }
        ValueAnimator animator =  mAnimatorFactory.createRestoreAnimator(startPoint, endPoint);
        if(animator == null){
            restoreData();
            return;
        }
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                PointF pointF = (PointF) animation.getAnimatedValue();
                mDraggedCenterP.x = pointF.x;
                mDraggedCenterP.y = pointF.y;
                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                //复原了
//                detachFromWindow();
                restoreData();
            }
        });
        animator.start();

    }
    /**
     * 拖拽到气泡消失
     */
    private void dismissBubble(){
        startDismissAnim();
    }
    /**
     * 气泡消失
     */
    private void startDismissAnim(){
        if(mAnimatorFactory == null){
            onDissmiss();
            return;
        }
        ValueAnimator animator =  mAnimatorFactory.createDismissAnimator(getContext());
        if(animator == null){
            onDissmiss();
            return;
        }
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                //拿到当前的值并重绘
                dismissBubbleAnimIndex = (int) animation.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                //动画开始改变状态
                isStartDismissBubbleAnim = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //动画结束后改变状态
                isStartDismissBubbleAnim = false;
                onDissmiss();
            }
        });
        animator.start();
    }

    /**
     * 气泡消失时，恢复初始值，并回调
     */
    private void onDissmiss(){
        detachFromWindow();
        restoreData();
        if(mStickyListener != null){
            mStickyListener.onDisappear();
        }
    }

    private void attachToWindow() {
        if(isDragging){
            return;
        }
        isDragging = true;
        isAttachWindow = true;

        originalIndexInParent = originalParent.indexOfChild(this);
        getLocationOnScreen(originalLocation);


        if (originalParent != null) {
            // 将view从它的父控件中移除
            originalParent.removeView(this);
        }
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        layoutParams.format = PixelFormat.TRANSPARENT;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        layoutParams.gravity = Gravity.START | Gravity.TOP;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.x = 0;
        layoutParams.y = 0;

//        Log.e("TAG", "originalLocation x = " + originalLocation[0] + "  originalLocation y = " + originalLocation[1]);
        //originalLocation 是View 左上角原点坐标，圆中心坐标需要居中

        mDraggedCenterP.x = originalLocation[0] + getWidth() / 2 ;
        mDraggedCenterP.y = originalLocation[1] - Utils.getStatusBarHeight(getContext()) + getHeight() / 2;
        mFixedCenterP.set(mDraggedCenterP);

        //windowManager 根布局不包含statusBar
        if (mWindowManager != null) {
            //通过继承TextView实现，无法解决动画结束后闪一下
//            setBackgroundDrawable(null);
            // 将view加入window
            mWindowManager.addView(this, layoutParams);
        }
    }
    
    private void detachFromWindow(){
        if (mWindowManager != null && isDragging) {
            isAttachWindow = false;
            // 把view从window中移除
            mWindowManager.removeViewImmediate(this); //立即删除，如果用removeView会是异步操作，通过handle发送message之后移除，会造成View添加进原父布局之后，从WindowManager删除而导致View中的mParent=null
            if (originalParent != null) {
                originalParent.addView(this,originalIndexInParent, originalLayoutParams);
//                setBackgroundDrawable(originalBackground);
                // 在高版本的SDK上，没有这段代码，view可能不会刷新
                originalParent.invalidate();
            }
        }
    }

    public void setAnimatorFactory(AnimatorFactory animatorFactory){
        mAnimatorFactory = animatorFactory;
    }

    public void setText(String text){
        this.mText = text;
        invalidate();
    }

    public void setStickyListener(StickyListener listener){
        this.mStickyListener = listener;
    }

    public void removeStickyListener(){
        this.mStickyListener = null;
    }
}
