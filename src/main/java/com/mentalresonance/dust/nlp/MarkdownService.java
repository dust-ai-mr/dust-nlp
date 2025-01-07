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

package com.mentalresonance.dust.nlp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownService {

    public static String convert(String markdown) {

        // Convert Headers
        markdown = convertHeaders(markdown);

        // Convert bold text
        markdown = convertBold(markdown);

        // Convert italic text
        markdown = convertItalic(markdown);

        // Convert links
        markdown = convertLinks(markdown);

        // Convert unordered lists
        markdown = convertUnorderedLists(markdown);

        markdown = markdown.replaceAll("\n", "<br>");

        return markdown;
    }

    private static String convertHeaders(String markdown) {
        for (int i = 6; i >= 1; i--) {
            String regex = "#".repeat(i) + "(.*)$" + "$";
            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(markdown);
            markdown = matcher.replaceAll("<h" + i + ">$1</h" + i + ">");
        }

        return markdown;
    }

    private static String convertBold(String markdown) {
        return markdown.replaceAll("\\*\\*(.*?)\\*\\*",
                        "<strong>$1</strong>")
                .replaceAll("__(.*?)__", "<strong>$1</strong>");
    }

    private static String convertItalic(String markdown) {
        return markdown.replaceAll("\\*(.*?)\\*", "<em>$1</em>")
                .replaceAll("_(.*?)_", "<em>$1</em>");
    }

    private static String convertLinks(String markdown) {
        return markdown.replaceAll("!\\[(.*?)\\]\\((.*?)\\)", "<img src='$2' alt='$1'/>")
                .replaceAll("\\[(.*?)\\]\\((.*?)\\)", "<a href='$2'>$1</a>");
    }

    private static String convertUnorderedLists(String markdown) {
        StringBuilder html = new StringBuilder();
        Pattern listItemPattern = Pattern.compile("^\\* (.*)$",
                Pattern.MULTILINE);

        Matcher matcher = listItemPattern.matcher(markdown);
        boolean inList = false;

        while (matcher.find()) {
            if (!inList) {
                html.append("<ul>");
                inList = true;
            }

            String itemContent = matcher.group(1);
            html.append("<li>").append(itemContent).append("</li>");
        }

        if (inList) {
            html.append("</ul>");
        }

        // Append any remaining text outside of lists
        Matcher fullMatch = listItemPattern.matcher(markdown);
        if (!fullMatch.find()) {
            html.append(markdown.replaceAll("^\\* (.*)$", "$1"));
        } else {
            String remainder = markdown.substring(fullMatch.end());
            html.append(remainder);
        }

        return html.toString();
    }
}
