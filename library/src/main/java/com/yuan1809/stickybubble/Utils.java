package com.yuan1809.stickybubble;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.v7.widget.AppCompatTextView;
import android.view.View;
import android.widget.TextView;

/**
 * Created by yuanye on 2019/1/11.
 */

public class Utils {

    private static int statusBarHeight = -1;

    /**
     * 获取状态栏高度
     * @param context
     * @return
     */
    public static int getStatusBarHeight(Context context) {
        if (statusBarHeight != -1) {
            return statusBarHeight;
        }
        int resourceId = context.getResources().getIdentifier("notch_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }

        if (statusBarHeight <= 0) {
            int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resId > 0) {
                statusBarHeight = context.getResources().getDimensionPixelSize(resId);
            }
        }

        return statusBarHeight;
    }

    public static Bitmap loadBitmapFromView(View view) {
//        if(view instanceof TextView) {
//            ((TextView)view).setHorizontallyScrolling(false);
//        }
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();  //启用DrawingCache并创建位图
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache()); //创建一个DrawingCache的拷贝，因为DrawingCache得到的位图在禁用后会被回收
        view.setDrawingCacheEnabled(false);  //禁用DrawingCahce否则会影响性能
        //千万别忘最后一步
        view.destroyDrawingCache();
        return bitmap;
    }

}
