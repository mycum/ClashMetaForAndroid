package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.design.ProxyDesign
import com.github.kr328.clash.design.model.ProxyState
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ProxyActivity : BaseActivity<ProxyDesign>() {
    override suspend fun main() {
        val mode = withClash { queryOverride(Clash.OverrideSlot.Session).mode }

        // 1. Получаем ТОЛЬКО видимые группы для отрисовки вкладок
        val visibleNames = withClash { queryProxyGroupNames(true) }
        // 2. Получаем АБСОЛЮТНО ВСЕ группы (включая скрытый "⚡ Авто-выбор") для отслеживания серверов
        val allNames = withClash { queryProxyGroupNames(false) }

        // Создаем единую карту состояний для всех существующих групп в ядре
        val unorderedStates = allNames.associateWith { ProxyState("?") }
        // Привязываем видимые вкладки к их состояниям из карты
        val states = visibleNames.map { unorderedStates[it]!! }

        val reloadLock = Semaphore(10)

        val design = ProxyDesign(
            this,
            mode,
            visibleNames,
            uiStore
        )

        setContentDesign(design)

        design.requests.send(ProxyDesign.Request.ReloadAll)

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ProfileLoaded -> {
                            val newNames = withClash {
                                queryProxyGroupNames(true)
                            }

                            if (newNames != visibleNames) {
                                startActivity(ProxyActivity::class.intent)
                                finish()
                            }
                        }
                        else -> Unit
                    }
                }
                design.requests.onReceive {
                    when (it) {
                        ProxyDesign.Request.ReLaunch -> {
                            startActivity(ProxyActivity::class.intent)
                            finish()
                        }
                        ProxyDesign.Request.ReloadAll -> {
                            visibleNames.indices.forEach { idx ->
                                design.requests.trySend(ProxyDesign.Request.Reload(idx))
                            }
                        }
                        is ProxyDesign.Request.Reload -> {
                            launch {
                                val group = reloadLock.withPermit {
                                    withClash {
                                        queryProxyGroup(visibleNames[it.index], uiStore.proxySort)
                                    }
                                }
                                val state = states[it.index]

                                state.now = group.now

                                // ИСПРАВЛЕНИЕ: "Мягко" опрашиваем вложенные автоматические группы
                                group.proxies.forEach { p ->
                                    if (p.type.group && unorderedStates.containsKey(p.name)) {
                                        val subGroup = withClash { queryProxyGroup(p.name, uiStore.proxySort) }
                                        if (subGroup != null) {
                                            unorderedStates[p.name]?.now = subGroup.now
                                        }
                                    }
                                }

                                design.updateGroup(
                                    it.index,
                                    group.proxies,
                                    group.type == Proxy.Type.Selector,
                                    state,
                                    unorderedStates
                                )
                            }
                        }
                        is ProxyDesign.Request.Select -> {
                            withClash {
                                patchSelector(visibleNames[it.index], it.name)
                                states[it.index].now = it.name
                            }

                            design.requestRedrawVisible()
                        }
                        is ProxyDesign.Request.UrlTest -> {
                            launch {
                                withClash {
                                    healthCheck(visibleNames[it.index])
                                }
                                design.requests.send(ProxyDesign.Request.Reload(it.index))
                            }
                        }
                        is ProxyDesign.Request.PatchMode -> {
                            design.showModeSwitchTips()

                            withClash {
                                val o = queryOverride(Clash.OverrideSlot.Session)
                                o.mode = it.mode
                                patchOverride(Clash.OverrideSlot.Session, o)
                            }
                        }
                    }
                }
            }
        }
    }
}