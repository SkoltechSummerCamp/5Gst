package ru.scoltech.openran.speedtest.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ru.scoltech.openran.speedtest.R
import ru.scoltech.openran.speedtest.adapter.ParentFragmentPagerAdapter
import ru.scoltech.openran.speedtest.customViews.HeaderView


class OptionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        val header = findViewById<HeaderView>(R.id.option_header)
        header.hideOptionsButton()

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        val tabLayout: TabLayout = findViewById(R.id.tab_layout)

        val pagerAdapter = ParentFragmentPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val tabNames = listOf("Dev Tools", "Setup pipeline")
            tab.text = tabNames[position]
        }.attach()
    }
}
