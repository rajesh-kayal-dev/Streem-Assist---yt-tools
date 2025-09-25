package com.YouTubeTools.Util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

@Slf4j
@UtilityClass
public class ValidationUtil {

    private static final Pattern YOUTUBE_VIDEO_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{11}$");
    private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.+$"
    );

    public static boolean isValidYouTubeVideoId(String videoId) {
        if (videoId == null) {
            return false;
        }
        return YOUTUBE_VIDEO_ID_PATTERN.matcher(videoId.trim()).matches();
    }

    public static boolean isValidYouTubeUrl(String url) {
        if (url == null) {
            return false;
        }
        return YOUTUBE_URL_PATTERN.matcher(url.trim().toLowerCase()).matches();
    }

    public static boolean isValidVideoTitle(String title) {
        return title != null && !title.trim().isEmpty() && title.trim().length() <= 100;
    }

    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return input.trim().replaceAll("[\\r\\n\\t]", " ").replaceAll("\\s+", " ");
    }

    public static void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("YOUR_YOUTUBE_API_KEY_HERE")) {
            throw new IllegalStateException("YouTube API key is not configured properly");
        }
    }
}
