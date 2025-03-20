package us.timinc.mc.cobblemon.whiteout

import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent
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
            CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.LOWEST, Whiteout::handleBattleFainted)
            eventsAttached = true
        }
    }

    private fun handleBattleFainted(battleFaintedEvent: BattleFaintedEvent) {
        val killed = battleFaintedEvent.killed
        val entity = killed.entity ?: return
        val owner = entity.owner ?: return
        if (killed.actor.type != ActorType.PLAYER) return
        if (killed.actor.pokemonList.all { it.health == 0 }) {
            owner.kill()
        }
    }
}