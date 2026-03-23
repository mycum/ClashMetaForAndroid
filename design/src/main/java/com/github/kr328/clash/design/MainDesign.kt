package com.github.kr328.clash.design

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.animation.AnimationUtils
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
        ToggleStatus, OpenProxy, OpenProfiles, OpenProviders, OpenLogs, OpenSettings, OpenHelp, OpenAbout, RequestUrlTest
    }

    private val binding = DesignMainBinding.inflate(context.layoutInflater, context.root, false)

    private var isCurrentlyRunning: Boolean? = null

    override val root: View get() = binding.root

    // Управление состоянием тестирования (блокировка кнопок + анимация)
    suspend fun setTestingState(isTesting: Boolean) {
        withContext(Dispatchers.Main) {
            // Отключаем клики по остальным кнопкам
            binding.btnProxy.isEnabled = !isTesting
            binding.btnSettings.isEnabled = !isTesting

            // Делаем их полупрозрачными для визуального отклика
            binding.btnProxy.alpha = if (isTesting) 0.5f else 1.0f
            binding.btnSettings.alpha = if (isTesting) 0.5f else 1.0f

            if (isTesting) {
                // Меняем иконку на спиннер и крутим
                binding.btnHealthCheck.setImageResource(R.drawable.ic_baseline_sync)
                val rotation = AnimationUtils.loadAnimation(context, R.anim.rotate_infinite)
                binding.btnHealthCheck.startAnimation(rotation)
            } else {
                // Возвращаем молнию
                binding.btnHealthCheck.clearAnimation()
                binding.btnHealthCheck.setImageResource(R.drawable.ic_baseline_flash_on)
            }
        }
    }

    // Оставляем пустыми для совместимости
    suspend fun setProfileName(name: String?) {}
    suspend fun setForwarded(value: Long) {}
    suspend fun setMode(mode: TunnelState.Mode) {}
    suspend fun setHasProviders(has: Boolean) {}

    // Умный парсер имени сервера
    suspend fun setProxyName(name: String?) {
        withContext(Dispatchers.Main) {
            if (name == null) {
                binding.locationIndicator.text = "🌍"
                return@withContext
            }

            // 1. Ищем готовый эмодзи флага в имени
            val flagRegex = Regex("[\\uD83C\\uDDE6-\\uD83C\\uDDFF]{2}")
            val match = flagRegex.find(name)
            if (match != null) {
                binding.locationIndicator.text = match.value
                return@withContext
            }

            // 2. Фолбэк на словарь (поиск по целым словам)
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
                else -> "🌍"
            }
            binding.locationIndicator.text = emoji
        }
    }

    suspend fun setClashRunning(running: Boolean) {
        withContext(Dispatchers.Main) {
            // Игнорируем холостые вызовы
            if (isCurrentlyRunning == running) return@withContext

            val isFirstRun = isCurrentlyRunning == null
            isCurrentlyRunning = running

            binding.clashRunning = running

            // 1. Красим тумблер
            val colorId = if (running) R.color.telegood_accent_blue else R.color.telegood_icon_off
            val color = ContextCompat.getColor(context, colorId)
            binding.switchView.trackTintList = ColorStateList.valueOf(color)

            // 2. Анимация рамки кнопки (перетекание из серого в синий)
            if (binding.mainPillButton.background !is android.graphics.drawable.TransitionDrawable) {
                binding.mainPillButton.background = ContextCompat.getDrawable(context, R.drawable.bg_pill_transition)
            }
            val transition = binding.mainPillButton.background as android.graphics.drawable.TransitionDrawable
            transition.isCrossFadeEnabled = true

            if (isFirstRun) {
                // Если приложение только открылось - ставим состояния без анимации
                if (running) transition.startTransition(0) else transition.resetTransition()
                binding.glowBackground.alpha = if (running) 1f else 0f
                binding.glowBackground.scaleX = if (running) 1f else 0.85f
                binding.glowBackground.scaleY = if (running) 1f else 0.85f
            } else {
                // 3. Та самая ПРОСТРАНСТВЕННАЯ АНИМАЦИЯ при клике!
                if (running) transition.startTransition(400) else transition.reverseTransition(400)

                binding.glowBackground.animate()
                    .alpha(if (running) 1f else 0f)
                    .scaleX(if (running) 1f else 0.85f)
                    .scaleY(if (running) 1f else 0.85f)
                    .setDuration(400)
                    .setInterpolator(android.view.animation.DecelerateInterpolator()) // Свет вырывается резко и плавно тормозит в конце
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