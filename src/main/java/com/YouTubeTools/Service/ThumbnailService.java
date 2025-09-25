package com.YouTubeTools.Service;

import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ThumbnailService {

    public String extractVideoId(String videoUrlOrId) {
        if (videoUrlOrId == null || videoUrlOrId.trim().isEmpty()) {
            return null;
        }

        // If it's already a video ID (11 characters, no special characters)
        if (videoUrlOrId.matches("^[a-zA-Z0-9_-]{11}$")) {
            return videoUrlOrId;
        }

        // Extract from various YouTube URL formats
        String[] patterns = {
                "youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})",
                "youtu\\.be/([a-zA-Z0-9_-]{11})",
                "youtube\\.com/embed/([a-zA-Z0-9_-]{11})",
                "youtube\\.com/v/([a-zA-Z0-9_-]{11})"
        };

        for (String pattern : patterns) {
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(videoUrlOrId);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }
}