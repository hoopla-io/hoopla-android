package uz.alphazet.domain.rv

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import uz.alphazet.data.rv.BaseItem
import uz.alphazet.data.rv.getBaseItemDiffUtil
import uz.alphazet.domain.utils.inflate

abstract class BasePagingAdapter<T : BaseItem> :
    PagingDataAdapter<T, BaseVH>(getBaseItemDiffUtil()) {
    abstract fun onCreateViewHolder(view: View, viewType: Int): BaseVH
    abstract override fun getItemViewType(position: Int): Int
    override fun onBindViewHolder(holder: BaseVH, position: Int) = holder.bind(position)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseVH =
        onCreateViewHolder(parent.inflate(viewType), viewType).apply {
            itemView.setOnClickListener {
                onItemClickListener?.invoke(
                    getItem(
                        absoluteAdapterPosition
                    )
                )
            }

        }

    private var onItemClickListener: ((T?) -> Unit)? = null
    fun setOnItemClickListener(l: (T?) -> Unit) {
        onItemClickListener = l
    }
}