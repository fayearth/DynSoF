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
package me.andre111.dynamicsf.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.util.Identifier;

public class ConfigDataGeneral {
	public static final List<String> DEFAULT_IGNORED_SOUND_EVENTS = Collections.unmodifiableList(Arrays.asList(
			// starts with
			"minecraft:block.lava.pop", // too frequent
			"minecraft:weather.rain", // too frequent
			"minecraft:music", // too frequent and nonsensical
			"minecraft:ui", // too frequent and nonsensical
			"atm", // atmosfera / atmosfera
			"dyn", // dynmus / dynamicMusic

			// ends with
			"_walk", // ends with / presencefootsteps.pf.grass.grass_walk
			"_wander", // ends with / presencefootsteps.pf.grass.grass_wander
			".step" // ends with / mc:block.copper.step
			) );
	
	public List<String> ignoredSoundEvents = DEFAULT_IGNORED_SOUND_EVENTS;
	
	
	//-----------------------------------------------------------------------
	private transient boolean cached = false;
	private transient Set<String> ignoredSoundEventsSet = Collections.emptySet();
	
	private void calculateCache() {
		if (!cached) {
			// KEEP AS STRING!
			// categories don't parse when converted to type Identifier
			ignoredSoundEventsSet = ConfigHelper.parseToSet(ignoredSoundEvents, s -> s);
			
			cached = true;
		}
	}
	
	public void recalculateCache() {
		cached = false;
		calculateCache();
	}
	
	public boolean isIgnoredSoundEvent(Identifier identifier) {
		calculateCache();
		// loop through sound events, and check if the beginning matches
		// in essence: .contains, but allowing categories
		for (String ignored : ignoredSoundEventsSet)
			if (ignored.startsWith(".") || ignored.startsWith("_")) {
				if (identifier.toString().endsWith(ignored)) return true;
			} else {
				if (identifier.toString().startsWith(ignored)) return true;
			}
		return false;
		// return ignoredSoundEventsSet.contains(identifier);
	}
}
