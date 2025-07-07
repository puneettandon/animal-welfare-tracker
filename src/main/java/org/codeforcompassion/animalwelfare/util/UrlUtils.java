package org.codeforcompassion.animalwelfare.util;

import java.net.URI;
import java.net.URISyntaxException;

public class UrlUtils {

    public static String normalize(String url) {
        try {
            URI uri = new URI(url);
            // Remove query params and fragments
            return new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    null,  // no query
                    null   // no fragment
            ).toString();
        } catch (URISyntaxException e) {
            return url; // fallback to original if parsing fails
        }
    }
}
