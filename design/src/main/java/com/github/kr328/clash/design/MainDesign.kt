package com.github.kr328.clash.design

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

    // Управление состоянием тестирования (блокировка кнопок и анимация молнии)
    suspend fun setTestingState(isTesting: Boolean) {
        withContext(Dispatchers.Main) {
            binding.btnProxy.isEnabled = !isTesting
            binding.btnSettings.isEnabled = !isTesting

            binding.btnProxy.alpha = if (isTesting) 0.5f else 1.0f
            binding.btnSettings.alpha = if (isTesting) 0.5f else 1.0f

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

    // Умный парсер имени сервера
    suspend fun setProxyName(name: String?, isRunning: Boolean) {
        withContext(Dispatchers.Main) {
            // 1. VPN выключен или идет подключение
            if (!isRunning || name.isNullOrEmpty()) {
                binding.locationIndicator.text = "\uD83D\uDD35"
                return@withContext
            }

            // 2. Сервер еще не выбран (ядро отдало системное имя группы)
            if (name == "AUTO-VPN" || name == "DIRECT" || name == "REJECT" || name == "GLOBAL") {
                binding.locationIndicator.text = "\uD83D\uDD35"
                return@withContext
            }

            // 3. Ищем готовый эмодзи флага в имени
            val flagRegex = Regex("[\\uD83C\\uDDE6-\\uD83C\\uDDFF]{2}")
            val match = flagRegex.find(name)
            if (match != null) {
                binding.locationIndicator.text = match.value
                return@withContext
            }

            // 4. Ищем по тексту
            val upper = name.uppercase()
            fun hasWord(word: String) = Regex("\\b$word\\b").containsMatchIn(upper)

            val emoji = when {
                hasWord("NL") || hasWord("NETHERLANDS") -> "🇳🇱"
                hasWord("US") || hasWord("AMERICA") -> "🇺🇸"
                hasWord("UK") || hasWord("GB") || hasWord("ENGLAND") -> "🇬🇧"
                hasWord("DE") || hasWord("GERMANY") -> "🇩🇪"
                hasWord("FR") || hasWord("FRANCE") -> "🇫🇷"
                hasWord("RU") || hasWord("RUSSIA") -> "🇷🇺"
                hasWord("SG") || hasWord("SINGAPORE") -> "🇸🇬"
                hasWord("FI") || hasWord("FINLAND") -> "🇫🇮"
                hasWord("TR") || hasWord("TURKEY") -> "🇹🇷"
                hasWord("PL") || hasWord("POLAND") -> "🇵🇱"
                hasWord("SE") || hasWord("SWEDEN") -> "🇸🇪"
                hasWord("CH") || hasWord("SWITZERLAND") -> "🇨🇭"
                hasWord("JP") || hasWord("JAPAN") -> "🇯🇵"
                hasWord("CA") || hasWord("CANADA") -> "🇨🇦"
                else -> "\uD83C\uDF0D" // Сервер подключен, но страну не распознали
            }
            binding.locationIndicator.text = emoji
        }
    }

    // Пространственная анимация главной кнопки
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