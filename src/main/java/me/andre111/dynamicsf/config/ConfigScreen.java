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
import java.util.List;
import java.util.function.Consumer;
//import java.util.Arrays;
import java.util.stream.Collectors;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.text.TranslatableText;

// should probably abstract this stuff, eh, later!
// use this:
// import java.util.function.Function;

public class ConfigScreen implements ModMenuApi {

	/*
	 * helper functions
	 */

	// top-level category
	private ConfigCategory category(ConfigBuilder builder, String key) {
			return builder.getOrCreateCategory(new TranslatableText(key));
	}

	// sub-level category override
	private SubCategoryBuilder category(ConfigEntryBuilder entryBuilder, String key) {
			return entryBuilder.startSubCategory(new TranslatableText(key));
	}

	// generate tooltip keys via dynamic arguments
	private TranslatableText[] generateTooltip(String key, String... tooltips) {
		if (tooltips.length == 0) tooltips = new String[] { key + ".tooltip" };
		final List<TranslatableText> tooltipList = Arrays.asList(tooltips).stream()
			.map(tip -> new TranslatableText(tip))
			.collect(Collectors.toList());
		return tooltipList.toArray(new TranslatableText[tooltipList.size()]);
	}
	
	// form accepting List<String>
	private void listf(
		Consumer<AbstractConfigListEntry> method, ConfigEntryBuilder entryBuilder,
		final String key, final List<String> value,
		final List<String> defaults,
		Consumer<List<String>> consumer,
		String... tooltips
	) {
		method.accept
			(entryBuilder
			.startStrList(new TranslatableText(key), value)
			.setTooltip(generateTooltip(key, tooltips))
			.setDefaultValue(defaults)
			.setSaveConsumer(consumer)
			.build()
			);
	}
	
	// form accepting boolean
	private void boolf(
		Consumer<AbstractConfigListEntry> method, ConfigEntryBuilder entryBuilder,
		final String key, final Boolean value,
		final Boolean defaults,
		Consumer<Boolean> consumer,
		String... tooltips
	) {
		method.accept
			(entryBuilder
			.startBooleanToggle(new TranslatableText(key), value)
			.setTooltip(generateTooltip(key, tooltips))
			.setDefaultValue(defaults)
			.setSaveConsumer(consumer)
			.build()
			);
	}
	
	// form accepting float
	private void floatf(
		Consumer<AbstractConfigListEntry> method, ConfigEntryBuilder entryBuilder,
		final String key, final float value,
		final float defaults,
		final float min, final float max,
		Consumer<Float> consumer,
		String... tooltips
	) {
		method.accept
			(entryBuilder
			.startFloatField(new TranslatableText(key), value)
			.setTooltip(generateTooltip(key, tooltips))
			.setDefaultValue(defaults)
			.setMin(min).setMax(max)
			.setSaveConsumer(consumer)
			.build()
			);
	}
	
	// form accepting int
	private void intf(
		Consumer<AbstractConfigListEntry> method, ConfigEntryBuilder entryBuilder,
		final String key, final int value,
		final int defaults,
		final int min, final int max,
		Consumer<Integer> consumer,
		String... tooltips
	) {
		method.accept
			(entryBuilder
			.startIntField(new TranslatableText(key), value)
			.setTooltip(generateTooltip(key, tooltips))
			.setDefaultValue(defaults)
			.setMin(min).setMax(max)
			.setSaveConsumer(consumer)
			.build()
			);
	}

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		ConfigData data = Config.getData();
		ConfigDataLiquid liquid = new ConfigDataLiquid();
		ConfigDataObstruction obstruction = new ConfigDataObstruction();
		ConfigDataReverb reverb = new ConfigDataReverb();
		ConfigDataEcho echo = new ConfigDataEcho();

		return parent -> {
			ConfigBuilder builder = ConfigBuilder.create();
			builder.setParentScreen(parent);
			builder.setTitle(new TranslatableText("dynamicsoundfilters.config.title") );
			ConfigEntryBuilder entryBuilder = builder.entryBuilder();

			builder.setSavingRunnable(Config::saveData);

			/*
			 * general settings
			 */
			ConfigCategory general = category(builder, "dynamicsoundfilters.config.general"); {
			listf(general::addEntry, entryBuilder,
				"dynamicsoundfilters.config.general.ignored",
				data.general.ignoredSoundEvents, ConfigDataGeneral.DEFAULT_IGNORED_SOUND_EVENTS,
				l -> {
					data.general.ignoredSoundEvents = l;
					data.general.recalculateCache();
				});
			}

			/*
			 * liquid filter settings
			 */
			ConfigCategory liquidFilter = category(builder, "dynamicsoundfilters.config.liquid"); {
				boolf(liquidFilter::addEntry, entryBuilder,
					"dynamicsoundfilters.config.liquid.enable", data.liquidFilter.enabled,
					liquid.getEnabled(), b -> data.liquidFilter.enabled = b);

				/*
				 *
				 * WATER settings
				 * 
				 */
				SubCategoryBuilder water = category(entryBuilder, "dynamicsoundfilters.config.liquid.water"); {
					// water gain
					floatf(water::add, entryBuilder,
						"dynamicsoundfilters.config.liquid.gain", data.liquidFilter.waterGain,
						liquid.getWaterGain(), 0f, 1f,
						f -> data.liquidFilter.waterGain = f);
					// water gainHF
					floatf(water::add, entryBuilder,
						"dynamicsoundfilters.config.liquid.gainhf", data.liquidFilter.waterGainHF,
						liquid.getWaterGainHF(), 0f, 1f,
						f -> data.liquidFilter.waterGainHF = f);
				}
				liquidFilter.addEntry(water.build() );

				/*
				 *
				 * LAVA settings
				 * 
				 */
				SubCategoryBuilder lava = category(entryBuilder, "dynamicsoundfilters.config.liquid.lava"); {
					// lava gainHF
					floatf(lava::add, entryBuilder,
						"dynamicsoundfilters.config.liquid.gain", data.liquidFilter.lavaGain,
						liquid.getLavaGain(), 0f, 1f,
						f -> data.liquidFilter.lavaGain = f);
					// lava gainHF
					floatf(lava::add, entryBuilder,
						"dynamicsoundfilters.config.liquid.gainhf", data.liquidFilter.lavaGainHF,
						liquid.getLavaGainHF(), 0f, 1f,
						f -> data.liquidFilter.lavaGainHF = f);
				}
				liquidFilter.addEntry(lava.build() );
			}

			/*
			 * obstruction filter settings
			*/
			ConfigCategory obstructionFilter = category(builder, "dynamicsoundfilters.config.obstruction"); {
				boolf(obstructionFilter::addEntry, entryBuilder,
					"dynamicsoundfilters.config.obstruction.enable", data.obstructionFilter.enabled,
					reverb.getEnabled(), b -> data.obstructionFilter.enabled = b);

				floatf(obstructionFilter::addEntry, entryBuilder,
					"dynamicsoundfilters.config.obstruction.step", data.obstructionFilter.obstructionStep,
						obstruction.getObstructionStep(), 0f, 1f,
						f -> data.obstructionFilter.obstructionStep = f);

				floatf(obstructionFilter::addEntry, entryBuilder,
					"dynamicsoundfilters.config.obstruction.max", data.obstructionFilter.obstructionMax,
						obstruction.getObstructionMax(), 0f, 1f,
						f -> data.obstructionFilter.obstructionMax = f);
			}

			/*
			 * echo filter settings
			*/
			ConfigCategory echoFilter = category(builder, "dynamicsoundfilters.config.echo"); {
				boolf(echoFilter::addEntry, entryBuilder,
					"dynamicsoundfilters.config.echo.enable", data.echoFilter.enabled,
					reverb.getEnabled(), b -> data.echoFilter.enabled = b);

				boolf(echoFilter::addEntry, entryBuilder,
					"dynamicsoundfilters.config.echo.doaverage", data.echoFilter.doAverage,
						echo.getDoAverage(),
						b -> data.echoFilter.doAverage = b);
			}

			/*
			 * reverb filter settings
			 */
			ConfigCategory reverbFilter = category(builder, "dynamicsoundfilters.config.reverb"); {
				/*
				 * reverb basic settings
				 */
				boolf(reverbFilter::addEntry, entryBuilder,
					"dynamicsoundfilters.config.reverb.enable", data.reverbFilter.enabled,
					reverb.getEnabled(), b -> data.reverbFilter.enabled = b);
				
				floatf(reverbFilter::addEntry, entryBuilder,
					"dynamicsoundfilters.config.reverb.percent", data.reverbFilter.reverbPercent,
					reverb.getReverbPercent(), 0f, 2f,
					f -> data.reverbFilter.reverbPercent = f);
				
				listf(reverbFilter::addEntry, entryBuilder,
					"dynamicsoundfilters.config.reverb.dimensions", data.reverbFilter.dimensionBaseReverb,
					reverb.getDimensionBaseReverb(),
					l -> {
						data.reverbFilter.dimensionBaseReverb = l;
						data.reverbFilter.recalculateCache();
					},
					"dynamicsoundfilters.config.reverb.dimensions.tooltip1",
					"dynamicsoundfilters.config.reverb.dimensions.tooltip2",
					"dynamicsoundfilters.config.reverb.dimensions.tooltip3",
					"dynamicsoundfilters.config.reverb.dimensions.tooltip4");

				listf(reverbFilter::addEntry, entryBuilder,
					"dynamicsoundfilters.config.reverb.blocks", data.reverbFilter.customBlockSolidity,
					reverb.getCustomBlockReverb(),
					l -> {
						data.reverbFilter.customBlockSolidity = l;
						data.reverbFilter.recalculateCache();
					},
					"dynamicsoundfilters.config.reverb.blocks.tooltip1",
					"dynamicsoundfilters.config.reverb.blocks.tooltip2",
					"dynamicsoundfilters.config.reverb.blocks.tooltip3");

				/*
				 * reverb scanner settings
				 */
				SubCategoryBuilder scanner = category(entryBuilder, "dynamicsoundfilters.config.reverb.scanner")
					.setTooltip(new TranslatableText("dynamicsoundfilters.config.reverb.scanner.tooltip") ); {
				
					intf(scanner::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.scanner.quality", data.reverbFilter.quality,
						reverb.getQuality(), 1, 10,
						i -> data.reverbFilter.quality = i);
				
					boolf(scanner::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.scanner.checksky", data.reverbFilter.checkSky,
						reverb.getCheckSky(),
						b -> data.reverbFilter.checkSky = b);
				}
				reverbFilter.addEntry(scanner.build() );

				/*
				 * reverb advanced settings
				 */
				SubCategoryBuilder advanced = category(entryBuilder, "dynamicsoundfilters.config.reverb.advanced")
					.setTooltip(new TranslatableText("dynamicsoundfilters.config.reverb.advanced.tooltip") ); {
				
					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.density", data.reverbFilter.density,
						reverb.getDensity(), 0f, 1f,
						f -> data.reverbFilter.density = f);
				
					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.diffusion", data.reverbFilter.diffusion,
						reverb.getDiffusion(), 0f, 1f,
						f -> data.reverbFilter.diffusion = f);
				
					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.gain", data.reverbFilter.gain,
						reverb.getGain(), 0f, 1f,
						f -> data.reverbFilter.gain = f);
				
					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.gainhf", data.reverbFilter.gainHF,
						reverb.getGainHF(), 0f, 1f,
						f -> data.reverbFilter.gainHF = f);
				
					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.mindecaytime", data.reverbFilter.minDecayTime,
						reverb.getMinDecayTime(), 0.1f, 20f,
						f -> data.reverbFilter.minDecayTime = f);
				
					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.decayhfratio", data.reverbFilter.decayHFRatio,
						reverb.getDecayHFRatio(), 0.1f, 20f,
						f -> data.reverbFilter.decayHFRatio = f);
				
					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.airabsorptiongainhf", data.reverbFilter.airAbsorptionGainHF,
						reverb.getAirAbsorptionGainHF(), 0.892f, 1f,
						f -> data.reverbFilter.airAbsorptionGainHF = f);
				
					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.reflectionsgainbase", data.reverbFilter.reflectionsGainBase,
						reverb.getReflectionsGainBase(), 0f, 1.58f,
						f -> data.reverbFilter.reflectionsGainBase = f);

					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.reflectionsgainmultiplier", data.reverbFilter.reflectionsGainMultiplier,
						reverb.getReflectionsGainMultiplier(), 0f, 1.58f,
						f -> data.reverbFilter.reflectionsGainMultiplier = f);
					
					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.reflectionsdelaymultiplier", data.reverbFilter.reflectionsDelayMultiplier,
							reverb.getReflectionsDelayMultiplier(), 0f, 0.3f,
							f -> data.reverbFilter.reflectionsDelayMultiplier = f);

					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.latereverbgainbase", data.reverbFilter.lateReverbGainBase,
						reverb.getLateReverbGainBase(), 0f, 5f,
						f -> data.reverbFilter.lateReverbGainBase = f);
					
					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.latereverbgainmultiplier", data.reverbFilter.lateReverbGainMultiplier,
							reverb.getLateReverbGainMultiplier(), 0f, 5f,
							f -> data.reverbFilter.lateReverbGainMultiplier = f);

					floatf(advanced::add, entryBuilder,
						"dynamicsoundfilters.config.reverb.advanced.latereverbdelaymultiplier", data.reverbFilter.lateReverbDelayMultiplier,
							reverb.getLateReverbDelayMultiplier(), 0f, 0.1f,
							f -> data.reverbFilter.lateReverbDelayMultiplier = f);
				}
				reverbFilter.addEntry(advanced.build() );
			}

			return builder.build();
		};
	}
}
