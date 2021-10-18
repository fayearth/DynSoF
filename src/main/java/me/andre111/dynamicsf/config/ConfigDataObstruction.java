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

public class ConfigDataObstruction {
	public boolean enabled = true;
	
	public float obstructionStep = 0.1f;
	public float obstructionMax = 0.98f;

	public boolean getEnabled() {
		return enabled;
	}

	public float getObstructionStep() {
		return obstructionStep;
	}

	public float getObstructionMax() {
		return obstructionMax;
	}
}
