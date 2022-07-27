package ru.scoltech.openran.speedtest.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.EditText
import ru.scoltech.openran.speedtest.backend.IcmpPinger
import android.os.Bundle
import ru.scoltech.openran.speedtest.R
import ru.scoltech.openran.speedtest.customViews.HeaderView
import android.content.SharedPreferences
import android.text.TextWatcher
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ru.scoltech.openran.speedtest.activities.OptionsActivity
import ru.scoltech.openran.speedtest.adapter.ParentFragmentPagerAdapter
import java.lang.Exception
import java.net.Inet4Address
import java.net.NetworkInterface



class OptionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

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
