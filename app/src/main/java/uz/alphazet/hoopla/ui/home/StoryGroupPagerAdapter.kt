package uz.alphazet.hoopla.ui.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class StoryGroupPagerAdapter(
    activity: FragmentActivity,
    private val storyIds: IntArray
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = storyIds.size

    override fun createFragment(position: Int): Fragment =
        StoryGroupFragment.newInstance(storyIds[position])

    override fun getItemId(position: Int): Long = storyIds[position].toLong()

    override fun containsItem(itemId: Long): Boolean =
        storyIds.any { it.toLong() == itemId }
}