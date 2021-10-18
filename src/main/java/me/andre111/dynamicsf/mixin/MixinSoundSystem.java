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
package me.andre111.dynamicsf.mixin;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import me.andre111.dynamicsf.filter.Reverb;

import me.andre111.dynamicsf.DynamicSoundFilters;
import me.andre111.dynamicsf.interfaces.SourceWithID;
import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

@Mixin(SoundSystem.class)
@SuppressWarnings("rawtypes")
public class MixinSoundSystem {

	// not needed! Causes MANY issues with echo filter

	// DEV NOTE: use LocalCapture.PRINT to check for changed locals when update
	// breaks
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/Channel$SourceManager;run(Ljava/util/function/Consumer;)V", ordinal = 0), method = "Lnet/minecraft/client/sound/SoundSystem;play(Lnet/minecraft/client/sound/SoundInstance;)V", locals = LocalCapture.CAPTURE_FAILHARD)
	public void onPlay(final SoundInstance soundInstance, CallbackInfo ci, WeightedSoundSet weightedSoundSet,
			Identifier identifier, Sound sound, float f, float g, SoundCategory soundCategory, float h, float i,
			SoundInstance.AttenuationType attenuationType, boolean bl, Vec3d vec3d, boolean bl3, boolean bl4,
			CompletableFuture completableFuture, Channel.SourceManager sourceManager) {
		sourceManager.run(source -> {
			SourceWithID sourceWithID = (SourceWithID) source;

			DynamicSoundFilters.getFilterManager().updateSoundInstance(soundInstance, sourceWithID.getID() );
		});
	}
	// fix issue #4 - (resource packs)
	@Inject(method = "reloadSounds", at = @At("TAIL"))
		public void dsf$reloadSounds(CallbackInfo ci) {
			Reverb.reinit();
	}	
}
