package uz.alphazet.domain.rv

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class BaseVH(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun bind(position: Int)
}

fun BaseVH.getString(resId: Int, vararg formatArgs: Any) =
    itemView.context.getString(resId, formatArgs)