package us.timinc.mc.cobblemon.whiteout

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent
import com.cobblemon.mod.common.api.scheduling.afterOnServer
import com.cobblemon.mod.common.util.getPlayer
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.damagesource.DamageType
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.event.server.ServerStartedEvent
import us.timinc.mc.cobblemon.whiteout.config.ConfigBuilder
import us.timinc.mc.cobblemon.whiteout.config.WhiteoutConfig

@Mod(Whiteout.MOD_ID)
object Whiteout {
    const val MOD_ID = "whiteout"
    val config: WhiteoutConfig = ConfigBuilder.load(WhiteoutConfig::class.java, MOD_ID)
    var eventsAttached = false

    val POKEBATTLE_DAMAGE_SOURCE: ResourceKey<DamageType> = ResourceKey.create(
        Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "pokebattle")
    )

    @EventBusSubscriber
    object Registration {
        @SubscribeEvent
        fun init(e: ServerStartedEvent) {
            if (eventsAttached) return
            CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.LOWEST, Whiteout::handleBattleFainted)
            eventsAttached = true
        }
    }

    private fun handleBattleFainted(evt: BattleVictoryEvent) {
        val allActors: MutableList<BattleActor> = mutableListOf()
        allActors.addAll(evt.losers)
        allActors.addAll(evt.winners)
        for (actor in allActors) {
            for (playerUUID in actor.getPlayerUUIDs()) {
                val player = playerUUID.getPlayer() ?: continue
                val playerParty = Cobblemon.storage.getParty(player)
                if (playerParty.all { it.currentHealth == 0 }) {
                    if (config.healTeam) {
                        afterOnServer(1, player.level()) {
                            playerParty.heal()
                        }
                    }
                    player.hurt(
                        if (!config.showMessage) player.damageSources().genericKill() else player.damageSources()
                            .source(
                                POKEBATTLE_DAMAGE_SOURCE
                            ), Float.MAX_VALUE
                    )
                }
            }
        }
    }
}