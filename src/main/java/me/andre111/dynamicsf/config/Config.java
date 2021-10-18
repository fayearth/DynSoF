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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;

public class Config {
	private static Gson gson = new Gson();
	private static ConfigData data = new ConfigData();
	private static File file = new File("./config/dynSFX/1.5.0.json");
	
	public static ConfigData getData() {
		return data;
	}
	
	public static void loadData() {
		if (file.exists() ) {
			try ( BufferedReader reader = new BufferedReader(new FileReader(file) )) {
				data = gson.fromJson(reader, ConfigData.class);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void saveData() {
		if (!file.exists() ) {
			if (!file.getParentFile().exists() ) {
				file.getParentFile().mkdirs();
			}
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(file) )) {
			gson.toJson(data, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

