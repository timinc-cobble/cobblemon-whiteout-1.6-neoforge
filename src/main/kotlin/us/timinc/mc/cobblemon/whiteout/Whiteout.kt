package us.timinc.mc.cobblemon.whiteout

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.Priority
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonFaintedEvent
import com.cobblemon.mod.common.api.scheduling.afterOnServer
import com.cobblemon.mod.common.util.getPlayer
import com.cobblemon.mod.common.util.isInBattle
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageType
import net.neoforged.fml.common.Mod
import us.timinc.mc.cobblemon.whiteout.config.ConfigBuilder
import us.timinc.mc.cobblemon.whiteout.config.WhiteoutConfig

@Mod(Whiteout.MOD_ID)
object Whiteout {
    @Suppress("MemberVisibilityCanBePrivate")
    const val MOD_ID = "whiteout"

    @Suppress("MemberVisibilityCanBePrivate")
    val config: WhiteoutConfig = ConfigBuilder.load(WhiteoutConfig::class.java, MOD_ID)

    @Suppress("MemberVisibilityCanBePrivate")
    val POKEBATTLE_DAMAGE_SOURCE: ResourceKey<DamageType> = ResourceKey.create(
        Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "pokebattle")
    )

    @Suppress("MemberVisibilityCanBePrivate")
    val POKEBATTLE_FLED_DAMAGE_SOURCE: ResourceKey<DamageType> = ResourceKey.create(
        Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MOD_ID, "fled")
    )

    init {
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.LOWEST, ::handleBattleVictory)
        CobblemonEvents.BATTLE_FLED.subscribe(Priority.LOWEST, ::handleBattleFled)
        CobblemonEvents.POKEMON_FAINTED.subscribe(Priority.LOWEST, ::handlePokemonFainted)
    }

    private fun handlePokemonFainted(evt: PokemonFaintedEvent) {
        if (config.killIfTeamKnockedOut && (evt.pokemon.getOwnerPlayer()?.isInBattle() == false)) {
            evt.pokemon.getOwnerPlayer()?.let { killEmAll(listOf(it), POKEBATTLE_DAMAGE_SOURCE) }
        }
    }

    private fun handleBattleFled(evt: BattleFledEvent) {
        if (config.killIfFled) killEmAll(
            evt.player.getPlayerUUIDs().mapNotNull { it.getPlayer() }, POKEBATTLE_FLED_DAMAGE_SOURCE
        )
    }

    private fun handleBattleVictory(evt: BattleVictoryEvent) {
        val playersToKill: MutableSet<ServerPlayer> = mutableSetOf()
        if (config.killIfTeamKnockedOut) {
            for (actor in evt.losers + evt.winners) {
                for (playerUUID in actor.getPlayerUUIDs()) {
                    val player = playerUUID.getPlayer() ?: continue
                    val playerParty = Cobblemon.storage.getParty(player)
                    if (playerParty.all { it.currentHealth == 0 }) {
                        playersToKill.add(player)
                    }
                }
            }
        }
        if (config.killIfLost) playersToKill.addAll(evt.losers.flatMap { loser ->
            loser.getPlayerUUIDs().mapNotNull { it.getPlayer() }
        })
        killEmAll(playersToKill.toList(), POKEBATTLE_DAMAGE_SOURCE)
    }

    private fun killEmAll(players: List<ServerPlayer>, killer: ResourceKey<DamageType>) {
        players.forEach { player ->
            val playerParty = Cobblemon.storage.getParty(player)
            if (config.healTeam) {
                afterOnServer(1, player.level()) {
                    playerParty.heal()
                }
            }
            player.hurt(
                if (!config.showMessage) player.damageSources().genericKill() else player.damageSources().source(
                    killer
                ), Float.MAX_VALUE
            )
        }
    }
}