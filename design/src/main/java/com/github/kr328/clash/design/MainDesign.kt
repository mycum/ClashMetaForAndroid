package com.github.kr328.clash.design

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.github.kr328.clash.design.databinding.DesignAboutBinding
import com.github.kr328.clash.design.databinding.DesignMainBinding
import com.github.kr328.clash.design.util.layoutInflater
import com.github.kr328.clash.design.util.root
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainDesign(context: Context) : Design<MainDesign.Request>(context) {
    enum class Request {
        ToggleStatus, OpenProxy, OpenProfiles, OpenProviders, OpenLogs, OpenSettings, OpenHelp, OpenAbout, RequestUrlTest
    }

    private val binding = DesignMainBinding.inflate(context.layoutInflater, context.root, false)

    private var isCurrentlyRunning: Boolean? = null

    override val root: View get() = binding.root

    private val flagRegex = Regex("[\\uD83C\\uDDE6-\\uD83C\\uDDFF]{2}")

    suspend fun setTestingState(isTesting: Boolean) {
        withContext(Dispatchers.Main) {
            if (isTesting) {
                binding.btnHealthCheck.setImageResource(R.drawable.ic_baseline_sync)
                val rotation = AnimationUtils.loadAnimation(context, R.anim.rotate_infinite)
                binding.btnHealthCheck.startAnimation(rotation)
            } else {
                binding.btnHealthCheck.clearAnimation()
                binding.btnHealthCheck.setImageResource(R.drawable.ic_baseline_flash_on)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    suspend fun setProxyName(name: String?, isRunning: Boolean) {
        withContext(Dispatchers.Main) {
            if (!isRunning || name.isNullOrEmpty() || name in listOf("AUTO-VPN", "DIRECT", "REJECT", "GLOBAL")) {
                binding.locationIndicator.text = "\uD83D\uDD35"
                return@withContext
            }

            val match = flagRegex.find(name)
            if (match != null) {
                binding.locationIndicator.text = match.value
                return@withContext
            }

            binding.locationIndicator.text = "\uD83C\uDF0D"
        }
    }

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) {
            if (isCurrentlyRunning == running) return@withContext

            val isFirstRun = isCurrentlyRunning == null
            isCurrentlyRunning = running

            binding.clashRunning = running

            val colorId = if (running) R.color.telegood_accent_blue else R.color.telegood_icon_off
            val color = ContextCompat.getColor(context, colorId)
            binding.switchView.trackTintList = ColorStateList.valueOf(color)

            if (binding.mainPillButton.background !is android.graphics.drawable.TransitionDrawable) {
                binding.mainPillButton.background = ContextCompat.getDrawable(context, R.drawable.bg_pill_transition)
            }
            val transition = binding.mainPillButton.background as android.graphics.drawable.TransitionDrawable
            transition.isCrossFadeEnabled = true

            if (isFirstRun) {
                if (running) transition.startTransition(0) else transition.resetTransition()
                binding.glowBackground.alpha = if (running) 1f else 0f
                binding.glowBackground.scaleX = if (running) 1f else 0.85f
                binding.glowBackground.scaleY = if (running) 1f else 0.85f
            } else {
                if (running) transition.startTransition(400) else transition.reverseTransition(400)

                binding.glowBackground.animate()
                    .alpha(if (running) 1f else 0f)
                    .scaleX(if (running) 1f else 0.85f)
                    .scaleY(if (running) 1f else 0.85f)
                    .setDuration(400)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()
            }
        }
    }

    suspend fun showAbout(versionName: String) {
        withContext(Dispatchers.Main) {
            val binding = DesignAboutBinding.inflate(context.layoutInflater).apply { this.versionName = versionName }
            AlertDialog.Builder(context).setView(binding.root).show()
        }
    }

    init { binding.self = this }

    fun request(request: Request) { requests.trySend(request) }
}