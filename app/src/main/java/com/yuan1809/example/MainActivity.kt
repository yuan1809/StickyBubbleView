package com.yuan1809.example

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.yuan1809.stickybubble.AnimatorFactory
import com.yuan1809.stickybubble.StickyBubbleView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

     private val animatorFactory:AnimatorFactory by lazy {
         AnimatorFactory()
     }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recycle_view.layoutManager = LinearLayoutManager(this)
        recycle_view.adapter = RecyclerAdapter(applicationContext, animatorFactory)
    }

    class RecyclerAdapter(val context:Context, val animatorFactory:AnimatorFactory): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount(): Int {
            return 20
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewHolder = (holder as ViewHolder)
            viewHolder.stickyBubbleView.setStickyListener{
                //do something on onDisappear
                viewHolder.stickyBubbleView.visibility = View.INVISIBLE;
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view = LayoutInflater.from(context).inflate(R.layout.layout_item, parent, false)
            var viewHolder = ViewHolder(view, animatorFactory)
            return  viewHolder
        }

        class ViewHolder(view:View, animatorFactory:AnimatorFactory):RecyclerView.ViewHolder(view){
            var stickyBubbleView:StickyBubbleView;
            init {
                stickyBubbleView = view.findViewById(R.id.StickyBubbleView);
                stickyBubbleView.setAnimatorFactory(animatorFactory);
            }
        }

    }
}
