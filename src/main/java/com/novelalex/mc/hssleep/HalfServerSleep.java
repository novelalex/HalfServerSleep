package com.novelalex.mc.hssleep;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.GameRules;
import net.minecraft.world.level.ServerWorldProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HalfServerSleep implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("halfserversleep");

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(this::onTick);
	}

	private void onTick(MinecraftServer server) {
		server.getWorlds().forEach((world)->{
			List<ServerPlayerEntity> players = world.getPlayers();
			int totalPlayers = players.size();
			int sleepers = 0;
			for(PlayerEntity p : players) {
				if(p.isSleeping()) {
					// make sure player slept for long enough
					if (p.getSleepTimer() == 100) {
						sleepers += 1;
					}
				}
			}
			if (sleepers >= (totalPlayers / 2)) {
				skipNight(world, players, totalPlayers, sleepers);
			}
		});
	}

	private void skipNight(ServerWorld world, List<ServerPlayerEntity> players, int totalPlayers, int sleepers) {
		// set world stuff
		if(world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)){
			int numDays = (int) (world.getLevelProperties().getTimeOfDay() / 24000);
			long newTime = (numDays+1) * 24000L;
			world.setTimeOfDay(newTime);
		}

		// reset weather
		if(world.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)){
			((ServerWorldProperties)world.getLevelProperties()).setRainTime(0);
			((ServerWorldProperties)world.getLevelProperties()).setRaining(false);
			((ServerWorldProperties)world.getLevelProperties()).setThunderTime(0);
			((ServerWorldProperties)world.getLevelProperties()).setThundering(false);
		}

		// wake up sleeping players
		players.forEach(p->{
			if(p.isSleeping()) {
				p.wakeUp(false, true);
			}
			p.sendMessage(Text.of(sleepers + "/" + sleepers + " slept"), true);
		});
	}
}