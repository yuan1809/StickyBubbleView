# StickyBubbleView
仿QQ未读消息拖拽删除粘性效果

## 参考Blog:  
https://blog.csdn.net/coderlile/article/details/70332759  

https://github.com/MonkeyMushroom/DragBubbleView

## Preview
![](https://github.com/yuan1809/StickyBubbleView/blob/master/preview.gif)

##使用
####Gradle
```
implementation 'com.yuan1809.stickybubble:StickyBubbleView:0.0.1'
```

####XML
```
<android.support.constraint.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="68dp"
    android:clipChildren="false"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <com.yuan1809.stickybubble.StickyBubbleView
        android:id="@+id/StickyBubbleView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:padding="6dp"
        android:layout_marginRight="8dp"
        app:sbvPathColor="#FF0000"
        app:sbvBackground="@drawable/bg"
        app:sbvTextSize="16sp"
        app:sbvTextColor="#FFFFFF"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintRight_toRightOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</android.support.constraint.ConstraintLayout>
```

####Kotlin
```
private val animatorFactory:AnimatorFactory by lazy {
         AnimatorFactory()
     }
     
stickyBubbleView = view.findViewById(R.id.StickyBubbleView);
stickyBubbleView.setAnimatorFactory(animatorFactory);
stickyBubbleView.setStickyListener{
    //do something on onDisappear
    stickyBubbleView.visibility = View.INVISIBLE;
}
```



