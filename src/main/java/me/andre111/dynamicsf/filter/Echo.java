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

// import org.lwjgl.openal.EXTEfx;

// import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;

public class Echo {
	private static boolean enabled = false;
	private static boolean doAverage = true;

	// for updateStats
	private static Vec3d newPos = new Vec3d(0,0,0);
	private static Vec3d furthest = new Vec3d(0,0,0);
	private static double distance = 0;
	private static int size = 2;
	private static float reduce = 0;
	private static float decay = 0;
	private static float sky = 0;


	// used to uniquely identify echos, without an audible difference
	private static float idPitch = 0.999999999999969f;
	// how many sounds to ignore (incremented upon an echo)
	private static int ignore = 0;

	
	// tracker variables for things past
	private static long prevTime = System.currentTimeMillis();
	private static Vec3d prevPos = new Vec3d(0,0,0);
	private static String prevName = "";

	// [[timer,soundInstance], ]
	private static List<Pair<Float,SoundInstance>> sounds = new ArrayList<>();

	public static boolean getEnabled() {
		return enabled;
	}

	public static boolean getDoAverage() {
		return doAverage;
	}

	public static void reset(final ConfigData data) {
		enabled = data.echoFilter.enabled;
		doAverage = data.echoFilter.doAverage;
		sounds = new ArrayList<>();
	}

	public static void updateSoundInstance(final SoundInstance soundInstance) {
		if (!enabled) {
			sounds = new ArrayList<>();
			return;
		}

		if (ignore > 0) {
			ignore--;
			return;
		}

		final Vec3d currentPos = new Vec3d(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ());
		final String currentName = soundInstance.getId().toString();
		final long time = System.currentTimeMillis();


		// filter out echos
		if (
			// if the name's new or the position's new or if 500ms has elapsed
			(prevName != currentName || currentPos.distanceTo(prevPos) >= 2 || Math.abs(prevTime - time) >= 500) &&
			// if the sound wasn't an echo, and was loud enough
			(soundInstance.getPitch() != idPitch && soundInstance.getVolume() > 0.2f)
		) {
			// only update time if a sound was pushed through
			prevTime = time;
			// debug
			// System.out.println("\t" + prevName + "\t" + currentName + "\t" + currentPos.distanceTo(prevPos));

			// add the sound to the echo 'queue'
			// not an actual queue due to memory handling surrounding queues
			sounds.add(new Pair<Float,SoundInstance>(30f,soundInstance));
		}
		prevPos = currentPos;
		prevName = currentName;
	}

	public static void updateStats(final ConfigData data, final Vec3d clientPos,
		final List<Vec3d> positions, final float reducer, final float decayValue, final float skyValue
	) {
		enabled = data.echoFilter.enabled;
		doAverage = data.echoFilter.doAverage;

		if (!enabled) return;

		// System.out.println("\n\n\n?");

		newPos = clientPos;
		furthest = clientPos;
		distance = 0;
		size = 2;

		// find furthest position
		for (Vec3d position : positions) {
			double newDistance = clientPos.distanceTo(position);
			if (newDistance > distance) {
				distance = newDistance;
				furthest = position;
			}
		}

		// average ? add values for mean()
		if (doAverage) {
			size += positions.size();
			for (Vec3d position : positions) {
				newPos = newPos.add(position);
			}
		}

		// evaluate average
		newPos = newPos.add(furthest).multiply(1f / size);

		// update values - if echo's at all reasonable, that is
		if (distance > 10) {
			reduce = reducer;
			decay = decayValue;
			sky = skyValue;
		} else {
			reduce = 1;
			decay = 0;
			sky = 0;
		}

	}

	public static void update(final MinecraftClient client) {
		// System.out.println("\n\n\n\t\t\t\t" + sounds.size());
		if (sounds.size() == 0) return;

		int i = 0;
		// increment values
		while (i < sounds.size()) {
			Pair<Float,SoundInstance> instance = null;

			// the try_catch stopped random errors appearing /shrug
			try {
				instance = sounds.get(i);
			} catch (Exception e) {
				System.out.println(e);
				continue;
			}
			
			final SoundInstance sound = instance.getSecond();

			float timer = instance.getFirst();
			// if timer's still going
			if (timer > 0) {
				// if reverb's not strong enough, don't duplicate sounds in a gross way
				if (reduce <= 2) {
					// System.out.println(decay);
					sounds.remove(i);
					continue;
				}

				final float reducer = Utils.clamp(reduce/20f,0f,0.75f);
				// decay the timer
				timer *= reducer * reducer * decay * decay;
				timer--;

				// if timer's still going, then don't do last section
				if (timer > 0) {
					// update timer value
					sounds.set(i, new Pair<Float,SoundInstance>(timer, sound));

					// don't loop infinitely!
					i++;
					continue;
				}
			}
			// else:

			// ensure the sound's ignored by echo
			ignore++;
			// calc volume: (vol/3 - echoDistance/128) * decay
			final float volume = (Utils.clamp(sound.getVolume() / 3f -
				(float) (new Vec3d(sound.getX(),sound.getY(),sound.getZ()))
				.distanceTo(newPos) / 128f)) * decay;
			// play sound, useDistance=false - no further delay
			client.world.playSound(newPos.getX(), newPos.getY(), newPos.getZ(), new SoundEvent(sound.getId()), sound.getCategory(), volume, idPitch, false);
			// remove the sound
			sounds.remove(i);

			// debug
			// System.out.println("\n\n\nECHO\n\n\n");
			// System.out.print(sound.getId() + "\t\t");
			// System.out.println(increment);

			// don't increment here, as a sound's removed
		}
	}
}
