package com.YouTubeTools.Controller;

import com.YouTubeTools.Model.VideoDetails;
import com.YouTubeTools.Service.ThumbnailService;
import com.YouTubeTools.Service.YouTubeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class YouTubeVideoController {

    private final YouTubeService youTubeService;
    private final ThumbnailService thumbnailService;

    // Thymeleaf form handling
    @PostMapping("/youtube/video-details")
    public String fetchVideoDetailsForm(@RequestParam("videoUrlOrId") String videoUrlOrId, Model model) {
        try {
            String videoId = thumbnailService.extractVideoId(videoUrlOrId);

            if (videoId == null) {
                model.addAttribute("error", "Invalid YouTube URL or ID. Please check and try again.");
                model.addAttribute("videoUrlOrId", videoUrlOrId);
                return "video-details";
            }

            VideoDetails details = youTubeService.getVideoDetails(videoId);

            if (details == null) {
                model.addAttribute("error", "Video not found. Please check the URL or try again later.");
            } else {
                model.addAttribute("videoDetails", details);
            }

            model.addAttribute("videoUrlOrId", videoUrlOrId);
            return "video-details";

        } catch (Exception e) {
            log.error("Error fetching video details: {}", e.getMessage(), e);
            model.addAttribute("error", "An error occurred while fetching video details: " + e.getMessage());
            model.addAttribute("videoUrlOrId", videoUrlOrId);
            return "video-details";
        }
    }

    // REST API endpoint for AJAX calls
    @PostMapping("/api/youtube/video-details")
    @ResponseBody
    public ResponseEntity<?> fetchVideoDetailsApi(@RequestBody Map<String, String> request) {
        try {
            String videoId = request.get("videoId");

            if (videoId == null || videoId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Video ID is required"));
            }

            VideoDetails details = youTubeService.getVideoDetails(videoId.trim());

            if (details == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(details);

        } catch (Exception e) {
            log.error("Error fetching video details via API: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to fetch video details: " + e.getMessage()));
        }
    }
}
