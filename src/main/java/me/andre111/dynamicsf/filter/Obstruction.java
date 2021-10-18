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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.openal.EXTEfx;

//import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstance.AttenuationType;
import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Obstruction {
	private static int id = -1;
	
	private static boolean enabled = false;
	private static Map<SoundInstance, Float> obstructions = new HashMap<>();
	private static Queue<SoundInstance> toScan = new ConcurrentLinkedQueue<>();
	
	// TODO: configurable
	private static List<Material> HIGH_OBSTRUCTION_MATERIALS = Arrays.asList(Material.WOOL, Material.SPONGE);
	
	public static void reinit() {
		id = EXTEfx.alGenFilters();
	}
	
	public static void updateGlobal(final boolean verdict, final MinecraftClient client, final ConfigData data, final Vec3d clientPos) {
		// pass along to submodules
		Liquid.updateGlobal(verdict, client, data, clientPos);
		
		if (verdict) update(client, data, clientPos);
		else reset(/*data*/);
	}
	
	public static boolean updateSoundInstance(final SoundInstance soundInstance, final ConfigData data) {
		// ensure sound id is valid
		if (id == -1) {
			reinit();
		}
		
		// process liquid filter
		float gain = 1f;
		float gainHF = 1f;
		if (Liquid.updateSoundInstance(soundInstance) ) {
			gain = Liquid.getGain();
			gainHF = Liquid.getGainHF();
		}
		
		// process this filter
		if (enabled && soundInstance.getAttenuationType() == AttenuationType.LINEAR) {
			Float obstructionAmount = obstructions.get(soundInstance);
			if (obstructionAmount == null) {
				toScan.add(soundInstance);
				obstructionAmount = 0f;
			} else if (obstructionAmount > 0.01f) {
				// bass is more audible through walls, treat it as such
				// I used PEMDAS. Sue me.
				// (My 4th grade math teacher crying with tears of joy)
				gain *= 1f - obstructionAmount * data.obstructionFilter.obstructionMax;
				gainHF *= 1f - obstructionAmount * 1.1f;
			}
		}

		gain = Utils.clamp(gain);
		gainHF = Utils.clamp(gainHF);
		// removed a branch here
		if (gain + gainHF >= 2f) return false;
		
		// TODO: implement caching system -- maybe???
		// openAL's effect system seems performant enough...
		EXTEfx.alFilteri(id, EXTEfx.AL_FILTER_TYPE, EXTEfx.AL_FILTER_LOWPASS);
		EXTEfx.alFilterf(id, EXTEfx.AL_LOWPASS_GAIN, gain);
		EXTEfx.alFilterf(id, EXTEfx.AL_LOWPASS_GAINHF, gainHF);
		
		return true;
	}
	
	public static int getID() {
		return id;
	}
	
	private static void reset(/*final ConfigData data*/) {
		enabled = false;
		obstructions.clear();
	}

	private static void update(final MinecraftClient client, final ConfigData data, final Vec3d clientPos) {
		enabled = data.obstructionFilter.enabled;
		
		// remove finished / stopped sounds
		obstructions.entrySet().removeIf (entry -> !client.getSoundManager().isPlaying(entry.getKey() ));
		
		// add new sounds
		SoundInstance newSoundInstance = null;
		while((newSoundInstance = toScan.poll() ) != null) obstructions.put(newSoundInstance, 0f);
		
		// update sound obstructions
		for(Map.Entry<SoundInstance, Float> e : obstructions.entrySet() ) {
			final float currentAmount = e.getValue();
			// prevent crashing on null pos
			final float nextAmount = getObstructionAmount(e.getKey(), client, data, clientPos);
			e.setValue((currentAmount + nextAmount * 2f) / 3f);
		}
	}
	
	private static float getObstructionAmount(final SoundInstance soundInstance, final MinecraftClient client, final ConfigData data, final Vec3d clientPos) {
		// prevent crashing on null pos
		//if (clientPos == null) return 0f;
		final float obstructionStep = data.obstructionFilter.obstructionStep;
		final float obstructionMax = data.obstructionFilter.obstructionMax;
		
		// get positions
		// .3f is for adjusting player footstep volume down cheap stairs
		// (realism tweak)
		final Vec3d soundPos = new Vec3d(soundInstance.getX(), soundInstance.getY() + 0.3f, soundInstance.getZ() );
		Vec3d currentPos = soundPos;
		// validation wasn't necessary!
		BlockPos.Mutable currentPosInt = new BlockPos.Mutable(soundPos.x, soundPos.y, soundPos.z);
		
		// get and evaluate distance
		final int distanceRounded = (int) Math.ceil( clientPos.distanceTo(soundPos) );
		// if distance is large, let builtin system handle it
		// 4x as far as a bell's audible, but ~36 less than a ghast's vocals
		if (distanceRounded > 64 || distanceRounded == 1) return 0f;

		// get xyz distance between sound and player, then make it incremental
		final Vec3d increments = clientPos.subtract(soundPos).multiply(1f / distanceRounded);

		// check obstruction
		float obstruction = 0.0f;
		// 0 <= loops <= 64
		for(int i=0; i <= distanceRounded; i++) {
			// increment
			currentPos = new Vec3d(currentPos.x + increments.x, currentPos.y + increments.y, currentPos.z + increments.z);
			// real block position
			currentPosInt.set(currentPos.x, currentPos.y, currentPos.z);

			// get block data
			final BlockState blockState = client.world.getBlockState(currentPosInt);
			// calculate obstruction
			if (blockState.isFullCube(client.world, currentPosInt) ) {
				// I used math. :)
				obstruction += increments.length() *
					obstructionStep *
					(HIGH_OBSTRUCTION_MATERIALS.contains( blockState.getMaterial() ) ? 2 : 1);
				
				if (obstruction >= obstructionMax) return obstructionMax;
			}
		}
		// if (distanceRounded > 2) {
		// 	/*if (obstruction == 0.0f) System.out.print(distanceRounded+" ");
		// 	else*/ System.out.println(String.format("\n%.2f",obstruction) + "\t\t"+distanceRounded+"\n");
		// }
		
		return obstruction;
	}
}
