package com.github.kr328.clash

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.design.MainDesign
import com.github.kr328.clash.design.ui.ToastDuration
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import com.github.kr328.clash.core.bridge.*
import com.github.kr328.clash.core.model.ProxySort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import com.github.kr328.clash.design.R

class MainActivity : BaseActivity<MainDesign>() {
    private var isTesting = false

    override suspend fun main() {
        val design = MainDesign(this)

        setContentDesign(design)
        design.fetch()

        // Легкий таймер (раз в 2 секунды) только для того, чтобы вовремя показать флаг после теста
        val ticker = ticker(TimeUnit.SECONDS.toMillis(2))

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart,
                        Event.ServiceRecreated,
                        Event.ClashStop, Event.ClashStart,
                        Event.ProfileLoaded, Event.ProfileChanged -> design.fetch()
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        MainDesign.Request.ToggleStatus -> {
                            if (clashRunning) stopClashService() else design.startClash()
                        }
                        MainDesign.Request.OpenProxy -> startActivity(ProxyActivity::class.intent)
                        MainDesign.Request.OpenProfiles -> startActivity(ProfilesActivity::class.intent)
                        MainDesign.Request.OpenProviders -> startActivity(ProvidersActivity::class.intent)
                        MainDesign.Request.OpenLogs -> {
                            if (LogcatService.running) startActivity(LogcatActivity::class.intent)
                            else startActivity(LogsActivity::class.intent)
                        }
                        MainDesign.Request.OpenSettings -> startActivity(SettingsActivity::class.intent)
                        MainDesign.Request.OpenHelp -> startActivity(HelpActivity::class.intent)
                        MainDesign.Request.OpenAbout -> design.showAbout(queryAppVersionName())

                        // Логика кнопки "Молния"
                        MainDesign.Request.RequestUrlTest -> {
                            if (clashRunning && !isTesting) {
                                launch {
                                    try {
                                        isTesting = true
                                        design.setTestingState(true)

                                        // Принудительно включаем "синий кружок" на время ручного теста
                                        design.setProxyName(null, true)

                                        withClash {
                                            val groups = queryProxyGroupNames(false)
                                            val targetGroup = groups.firstOrNull { it.equals("AUTO-VPN", true) }
                                                ?: groups.firstOrNull { it != "GLOBAL" && it != "DIRECT" && it != "REJECT" }

                                            if (targetGroup != null) {
                                                healthCheck(targetGroup)
                                            }
                                        }
                                        // Как только тест завершен - запрашиваем актуальный флаг
                                        design.fetchProxy()
                                    } catch (_: Exception) {
                                    } finally {
                                        isTesting = false
                                        design.setTestingState(false)
                                    }
                                }
                            }
                        }
                    }
                }

                if (clashRunning) {
                    ticker.onReceive {
                        // Обновляем флаг только если не идет тестирование
                        if (!isTesting) design.fetchProxy()
                    }
                }
            }
        }
    }

    private suspend fun MainDesign.fetch() {
        setClashRunning(clashRunning)
        if (clashRunning) fetchProxy() else setProxyName(null, false)
    }

    private suspend fun MainDesign.fetchProxy() {
        if (!clashRunning) {
            setProxyName(null, false)
            return
        }
        val activeProxy = withClash {
            try {
                val groups = queryProxyGroupNames(false)
                val mainGroup = groups.firstOrNull { it.equals("AUTO-VPN", true) }
                    ?: groups.firstOrNull { it != "GLOBAL" && it != "DIRECT" && it != "REJECT" }

                if (mainGroup != null) {
                    queryProxyGroup(mainGroup, ProxySort.Default).now
                } else null
            } catch (_: Exception) {
                null
            }
        }
        setProxyName(activeProxy, true)
    }

    private suspend fun MainDesign.startClash() {
        val active = withProfile { queryActive() }

        if (active == null || !active.imported) {
            showToast(R.string.no_profile_selected, ToastDuration.Long) {
                setAction(R.string.profiles) { startActivity(ProfilesActivity::class.intent) }
            }
            return
        }

        val vpnRequest = startClashService()
        try {
            if (vpnRequest != null) {
                val result = startActivityForResult(ActivityResultContracts.StartActivityForResult(), vpnRequest)
                if (result.resultCode == RESULT_OK) startClashService()
            }
        } catch (e: Exception) {
            design?.showToast(R.string.unable_to_start_vpn, ToastDuration.Long)
        }
    }

    private suspend fun queryAppVersionName(): String {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageInfo(packageName, 0).versionName + "\n" + Bridge.nativeCoreVersion().replace("_", "-")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { _: Boolean -> }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

val mainActivityAlias = "${MainActivity::class.java.name}Alias"