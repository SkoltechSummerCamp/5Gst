package ru.scoltech.openran.speedtest.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import ru.scoltech.openran.speedtest.activities.DevToolsTab
import ru.scoltech.openran.speedtest.activities.SetupPipelineTab


class ParentFragmentPagerAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DevToolsTab()
            1 -> SetupPipelineTab()
            else -> throw IllegalArgumentException("Illegal argument position=$position")
        }
    }

    override fun getItemCount(): Int {
        return 2
    }
}

