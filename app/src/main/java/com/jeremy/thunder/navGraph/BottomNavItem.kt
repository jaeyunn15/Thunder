package com.jeremy.thunder.navGraph

import androidx.annotation.DrawableRes
import com.jeremy.thunder.demo.R

sealed class BottomNavItem(
    val title: Int,
    @DrawableRes val icon: Int,
    val screenRoute: String
) {
    object Home : BottomNavItem(R.string.home, R.drawable.ic_baseline_home_24, "homeRoute")
    object Search : BottomNavItem(R.string.search, R.drawable.ic_heart_filled, "searchRoute")
}