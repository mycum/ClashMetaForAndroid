package com.github.kr328.clash.service.store

import android.content.Context
import com.github.kr328.clash.common.store.Store
import com.github.kr328.clash.common.store.asStoreProvider
import com.github.kr328.clash.service.PreferenceProvider
import com.github.kr328.clash.service.model.AccessControlMode
import java.util.*

class ServiceStore(context: Context) {
    private val store = Store(
        PreferenceProvider
            .createSharedPreferencesFromContext(context)
            .asStoreProvider()
    )

    var activeProfile: UUID? by store.typedString(
        key = "active_profile",
        from = { if (it.isBlank()) null else UUID.fromString(it) },
        to = { it?.toString() ?: "" }
    )

    var bypassPrivateNetwork: Boolean by store.boolean(
        key = "bypass_private_network",
        defaultValue = true
    )

    var accessControlMode: AccessControlMode by store.enum(
        key = "access_control_mode",
        defaultValue = AccessControlMode.AcceptSelected, // Изменили на AcceptSelected
        values = AccessControlMode.values()
    )

    var accessControlPackages by store.stringSet(
        key = "access_control_packages",
        defaultValue = setOf(
            // Telegram и популярные форки
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.telegram.plus",
            "org.thunderdog.challegram",
            "tw.nekomimi.nekogram",
            "exteragram.network",
            "com.vk.im",

            // WhatsApp
            "com.whatsapp",
            "com.whatsapp.w4b",

            // YouTube: Официальный клиент и основные ReVanced / RVX моды
            "com.google.android.youtube",
            "app.revanced.android.youtube",
            "app.rvx.android.youtube",
            "anddea.youtube",
            "rufus.youtube",
            "com.vanced.android.youtube",

            // YouTube Music: Официальный клиент и моды
            "com.google.android.apps.youtube.music",
            "app.revanced.android.apps.youtube.music",
            "app.rvx.android.apps.youtube.music",
            "anddea.youtube.music",
            "rufus.youtube.music",
            "com.vanced.android.apps.youtube.music",

            // YouTube: Альтернативные Open-Source клиенты
            "org.schabi.newpipe",
            "com.github.libretube",
            "free.rm.skytube.oss",
            "free.rm.skytube.extra",
            "com.kapp.youtube.final",

            // YouTube для Android TV (SmartTube)
            "com.liskovsoft.smarttubetv.release",
            "com.liskovsoft.smarttubetv.beta",

            // Дополнительные сервисы YouTube
            "com.google.android.apps.youtube.kids",
            "com.google.android.apps.youtube.creator",

            // Instagram
            "com.instagram.android",
            "com.instander.android",

            // Twitter (X)
            "com.twitter.android",

            // Discord
            "com.discord"
        )
    )

    var dnsHijacking by store.boolean(
        key = "dns_hijacking",
        defaultValue = true
    )

    var systemProxy by store.boolean(
        key = "system_proxy",
        defaultValue = true
    )

    var allowBypass by store.boolean(
        key = "allow_bypass",
        defaultValue = true
    )

    var allowIpv6 by store.boolean(
        key = "allow_ipv6",
        defaultValue = false
    )

    var tunStackMode by store.string(
        key = "tun_stack_mode",
        defaultValue = "system"
    )

    var dynamicNotification by store.boolean(
        key = "dynamic_notification",
        defaultValue = true
    )
}