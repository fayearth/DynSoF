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
package me.andre111.dynamicsf;

import org.lwjgl.openal.AL11;
import org.lwjgl.openal.EXTEfx;

import me.andre111.dynamicsf.config.Config;
import me.andre111.dynamicsf.config.ConfigData;
import me.andre111.dynamicsf.filter.Obstruction;
import me.andre111.dynamicsf.filter.Reverb;
import me.andre111.dynamicsf.filter.Echo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class FilterManager {
	private ConfigData data = Config.getData();
	private Vec3d clientPos = new Vec3d(0,0,0);
	private boolean verdict = false;
	private boolean update = false;

	public void updateGlobal(final MinecraftClient client) {
		// 50ms inaccuraccy at worst + only ran 1/2 as often
		update = !update;
		// update = true;
		// To recalculate, or not to recalculate.
		// That, is the boolean condition.
		verdict = !(client.world == null || client.player == null) && client.isRunning();

		// if worth updating, then do so
		if (update && verdict) {
			// get player's head/ears position
			clientPos = client.player.getPos().add(0, client.player.getEyeHeight(client.player.getPose() ), 0);
			// get config states
			data = Config.getData();
			
			// echo's special :)
			Echo.update(client);
		}

		Reverb.updateGlobal(verdict, client, data, clientPos);
		Obstruction.updateGlobal(verdict, client, data, clientPos);
	}
	
	public void updateSoundInstance(final SoundInstance soundInstance, final int sourceID) {
		// cancel if not needed
		if (data.general.isIgnoredSoundEvent(soundInstance.getId() )) return;

		// whether each effect grouping should be applied
		final boolean reverberate = Reverb.updateSoundInstance(soundInstance/*, data*/);
		final boolean obstructed = Obstruction.updateSoundInstance(soundInstance, data);
		Echo.updateSoundInstance(soundInstance/*, data*/);

		// only evaluate obstruction and reverb once!
		// since ran whence an err', worry merely of branches
		final int obstructionID = obstructed ? Obstruction.getID() : 0;
		final int reverbSlot = reverberate ? Reverb.getSlot() : 0;
		
		// apply effects
		for (int i = 0; i < 2; i++) {
			// EXTEfx.AL_FILTER_BANDPASS EXTEfx.AL_EFFECTSLOT_GAIN EXTEfx.AL_EQUALIZER_HIGH_GAIN

			AL11.alSourcei(sourceID, EXTEfx.AL_DIRECT_FILTER, obstructionID);
			AL11.alSource3i(sourceID, EXTEfx.AL_AUXILIARY_SEND_FILTER, reverbSlot, 1, obstructionID);

			// if error: log it and try once more, otherwise: end loop
			final int error = AL11.alGetError();
			if (error == AL11.AL_NO_ERROR) break;
			else System.err.println("OpenAL error: "+error);
		}
	}
}
