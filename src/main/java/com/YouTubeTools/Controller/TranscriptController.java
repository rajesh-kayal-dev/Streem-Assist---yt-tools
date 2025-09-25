package com.YouTubeTools.Controller;

import com.YouTubeTools.Model.VideoTranscript;
import com.YouTubeTools.Service.TranscriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TranscriptController {

    private final TranscriptService transcriptService;


    @GetMapping("/transcript")
    public String showTranscriptPage() {
        return "transcript"; // requires authentication
    }

    @GetMapping("/api/transcript/status")
    @ResponseBody
    public ResponseEntity<?> getAuthStatus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            Map<String, Object> response = new HashMap<>();

            if (authentication != null && authentication.isAuthenticated() &&
                    !"anonymousUser".equals(authentication.getName())) {

                response.put("authenticated", true);

                // Get user details from OAuth2User
                if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                    String name = oauth2User.getAttribute("name");
                    String email = oauth2User.getAttribute("email");
                    response.put("user", name != null ? name : (email != null ? email : "User"));
                } else {
                    response.put("user", authentication.getName());
                }

                log.debug("User authenticated: {}", response.get("user"));
            } else {
                response.put("authenticated", false);
                log.debug("User not authenticated");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking authentication status: ", e);
            return ResponseEntity.ok(Map.of("authenticated", false));
        }
    }

    @PostMapping("/api/transcript/fetch")
    @ResponseBody
    public ResponseEntity<?> fetchTranscript(@RequestBody Map<String, String> request) {
        try {
            // Check authentication first
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() ||
                    "anonymousUser".equals(authentication.getName())) {

                log.warn("Unauthenticated transcript request from IP: {}",
                        request != null ? request.get("clientIp") : "unknown");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "Authentication required. Please login with Google."
                ));
            }

            String videoUrlOrId = request.get("videoUrlOrId");
            String language = request.getOrDefault("language", "en");

            if (videoUrlOrId == null || videoUrlOrId.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Video URL or ID is required"
                ));
            }

            String videoId = extractVideoId(videoUrlOrId);
            if (videoId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid YouTube video URL or ID"
                ));
            }

            log.info("Fetching transcript for video: {} by user: {} with language: {}",
                    videoId, authentication.getName(), language);

            VideoTranscript transcript = transcriptService.getTranscript(videoId, language);

            if (transcript == null || transcript.getTranscriptEntries() == null ||
                    transcript.getTranscriptEntries().isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "No transcript available for this video. The video may not have captions enabled, may be private/restricted, or transcripts may be disabled."
                ));
            }

            // Add full transcript text if not already present
            if (transcript.getFullText() == null || transcript.getFullText().isEmpty()) {
                String fullText = transcript.getTranscriptEntries().stream()
                        .map(VideoTranscript.TranscriptEntry::getText)
                        .reduce("", (a, b) -> a + " " + b)
                        .trim();
                transcript.setFullText(fullText);
            }

            log.info("Successfully fetched transcript for video: {} with {} entries",
                    videoId, transcript.getTranscriptEntries().size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", transcript,
                    "videoId", videoId
            ));

        } catch (Exception e) {
            log.error("Error fetching transcript: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", "Error fetching transcript. Please try again later."
            ));
        }
    }

    private String extractVideoId(String urlOrId) {
        if (urlOrId == null) return null;

        urlOrId = urlOrId.trim();

        // Check if it's already a video ID (11 characters, alphanumeric with dashes and underscores)
        if (urlOrId.matches("[a-zA-Z0-9_-]{11}")) {
            return urlOrId;
        }

        // Extract from various YouTube URL formats
        Pattern pattern = Pattern.compile(
                "(?:youtube\\.com/(?:watch\\?v=|embed/|v/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
        );
        Matcher matcher = pattern.matcher(urlOrId);
        return matcher.find() ? matcher.group(1) : null;
    }
}