package com.celeblingo.helper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String extractUrl(String inputHtml, String preText, String defaultUrl) {
        String urlPattern = "https?://[^\\s\"']+";
        // Find the specific part of the string first
        int index = inputHtml.indexOf(preText);
        if (index == -1) {
            return defaultUrl; // Pretext not found
        }
        String subString = inputHtml.substring(index);

        // Adjusted pattern to stop at quotes
        Pattern pattern = Pattern.compile(urlPattern);
        Matcher matcher = pattern.matcher(subString);
        if (matcher.find()) {
            // Extract URL, ensuring to trim at the end quote if present
            String url = matcher.group();
            int endIndex = url.indexOf("\"");
            if (endIndex != -1) {
                url = url.substring(0, endIndex);
            }
            return url;
        }
        return defaultUrl; // URL pattern not found
    }
}
