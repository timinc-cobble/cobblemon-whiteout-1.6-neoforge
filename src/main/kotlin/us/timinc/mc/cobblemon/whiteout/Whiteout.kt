package us.timinc.mc.cobblemon.whiteout

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent
import com.cobblemon.mod.common.util.getPlayer
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.event.server.ServerStartedEvent

@Mod(Whiteout.MOD_ID)
object Whiteout {
    const val MOD_ID = "whiteout"
    var eventsAttached = false

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
                if (Cobblemon.storage.getParty(player).all { it.currentHealth == 0 }) {
                    player.kill()
                }
            }
        }
    }
}