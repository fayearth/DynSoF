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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.andre111.dynamicsf.filter.Reverb;
import me.andre111.dynamicsf.filter.Reverb.ReverbInfo;
import net.minecraft.util.Identifier;

public class ConfigDataReverb {
	public boolean enabled = true;
	
	public float reverbPercent = 1f;
	
	public int quality = 4;
	public boolean checkSky = true;
	
	public List<String> dimensionBaseReverb = Arrays.asList("minecraft:the_nether;1.0", "minecraft:the_nether;-0.3");
	public List<String> customBlockSolidity = new ArrayList<>();

	// Are these accurate? More research's needed.
	public float density = 0.2f;
	public float diffusion = 0.6f;
	public float gain = 0.15f;
	public float gainHF = 0.8f;
	public float minDecayTime = 0.1f;
	public float decayHFRatio = 0.7f;
	public float airAbsorptionGainHF = 0.99f;
	public float reflectionsGainBase = 0.05f;
	public float reflectionsGainMultiplier = 0.05f;
	public float reflectionsDelayMultiplier = 0.025f;
	public float lateReverbGainBase = 1.26f;
	public float lateReverbGainMultiplier = 0.1f;
	public float lateReverbDelayMultiplier = 0.01f;
	
	
	//-----------------------------------------------------------------------
	private transient boolean cached = false;
	private transient Map<Identifier, Float> dimensionBaseReverbMap = new HashMap<>();
	private transient Map<Identifier, Reverb.ReverbInfo> customBlockReverbMap = new HashMap<>();
	
	private void calculateCache() {
		if (!cached) {
			dimensionBaseReverbMap = ConfigHelper.parseToMap(dimensionBaseReverb, Identifier::new, Float::parseFloat);
			customBlockReverbMap = ConfigHelper.parseToMap(customBlockSolidity, Identifier::new, Reverb.ReverbInfo::fromName);
			
			cached = true;
		}
	}
	
	public void recalculateCache() {
		cached = false;
		calculateCache();
	}
	
	public float getDimensionBaseReverb(Identifier dimension) {
		calculateCache();
		
		if (dimensionBaseReverbMap.containsKey(dimension) ) {
			return dimensionBaseReverbMap.get(dimension);
		}
		
		return 0f;
	}
	
	public ReverbInfo getCustomBlockReverb(Identifier block) {
		calculateCache();
		
		return customBlockReverbMap.get(block);
	}

	// please, someone tell me that there's a better way to do this in java :/

	public boolean getEnabled() {
		return enabled;
	}

	public float getReverbPercent() {
		return reverbPercent;
	}

	public int getQuality() {
		return quality;
	}

	public boolean getCheckSky() {
		return checkSky;
	}

	public List<String> getDimensionBaseReverb() {
		return dimensionBaseReverb;
	}

	public List<String> getCustomBlockReverb() {
		return customBlockSolidity;
	}

	public float getDensity() {
		return density;
	}

	public float getDiffusion() {
		return diffusion;
	}

	public float getGain() {
		return gain;
	}

	public float getGainHF() {
		return gainHF;
	}

	public float getMinDecayTime() {
		return minDecayTime;
	}

	public float getDecayHFRatio() {
		return decayHFRatio;
	}

	public float getAirAbsorptionGainHF() {
		return airAbsorptionGainHF;
	}

	public float getReflectionsGainBase() {
		return reflectionsGainBase;
	}

	public float getReflectionsGainMultiplier() {
		return reflectionsGainMultiplier;
	}

	public float getReflectionsDelayMultiplier() {
		return reflectionsDelayMultiplier;
	}

	public float getLateReverbGainBase() {
		return lateReverbGainBase;
	}

	public float getLateReverbGainMultiplier() {
		return lateReverbGainMultiplier;
	}

	public float getLateReverbDelayMultiplier() {
		return lateReverbDelayMultiplier;
	}
}
