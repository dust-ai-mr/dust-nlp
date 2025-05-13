/*
 *
 *  Copyright 2024-Present Alan Littleford
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package com.mentalresonance.dust.nlp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Useful utilities for parsing out structured data from LLMs (lists)
 */
public class ChatUtils {

	/**
	 * Isolate a numerical list e.g.
	 * <pre>
	 *     1. 45
	 *     Other stuff
	 *     3. Nice
	 * </pre>
	 * Would result in [45, "Nice"]
	 * @param utterance from LLM - should be a numerical list
	 * @return  list of items in utterance
	 */
	public static List<String> numericList(String utterance) {
		List<String> items = new LinkedList<>();
		String[] lines = utterance.split("\\r?\\n");

		Pattern pattern = Pattern.compile("[0-9]+\\..*");

		for (String line : lines) {
			Matcher matcher = pattern.matcher(line);
			if (matcher.matches()) {
				int start = 1 + line.indexOf('.');

				if (start == 0) {
					start = line.indexOf(' ');
				}

				if (start > 0 && start < line.length()) {
					items.add(line.substring(start).trim());
				}
			}
		}

		// Remove any null or empty items
		items.removeIf(String::isEmpty);

		return items;
	}

	/**
	 * Isolate numerical lists separated by other stuff. For example
	 * <pre>
	 *     1. This is
	 *     2. a list
	 *     Other stuff
	 *     4. the end
	 * </pre>
	 * would result in [["this is", "a list"], ["the end"]]
	 * @param utterance from LLM
	 * @return The list
	 */
	public static List<List<String>> numericLists(String utterance) {
		List<String> items = new ArrayList<>();
		List<List<String>> lists = new ArrayList<>();
		boolean inList = false;

		String[] lines = utterance.split("\\r?\\n");

		Pattern pattern = Pattern.compile("[0-9]+\\..*");

		for (String line : lines) {
			Matcher matcher = pattern.matcher(line);
			if (matcher.matches()) {
				int start = 1 + line.indexOf('.');

				inList = true;
				if (start == 0) {
					start = line.indexOf(' ');
				}

				if (start > 0 && start < line.length()) {
					items.add(line.substring(start).trim());
				}
			} else if (inList) {
				inList = false;
				lists.add(new ArrayList<>(items));
				items.clear();
			}
		}

		// Add the last list if still inList and items is not empty
		if (inList && !items.isEmpty()) {
			lists.add(items);
		}

		return lists;
	}

	/**
	 * Parse out numeric list of integers from utterance e.g.
	 * <pre>
	 *     1.   2,3,4
	 *     2.   90, 100
	 * </pre>
	 * would result in [[1,2,3,4], [2,90, 100]]
	 * @param utterance  list of lines containing integers
	 * @return List of lists corresponding to collected integers
	 */
	public static List<List<Integer>> numericListOfIntegers(String utterance) {
		List<List<Integer>> items = new ArrayList<>();

		// Split the input string into lines
		String[] lines = utterance.split("\\r?\\n");

		// Pattern to match lines starting with numbers
		Pattern linePattern = Pattern.compile("[0-9]+.*");
		Pattern numberPattern = Pattern.compile("\\d+");

		for (String line : lines) {
			Matcher lineMatcher = linePattern.matcher(line);
			if (lineMatcher.matches()) {
				Matcher numberMatcher = numberPattern.matcher(line);
				List<Integer> numbers = new ArrayList<>();
				while (numberMatcher.find()) {
					numbers.add(Integer.parseInt(numberMatcher.group()));
				}
				items.add(numbers);
			}
		}

		return items;
	}

	/**
	 * Turn a list into a stringified numeric list. e.g.
	 * <pre>
	 *     ["a", "b", "c"]  becomes
	 *
	 *     1. a
	 *     2. b
	 *     3. c
	 * </pre>
	 *
	 * @param list to stringify
	 * @return numeric list
	 */
	public static String toNumericList(List<String> list) {
		StringBuilder result = new StringBuilder();
		int size = list.size();

		for (int i = 0; i < size; i++) {
			result.append(i + 1).append(". ").append(list.get(i));
			if (i < size - 1) {
				result.append("\n");
			}
		}

		return result.toString();
	}
}
