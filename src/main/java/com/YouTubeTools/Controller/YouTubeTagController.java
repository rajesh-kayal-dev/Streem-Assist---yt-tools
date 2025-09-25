package com.YouTubeTools.Controller;

import com.YouTubeTools.Model.SearchVideo;
import com.YouTubeTools.Service.YouTubeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/youtube-tag")
@RequiredArgsConstructor
public class YouTubeTagController {

    private final YouTubeService youTubeService;

    @Value("${youtube.api.key}")
    private String apiKey;

    @PostMapping("/search")
    public ResponseEntity<?> searchVideoTags(@RequestBody Map<String, String> request) {
        try {
            String videoTitle = request.get("videoTitle");

            if (!isApiKeyConfigured()) {
                log.error("YouTube API key is not configured");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "YouTube API key is not configured"));
            }

            if (videoTitle == null || videoTitle.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Video title is required"));
            }

            SearchVideo result = youTubeService.searchVideos(videoTitle.trim());

            if (result.getPrimaryVideo() == null &&
                    (result.getRelatedVideos() == null || result.getRelatedVideos().isEmpty())) {
                return ResponseEntity.ok(Map.of(
                        "message", "No videos found with tags for this title",
                        "primaryVideo", (Object) null,
                        "relatedVideos", result.getRelatedVideos()
                ));
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error searching for video tags: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to search for video tags: " + e.getMessage()));
        }
    }

    private boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("YOUR_YOUTUBE_API_KEY_HERE");
    }
}
