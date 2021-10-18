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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import org.lwjgl.openal.EXTEfx;
import org.lwjgl.openal.AL11;

import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.world.ClientWorld;
//import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
//import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;

public class Reverb {
	private static int id = -1;
	private static int slot = -1;

	private static boolean enabled = false;
	private static boolean checkSky = true;
	private static int ticks = 0;
	private static float prevDecayFactor = 0f;
	private static float prevRoomFactor = 0f;
	private static float prevSkyFactor = 0f;

	private static float density = 0.2f;
	private static float diffusion = 0.6f;
	private static float gain = 0.15f;
	private static float gainHF = 0.8f;
	private static float decayTime = 0.1f;
	private static float decayHFRatio = 0.7f;
	private static float reflectionsGain = 0f;
	private static float reflectionsDelay = 0f;
	private static float lateReverbGain = 0f;
	private static float lateReverbDelay = 0f;
	private static float airAbsorptionGainHF = 0.99f;
	private static float roomRolloffFactor = 0f;
	private static int decayHFLimit = 1;

	private static int[] scanSizes = new int[] {40, 50, 40, 20, 50};
	// skip direction.up, use checkSky for that
	private static final Direction[] validationOffsets = new Direction[] { Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
	// initial tracer value
	private static final Vec3d initPoint = new Vec3d(0d, 0d, 1d).rotateX(-22.5f);

	// the direction-based increment used for raycasting
	private static Vec3d tracer = initPoint;
	// raycast rotation value
	private static int tracerOffset = 0;
	// how many sky-available blocks are there
	private static float sky = 0;
	// how far everything was, combined
	private static float compositeDistance = 0;


	// TODO: move this into settings update calculations
	// min: 1, max: 6
	// final int quality = data.reverbFilter.quality;
	private static int quality = 4; // mid - 64
	// starting offset for scanning
	private static Vec3d halfBox = new Vec3d(quality, quality, quality).multiply(0.5);
	// randomly generated offset for raycast
	//	list of
	//		surface location + list of
	// 			blockID + material
	private static List<Pair<Identifier,Material>> surfaces = new ArrayList<>();
	private static List<Vec3d> positions = new ArrayList<>();
	
	//TODO: configurable
	private static List<Material> HIGH_REVERB_MATERIALS = Arrays.asList(Material.STONE, Material.GLASS, Material.ICE, Material.DENSE_ICE, Material.METAL);
	private static List<Material> LOW_REVERB_MATERIALS = Arrays.asList(Material.WOOL, Material.CARPET, Material.LEAVES, Material.PLANT, Material.UNDERWATER_PLANT, Material.REPLACEABLE_PLANT, Material.REPLACEABLE_UNDERWATER_PLANT, Material.SOLID_ORGANIC, Material.GOURD, Material.CACTUS, Material.COBWEB, Material.CAKE, Material.SPONGE, Material.SNOW_LAYER, Material.SNOW_BLOCK, Material.WOOD);

	public static void reinit() {
		id = EXTEfx.alGenEffects();
		slot = EXTEfx.alGenAuxiliaryEffectSlots();
		final int err = AL11.alGetError();
		if (err != AL11.AL_NO_ERROR) System.out.println(err);
	}

	public static void updateGlobal(final boolean verdict, final MinecraftClient client, final ConfigData data, final Vec3d clientPos) {
		if (verdict) update(client, data, clientPos);
		else reset(data);
	}

	public static boolean updateSoundInstance(final SoundInstance soundInstance) {
		// ensure sound id is valid
		if (id == -1) {
			reinit();
		}

		// if not needed, exit
		if (!enabled || reflectionsDelay + lateReverbDelay <= 0 ) return false;

		if (soundInstance.getAttenuationType() == SoundInstance.AttenuationType.LINEAR) {
			roomRolloffFactor = 2f / (Math.max(soundInstance.getVolume(), 1f) + 2f);
		} else {
			roomRolloffFactor = 0f;
		}

		density = Utils.clamp(density);
		diffusion = Utils.clamp(diffusion);
		gain = Utils.clamp(gain);
		gainHF = Utils.clamp(gainHF);
		decayTime = Utils.clamp(decayTime, 0.1f,20f);
		decayHFRatio = Utils.clamp(decayHFRatio, 0.1f,2f);
		reflectionsGain = Utils.clamp(reflectionsGain, 0f,3.16f);
		reflectionsDelay = Utils.clamp(reflectionsDelay, 0f,0.3f);
		lateReverbGain = Utils.clamp(lateReverbGain, 0f,10f);
		lateReverbDelay = Utils.clamp(lateReverbDelay, 0,0.1f);
		airAbsorptionGainHF = Utils.clamp(airAbsorptionGainHF, 0.892f,1f);
		roomRolloffFactor = Utils.clamp(roomRolloffFactor, 0f,10f);
		decayHFLimit = Utils.clamp(decayHFLimit);

		EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, 0);
		EXTEfx.alEffecti(id, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);
		EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_DENSITY, density);
		EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_DIFFUSION, diffusion);
		EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_GAIN, gain);
		EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_GAINHF, gainHF);
		EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_DECAY_TIME, decayTime);
		EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_DECAY_HFRATIO, decayHFRatio);
		EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, reflectionsGain);
		EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_REFLECTIONS_DELAY, reflectionsDelay);
		EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, lateReverbGain);
		EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_LATE_REVERB_DELAY, lateReverbDelay);
		EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_AIR_ABSORPTION_GAINHF, airAbsorptionGainHF);
		EXTEfx.alEffectf(id, EXTEfx.AL_REVERB_ROOM_ROLLOFF_FACTOR, roomRolloffFactor);
		EXTEfx.alEffecti(id, EXTEfx.AL_REVERB_DECAY_HFLIMIT, decayHFLimit);
		EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, id);
		EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, 1);

		return true;
	}

	public static int getSlot() {
		return slot;
	}

	private static void reset(final ConfigData data) {
		enabled = false;
		density = data.reverbFilter.density;
		diffusion = data.reverbFilter.diffusion;
		gain = data.reverbFilter.gain;
		gainHF = data.reverbFilter.gainHF;
		decayTime = data.reverbFilter.minDecayTime;
		decayHFRatio = data.reverbFilter.decayHFRatio;
		reflectionsGain = 0;
		reflectionsDelay = 0;
		lateReverbGain = 0;
		lateReverbDelay = 0;
		airAbsorptionGainHF = data.reverbFilter.airAbsorptionGainHF;
		roomRolloffFactor = 0.0f;

		tracer = initPoint;
		sky = 0;

		quality = data.reverbFilter.quality; // mid - 64
		halfBox = new Vec3d(quality, quality, quality).multiply(0.5);
		scanSizes = new int[] {quality * 8, quality * 20, quality * 8, quality * 8, quality * 20};
		Echo.reset(data);
	}

	private static void update(final MinecraftClient client, final ConfigData data, final Vec3d clientPos) {
		enabled = data.reverbFilter.enabled;
		if (!enabled) return;

		// split up scanning over multiple ticks
		if (ticks < 16) {
			// raycast
			final Object[] raycast = trace(client, clientPos/*.add(tracerOffset) we're rotating now*/, scanSizes[(int) ticks / 4], tracer);
			final boolean foundSurface = (boolean) raycast[0];

			// if something was hit, then gather information about it
			if (foundSurface) {
				Vec3d pos = (Vec3d) raycast[1];
				final BlockPos blockPos = new BlockPos(pos);
				// move box bounds negative, so that surface spot is the center
				pos = pos.subtract(halfBox);
				// main calculation
				// n^3 - I wonder if there's any way to reduce this
				// to at least log(3n)
				for (double x = 0; x < quality; x++) {
					for (double z = 0; z < quality; z++) {
						// loop through y first, less likely to need to
						// get a different blockstate, resulting (I think)
						// in better memory usage
						for (double y = 0; y < quality; y++) {
							// get information
							final BlockState blockState = client.world.getBlockState(new BlockPos(pos.add(x, y, z)));
							final Material material = blockState.getMaterial();
							final Identifier blockID = Registry.BLOCK.getId(blockState.getBlock());
							// if block could reverb, and is solid, await calculations
							if (
								material.blocksMovement() ||
								!( material == Material.AIR
								|| material == Material.WATER
								|| material == Material.LAVA
								) ) surfaces.add(new Pair<>(blockID, material));
						}
					}
				}
				positions.add(new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
			}

			// move to the next cardinal direction
			tracer = tracer.rotateY(90);
			// after 1, every 4 ticks move up 1 level
			if (++ticks % 4 == 0) tracer = tracer.rotateX(22.5f);

		} else if (ticks < 20) {
			// check below you
			if (ticks == 18) {
				final int scanSize = scanSizes[3]; // ticks(18) / 4 = 4 (index)
				final Object[] raycast = trace(client, clientPos, scanSize, new Vec3d(0,-1,0));
				// if found surface, and distance between steps taken and
				// scanSize is over 1/3rd: add 2/3rds of steps taken
				if((boolean) raycast[0] && Math.abs((int) raycast[2] - scanSize) > scanSize / 3)
					// cheap way to add reverb from below
					sky += (int) raycast[2] / 3;
			}
			// keep ticking
			ticks++;
		} else {
			ticks = 0;
			tracer = initPoint;
			
			checkSky = data.reverbFilter.checkSky;
			halfBox = new Vec3d(quality, quality, quality).multiply(0.5);

			final float reverbPercent = data.reverbFilter.reverbPercent;
			final float minDecayTime = data.reverbFilter.minDecayTime;
			final float reflectionGainBase = data.reverbFilter.reflectionsGainBase;
			final float reflectionGainMultiplier = data.reverbFilter.reflectionsGainMultiplier;
			final float reflectionDelayMultiplier = data.reverbFilter.reflectionsDelayMultiplier;
			final float lateReverbGainBase = data.reverbFilter.lateReverbGainBase;
			final float lateReverbGainMultiplier = data.reverbFilter.lateReverbGainMultiplier;
			final float lateReverbDelayMultiplier = data.reverbFilter.lateReverbDelayMultiplier;

			// get base reverb
			float decayFactor = 0f;
			double highReverb = 0d;
			double midReverb  = 0d;
			double lowReverb  = 0d;

			// try {
			decayFactor = data.reverbFilter.getDimensionBaseReverb(
				client.world.getRegistryKey().getValue() );
			// } catch (NullPointerException e) {
			// 	surfaces = new ArrayList<>();
			// 	return; // if no client.world
			// }

			// loop through surfaces
			for (Pair<Identifier,Material> block : surfaces) {
				// custom block reverb overrides
				final ReverbInfo customReverb = data.reverbFilter.getCustomBlockReverb( block.getFirst() );
				// calculate reverbage
				if (customReverb == null) {
					// material based reverb
					if (HIGH_REVERB_MATERIALS.contains( block.getSecond() )) highReverb++;
					else if (LOW_REVERB_MATERIALS.contains( block.getSecond() )) lowReverb++;
					else midReverb++;
				} else {
					// custom reverb
					switch(customReverb) {
					case HIGH: highReverb++; break;
					case LOW:   lowReverb++; break;
					default:    midReverb++; break;
					}
				}
			}

			// calculate decay factor
			if (highReverb + midReverb + lowReverb > 0d) {
				decayFactor += (highReverb - lowReverb) / (highReverb + midReverb + lowReverb);
			}
			decayFactor = Utils.clamp(decayFactor);

			final int posCount = Utils.clamp(positions.size(), 1,16);
			// float reverbage = posCount / (float) (compositeDistance/posCount);
			float reverbage = Utils.clamp(posCount / (float) Math.sqrt(compositeDistance), 0,5f);

			// calculate sky value, before passing to echo
			sky = Utils.clamp(sky / (posCount * posCount), 0.1f,30);

			// interpolate values
			decayFactor = (decayFactor + prevDecayFactor) / 2f;
			prevDecayFactor = decayFactor;

			reverbage = (reverbage + prevRoomFactor) / 2f;
			prevRoomFactor = reverbage;

			sky = (sky + prevSkyFactor) / 2f;
			prevSkyFactor = sky;

			// clear blocks
			surfaces = new ArrayList<>();
			positions = new ArrayList<>();


			// update values
			decayTime = Math.max(minDecayTime, (reverbPercent + sky) * 6f * decayFactor * reverbage);
			reflectionsGain = (reverbPercent + sky) * (reflectionGainBase + reflectionGainMultiplier * reverbage);
			reflectionsDelay = reflectionDelayMultiplier * reverbage;
			lateReverbGain = reverbPercent * (lateReverbGainBase + lateReverbGainMultiplier * reverbage);
			lateReverbDelay = lateReverbDelayMultiplier * reverbage;

			// process echo values
			Echo.updateStats(data, clientPos, positions, decayTime, decayFactor, sky);

			// debug
			System.out.println(Float.toString(decayFactor) +
			"," + Float.toString(reverbage) +
			"," + Float.toString(sky) +
			"," + Float.toString(reflectionsGain) +
			"," + Float.toString(reflectionsDelay) +
			"," + Float.toString(lateReverbGain) +
			"," + Float.toString(lateReverbDelay)
				);

			sky = 0;
			compositeDistance = 0;
			// 90/5*2=36 - 5s loop
			tracerOffset = (tracerOffset + 36) % 360;
		}
	}

	private static Object[] trace(final MinecraftClient client, Vec3d pos, int range, final Vec3d tracer) {

		final Vec3d offsetTracer = tracer.rotateY(tracerOffset);

		BlockPos blockPos = new BlockPos(pos);
		boolean foundSurface = false;
		int steps = 0;
		// ensure at least one loop
		range = Math.max(1, range);
		// loop
		for (; steps < range; steps++) {
			blockPos = new BlockPos(pos);
			// final BlockState blockState = client.world.getBlockState(blockPos);
			// if full block here
			if ( client.world.getBlockState(blockPos).isFullCube(client.world, blockPos) ) {
				// if distance is big enough
				if (steps >= 2) {
					
					// detect if it's a real surface, not a fickle obstruction
					int surface = 0;
					// check surrounding blocks
					for (Direction direction : validationOffsets) {
						// copy, then move the position in _ direction
						BlockPos bPos = blockPos.offset(direction, 1);
						if (client.world.getBlockState(bPos).isFullCube(client.world, bPos)) surface++;
					}
					
					if (surface >= 3) {
						foundSurface = true;
						compositeDistance += steps;
					}
				}
				break;
				// if found still fluid, it's a body of water, stop looping
			} else if ( client.world.getFluidState(blockPos).isStill() ) break;
			// otherwise, move along
			pos = pos.add(offsetTracer);

			// check sky access every 5 blocks, starting at 1
			if ( checkSky && steps % 5 == 1 && hasSkyAbove(client.world, blockPos) ) sky++;
		}
		return new Object[] { foundSurface, pos, steps };
	}

	private static boolean hasSkyAbove(final ClientWorld world, final BlockPos pos) {
		if (world.getDimension().hasCeiling() ) return false;
		
		final Chunk chunk = world.getChunk(pos);
		final Heightmap heightMap = chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING);
		// just use modulo?
		final int x = Math.abs(pos.getX() % 16); // 16 = chunk size
		final int z = Math.abs(pos.getZ() % 16);
		// int x = pos.getX() - chunk.getPos().getStartX();
		// int z = pos.getZ() - chunk.getPos().getStartZ();
		// x = Math.max(0, Math.min(x, 15) );
		// z = Math.max(0, Math.min(z, 15) );
		return heightMap != null && heightMap.get(x, z) <= pos.getY();
	}
	
	public static enum ReverbInfo {
		HIGH,
		MID,
		LOW;
		
		public static ReverbInfo fromName(String name) {
			name = name.toUpperCase();
			return ReverbInfo.valueOf(name);
		}
	}
}

