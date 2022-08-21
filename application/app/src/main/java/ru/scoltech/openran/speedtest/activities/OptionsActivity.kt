package ru.scoltech.openran.speedtest.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ru.scoltech.openran.speedtest.R
import ru.scoltech.openran.speedtest.customViews.HeaderView


class OptionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        val header = findViewById<HeaderView>(R.id.option_header)
        header.hideOptionsButton()

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)

        val pagerAdapter = OptionsPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val tabNames = listOf("Dev Tools", "Setup pipeline", "Network info")
            tab.text = tabNames[position]
        }.attach()
    }

    private class OptionsPagerAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentStateAdapter(fragmentActivity) {

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DevToolsTab()
                1 -> SetupPipelineTab()
                2 -> NetworkInfoTab()
                else -> throw IllegalArgumentException("Illegal argument position=$position")
            }
        }

        override fun getItemCount(): Int {
            return 3
        }
    }
}
