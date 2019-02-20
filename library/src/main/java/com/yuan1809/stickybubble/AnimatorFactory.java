package com.yuan1809.stickybubble;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.view.animation.LinearInterpolator;

/**
 * Created by yuanye on 2019/1/14.
 */
public class AnimatorFactory {

    /* 气泡爆炸的图片id数组 */
    private int[] mExplosionDrawables = {R.mipmap.explosion_one, R.mipmap.explosion_two
            , R.mipmap.explosion_three, R.mipmap.explosion_four, R.mipmap.explosion_five};
    /* 气泡爆炸的bitmap数组 */
    private Bitmap[] mExplosionBitmaps;


    public AnimatorFactory() {
    }

    public ValueAnimator createDismissAnimator(Context context){
        //做一个int型属性动画，从0开始，到气泡爆炸图片数组个数结束
        ValueAnimator anim = ValueAnimator.ofInt(0, mExplosionDrawables.length);
        anim.setInterpolator(new LinearInterpolator());
        anim.setDuration(500);
        return anim;
    }

    public synchronized Bitmap[] getDismissAnimatorBitmaps(Context context){
        if(mExplosionBitmaps != null){
            return mExplosionBitmaps;
        }
        mExplosionBitmaps = new Bitmap[mExplosionDrawables.length];
        for (int i = 0; i < mExplosionDrawables.length; i++) {
            //将气泡爆炸的drawable转为bitmap
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), mExplosionDrawables[i]);
            mExplosionBitmaps[i] = bitmap;
        }
        return  mExplosionBitmaps;
    }

    public ValueAnimator createRestoreAnimator(PointF dragCircleCenter, PointF fixedCircleCenter) {
        ValueAnimator valueAnimator = ValueAnimator.ofObject(new PointFEvaluator(), dragCircleCenter, fixedCircleCenter);
        valueAnimator.setDuration(200);
        valueAnimator.setInterpolator(new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                float f = 0.571429f;
                return (float) (Math.pow(2, -4 * input) * Math.sin((input - f / 4) * (2 * Math.PI) / f) + 1);
            }
        });
        return valueAnimator;
    }

    /**
     * PointF动画估值器
     */
    public class PointFEvaluator implements TypeEvaluator<PointF> {

        @Override
        public PointF evaluate(float fraction, PointF startPointF, PointF endPointF) {
            float x = startPointF.x + fraction * (endPointF.x - startPointF.x);
            float y = startPointF.y + fraction * (endPointF.y - startPointF.y);
            return new PointF(x, y);
        }
    }

}
