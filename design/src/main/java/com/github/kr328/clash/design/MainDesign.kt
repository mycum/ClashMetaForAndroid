package com.github.kr328.clash.design

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.databinding.DesignMainBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainDesign(context: Context) : Design<MainDesign.Request>(context) {
    enum class Request {
        ToggleStatus,
        OpenProxy,
        OpenProfiles,
        OpenProviders,
        OpenLogs,
        OpenSettings,
        OpenHelp,
        OpenAbout,
    }

    private val binding = DesignMainBinding
        .inflate(context.layoutInflater, context.root, false)

    override val root: View
        get() = binding.root

    // Оставляем пустыми, чтобы не сломать MainActivity
    suspend fun setProfileName(name: String?) {}
    suspend fun setForwarded(value: Long) {}
    suspend fun setMode(mode: TunnelState.Mode) {}
    suspend fun setHasProviders(has: Boolean) {}

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) {
            binding.clashRunning = running

            // Ручная установка цветов, чтобы обойти баг компилятора KAPT
            val colorId = if (running) R.color.telegood_accent_blue else R.color.telegood_icon_off
            val color = ContextCompat.getColor(context, colorId)
            val csl = ColorStateList.valueOf(color)

            binding.switchView.trackTintList = csl
            binding.locationIndicator.backgroundTintList = csl

            val bgId = if (running) R.drawable.bg_pill_on else R.drawable.bg_pill_off
            binding.mainPillButton.background = ContextCompat.getDrawable(context, bgId)
        }
    }

    suspend fun showAbout(versionName: String) {
        withContext(Dispatchers.Main) {
            val binding = DesignAboutBinding.inflate(context.layoutInflater).apply {
                this.versionName = versionName
            }

            AlertDialog.Builder(context)
                .setView(binding.root)
                .show()
        }
    }

    init {
        binding.self = this
    }

    fun request(request: Request) {
        requests.trySend(request)
    }
}