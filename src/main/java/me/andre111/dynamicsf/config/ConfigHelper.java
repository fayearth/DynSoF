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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import me.andre111.dynamicsf.DynamicSoundFilters;

public class ConfigHelper {
	public static <K, V> Map<K, V> parseToMap(List<String> entries, Function<String, K> keyParser, Function<String, V> valueParser) {
		Map<K, V> map = new HashMap<>();
		
		if (entries != null) {
			for(String entry : entries) {
				try {
					if (entry == null) throw new RuntimeException();
					String[] split = entry.split(";");
					if (split.length != 2) throw new RuntimeException();
					
					K key = keyParser.apply(split[0]);
					V value = valueParser.apply(split[1]);
					
					map.put(key, value);
				} catch(RuntimeException e) {
					DynamicSoundFilters.getLogger().error("Ignoring broken config entry: "+entry, e);
				}
			}
		}
		
		return map;
	}
	
	public static <K> Set<K> parseToSet(List<String> entries, Function<String, K> keyParser) {
		Set<K> set = Collections.newSetFromMap(new HashMap<>() );
		
		if (entries != null) {
			for(String entry : entries) {
				try {
					if (entry == null) throw new RuntimeException();
					
					K key = keyParser.apply(entry);
					
					set.add(key);
				} catch(RuntimeException e) {
					DynamicSoundFilters.getLogger().error("Ignoring broken config entry: "+entry, e);
				}
			}
		}
		
		return set;
	}
}
