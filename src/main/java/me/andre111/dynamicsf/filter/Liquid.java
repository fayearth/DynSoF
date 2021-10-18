/*
 * Copyright (c) 2021 Andr? Schweiger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.andre111.dynamicsf.filter;

import me.andre111.dynamicsf.config.ConfigData;
import me.andre111.dynamicsf.Utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.fluid.FluidState;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
//import net.minecraft.util.math.MathHelper;

public class Liquid {
	private static boolean enabled = false;
	private static float gain = 1.0f;
	private static float gainHF = 1.0f;
	private static float targetGain = 1.0f;
	private static float targetGainHF = 1.0f;

	public static void updateGlobal(final boolean verdict, final MinecraftClient client, final ConfigData data, final Vec3d clientPos) {
		if (verdict) update(client, data, clientPos);
		else reset(/*data*/);
	}
	
	public static boolean updateSoundInstance(final SoundInstance soundInstance) {
		if (!enabled) return false;

		gain = Utils.clamp(gain);
		gainHF = Utils.clamp(gainHF);
		// removed a branch here
		if (gain + gainHF >= 2) return false;
		
		return true;
	}
	
	public static float getGain() {
		return gain;
	}

	public static float getGainHF() {
		return gainHF;
	}
	
	private static void reset() {
		enabled = false;
		gain = 1f;
		gainHF = 1f;
		targetGain = 1f;
		targetGainHF = 1f;
	}

	private static void update(final MinecraftClient client, final ConfigData data, final Vec3d clientPos) {
		enabled = data.liquidFilter.enabled;

		// if not enabled, exit
		if (!enabled /*|| clientPos == null*/) return;

		// round client pos
		BlockPos playerPos = new BlockPos(clientPos);
		
		// update target values
		FluidState fluidState = client.world.getFluidState(playerPos);

		// detect if in fluid, sort by usual density
		if (fluidState.isIn(FluidTags.LAVA) ) {
			targetGain = data.liquidFilter.lavaGain;
			targetGainHF = data.liquidFilter.lavaGainHF;
		} else if (fluidState.isIn(FluidTags.WATER) ) {
			targetGain = data.liquidFilter.waterGain;
			targetGainHF = data.liquidFilter.waterGainHF;
		} else {
			targetGain = 1f;
			targetGainHF = 1f;
		}
		
		// interpolate values
		gain = (targetGain + gain) / 2f;
		gainHF = (targetGainHF + gainHF) / 2f;
	}
}
