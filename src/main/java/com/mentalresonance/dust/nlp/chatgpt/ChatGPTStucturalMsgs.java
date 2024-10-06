/*
 * Copyright 2024 Alan Littleford
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
 */

package com.mentalresonance.dust.nlp.chatgpt;

import java.util.List;

/**
 * The following messages are very generic but useful in using CHATGpt as a 'semantic processor' when
 * interacting with user input of some kind.
 * All of these have been tested on ChatGpt 3.5
 */
public class ChatGPTStucturalMsgs {
    /**
     * Constructor
     */
    public ChatGPTStucturalMsgs() {}

    /**
     * Useful prompt to select item from a list
     */
    public static class GPTItemFromListMsg extends ChatGptRequestResponseMsg {

        /**
         * Interacting with a user to prompt them to make a selection. The list is usually numeric.
         * The prompt will return the matching item
         * @param list numeric list
         * @param item putative item in the list
         */
        public GPTItemFromListMsg(String list, String item) {
            super("""
				Be sure to follow my request exactly,
				Below is a list of items and a user response.
				Your goal is to return an item from the list If the response mentions it otherwise say No.
				Be sure to return the name of the matching item. Do not say 'Yes' and
				do not return the user response: return the list entry.
				List:
				%s
				Response: %s
			""".formatted(list, item));
        }
    }

    /**
     * Match user input with its 'intention'. This prompt works best with an Alphabetic list - for some reason
     * ChatGPT keeps getting hung up if this is a numeric list.
     * The prompt will return the alphabetic index of the matching entry.
     */
    public static class GPTCategorizeTextMsg extends ChatGptRequestResponseMsg {
        /**
         * Constructor
         * @param alphaList alphabetic list of intentions
         * @param text the text
         */
        public GPTCategorizeTextMsg(String alphaList, String text) {
            super("""
				Characterize the following text as one of the following. Return the matching letter only.
				%s
				Text: %s
			""".formatted(alphaList, text));
        }
    }

    /**
     * Convert list to numeric list
     * @param list list of String entries
     * @return String representation of list as a numeric list starting at 1.
     */
    public static String listToNumericList(List<String> list) {
        int i = 1;
        StringBuilder sb = new StringBuilder();

        for (String entry : list) {
            sb.append("%d. %s\n".formatted(i++, entry));
        }
        return sb.toString();
    }

    /**
     * Covert list to alphabetic list
     * @param list list of String entries
     * @return String representation of list as an alphabetic list starting at a)
     */
    public static String listToAlphabeticList(List<String> list) {
        int i = 97; // a
        StringBuilder sb = new StringBuilder();

        for (String entry : list) {
            sb.append("%c) %s\n".formatted(i++, entry));
        }
        return sb.toString();
    }
}
