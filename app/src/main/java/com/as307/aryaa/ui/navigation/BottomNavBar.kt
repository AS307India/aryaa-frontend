package com.as307.aryaa.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.as307.aryaa.ui.theme.AryaaColors

enum class BottomNavTab(val label: String, val route: String) {
    HOME("Home", Destination.Home.route),
    CONTACTS("Contacts", Destination.Contacts.route),
    SOS("SOS", Destination.Sos.route),
    PROFILE("Profile", "profile") // reserved — implemented in a later unit
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onTabSelected: (BottomNavTab) -> Unit
) {
    NavigationBar(
        containerColor = AryaaColors.NavyCard,
        tonalElevation = 0.dp
    ) {
        BottomNavTab.values().forEach { tab ->
            val selected = currentRoute == tab.route
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            BottomNavTab.HOME -> Icons.Filled.Home
                            BottomNavTab.CONTACTS -> Icons.Filled.People
                            BottomNavTab.SOS -> Icons.Filled.Warning
                            BottomNavTab.PROFILE -> Icons.Filled.Person
                        },
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp),
                        tint = if (selected) AryaaColors.Saffron else AryaaColors.Slate
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 11.sp,
                        color = if (selected) AryaaColors.Saffron else AryaaColors.Slate
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AryaaColors.Saffron.copy(alpha = 0.15f),
                    selectedIconColor = AryaaColors.Saffron,
                    unselectedIconColor = AryaaColors.Slate,
                    selectedTextColor = AryaaColors.Saffron,
                    unselectedTextColor = AryaaColors.Slate
                )
            )
        }
    }
}
