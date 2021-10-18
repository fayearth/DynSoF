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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.andre111.dynamicsf.config.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

@Environment(EnvType.CLIENT)
public class DynamicSoundFilters implements ModInitializer {
	private static final FilterManager FILTER_MANAGER = new FilterManager();
	private static final Logger LOGGER = LogManager.getLogger();
	
	public static FilterManager getFilterManager() {
		return FILTER_MANAGER;
	}
	
	public static Logger getLogger() {
		return LOGGER;
	}

	@Override
	public void onInitialize() {
		Config.loadData();
		ClientTickEvents.END_CLIENT_TICK.register(FILTER_MANAGER::updateGlobal);
	}
}
