/*
 *
 *  Copyright 2024-2025 Alan Littleford
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

package com.mentalresonance.dust.nlp.lang;

import com.ibm.icu.text.BreakIterator;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Fluent text processing. Typically this is for short pieces of text - e.g. named entities.
 */
public class Words {
    
    String text;

	Words(String text) {
        this.text = text;
    }

    /**
     *  Punctuation causes problems. We want to remove trailing punctuation but leave embedded punctuation
     *  (e.g. map  foo:  but keep http://foo/bar)
     *  Also remove possessives while we are at it and look out for apostrophe/leftquote/rightquote hell.
     *  Finally replace multiple spaces by one.
     */
    Words dePunctuate() {
        return new Words(dePunctuate(text));
    }

    /**
     * Remove all punctuation from text
     * @param text to depunctuate
     * @return depunctuated
     */
    public static String dePunctuate(String text) {
        if (null != text) {
            text = text
                    .trim()
                    .replaceAll("\\. |, |: |\\?|\\.\\$|,\\$|:\\$|;|'s|\\u2018s|\\u2019s|\"", " ")
                    .replaceAll("\\x20+", " ")
                    .trim();
        }
        return text;
    }

    /**
     * Remove all punctuation.
     * @return Words with depunctuated text
     */
    public Words dePunctuateAll() {
        return new Words(
            text
                .trim()
                .replaceAll("\\.|,|:|;|'s|\\?\\u2018s|\\u2019s|\"", " ")
                .replaceAll("\\x20+", " ")
                .trim()
        );
    }

    /**
     * Remove all spaces
     * @return Words despaced
     */
    public Words deSpace() {
        return new Words(
            text
                .trim()
                .replaceAll("\\x20", "")
        );
    }

    /**
     * Remove special characters '-', '&'
     * @return Words - deSpecialed
     */
    Words deSpecialChar() {
        return new Words(
            text
                .replaceAll("\\u002d|\\u0026", " ")
                .replaceAll("\\x20+", " ")
                .trim()
        );
    }

    /**
     * Words lower case
     * @return Words lower cased
     */
    Words lowerCase() { return new Words(text.toLowerCase()); }

    /**
     * If any word is all CAPS leave it as such, else lower case it
     * @return Words as described
     */
    public Words carefulToLowerCase() {

        return new Words(
                Arrays.stream(text
                    .split(" "))
                    .map(t -> allUpper(t) ? t : t.toLowerCase())
                    .collect(Collectors.joining(" ")
                )
        );
    }

    /**
     * Take the text and split it into sentences.
     * @param text the text
     * @param locale the local
     * @return list of sentences
     */
    public static LinkedList<String> sentences(String text, Locale locale) {
        LinkedList<String> sentences = new LinkedList<>();

        BreakIterator breakIterator = BreakIterator.getSentenceInstance(locale);
        breakIterator.setText(text);

        int start = breakIterator.first();
        for (int end = breakIterator.next(); end != BreakIterator.DONE; start = end, end = breakIterator.next()) {
            sentences.add(text.substring(start, end));
        }
        return sentences;
    }

    @Override
    public String toString() { return text; }

    /**
     * Check for all upper case
     * @param input text
     * @return true if all upper case
     */
    static boolean allUpper(String input) {
        return input.chars().noneMatch(Character::isLowerCase);
    }
    /**
     * Check for all lower case
     * @param input text
     * @return true if all lower case
     */
    static boolean allLower(String input) {
        return input.chars().noneMatch(Character::isUpperCase);
    }
}
