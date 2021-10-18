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

// this' how you format things (One cannot read parenthesis, and if statements
// properly w/out this)
// find (regex): ^(\s*([^/])(\2|\*).+)(if|\))\s*(\(|\))\s*
// replace (regex-replace): $1$4 $5
// if you don't understand it, don't worry, test it out...
// there's this one too:
// ^(\s*[^/]+\(.+\))([^\s;()\\,+.])
// $1 $2
// however, this one's dangerous (mostly to the gradle build files) and should
// be used cautiously

/**
 * Utils
 */
public class Utils {


    // clamping between two numbers was done soo often, I figured I'd abstract it.
    /**
     * Clamps the given value between 0 & 1
     */
    public static double clamp (final double value, final double ... range) {
        final double min = range.length > 0 ?  range[0] : 0;
        final double max = range.length >= 2 ? range[1] : 1;
        return Math.min(max, Math.max(value, min) );
    }
    /**
     * Clamps the given value between 0 & 1
     */
    public static float clamp (final float value, final float ... range) {
        final float min = range.length > 0 ?  range[0] : 0;
        final float max = range.length >= 2 ? range[1] : 1;
        return Math.min(max, Math.max(value, min) );
    }
    /**
     * Clamps the given value between 0 & 1
     */
    public static int clamp (final int value, final int ... range) {
        final int min = range.length > 0 ?  range[0] : 0;
        final int max = range.length >= 2 ? range[1] : 1;
        return Math.min(max, Math.max(value, min) );
    }
    /**
     * Clamps the given value between 0 & 1
     */
    public static long clamp (final long value, final long ... range) {
        final long min = range.length > 0 ?  range[0] : 0;
        final long max = range.length >= 2 ? range[1] : 1;
        return Math.min(max, Math.max(value, min) );
    }
}
