package org.codeforcompassion.animalwelfare.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SourceExtractorUtil {

    public static String extractSource(String feedUrl, String summaryHtml, String articleUrl) {
        String source = null;

        // 1. Try extracting from feed URL (site:domain.com)
        Matcher matcher = Pattern.compile("site:([\\w.-]+)").matcher(feedUrl);
        if (matcher.find()) {
            source = matcher.group(1).replace("www.", "");
        }

        // 2. Try from <font> tag in summary HTML
        if (isEmpty(source)) {
            try {
                Document doc = Jsoup.parse(summaryHtml);
                source = doc.select("font").text();
            } catch (Exception ignored) {}
        }

        // 3. Fallback to domain of article URL
        if (isEmpty(source)) {
            try {
                String domain = new URL(articleUrl).getHost();
                source = domain.startsWith("www.") ? domain.substring(4) : domain;
            } catch (Exception ignored) {
                source = "Unknown";
            }
        }

        return source != null ? source.trim() : "Unknown";
    }

    private static boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }
}
