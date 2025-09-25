package com.YouTubeTools.Service;

import com.YouTubeTools.Model.VideoTranscript;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TranscriptService {

    @Value("${youtube.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public TranscriptService(RestTemplate restTemplate, WebClient.Builder webClientBuilder) {
        this.restTemplate = restTemplate;
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = new ObjectMapper();
    }

    public VideoTranscript getTranscript(String videoId, String language) {
        try {
            log.info("Fetching transcript for video: {} with language: {}", videoId, language);

            // Try multiple methods to get transcript
            VideoTranscript transcript = null;

            // Method 1: Try using YouTube's timedtext API
            transcript = fetchTranscriptFromTimedText(videoId, language);
            if (transcript != null && !transcript.getTranscriptEntries().isEmpty()) {
                log.info("Successfully fetched transcript using timedtext API");
                return transcript;
            }

            // Method 2: Try fetching from YouTube page with captions data
            transcript = fetchTranscriptFromYouTubePage(videoId, language);
            if (transcript != null && !transcript.getTranscriptEntries().isEmpty()) {
                log.info("Successfully fetched transcript from YouTube page");
                return transcript;
            }

            // Method 3: Try using YouTube Data API v3 with captions
            transcript = fetchTranscriptUsingDataAPI(videoId, language);
            if (transcript != null && !transcript.getTranscriptEntries().isEmpty()) {
                log.info("Successfully fetched transcript using Data API");
                return transcript;
            }

            log.warn("No transcript found for video: {}", videoId);
            return createEmptyTranscript(videoId, language);

        } catch (Exception e) {
            log.error("Error fetching transcript for video: {}", videoId, e);
            return createEmptyTranscript(videoId, language);
        }
    }

    private VideoTranscript fetchTranscriptFromTimedText(String videoId, String language) {
        try {
            // First, get available caption tracks
            String listUrl = String.format("https://www.youtube.com/api/timedtext?v=%s&type=list", videoId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> listResponse = restTemplate.exchange(listUrl, HttpMethod.GET, entity, String.class);

            if (listResponse.getStatusCode() == HttpStatus.OK && listResponse.getBody() != null) {
                // Parse available languages
                String targetLang = findBestLanguageMatch(listResponse.getBody(), language);

                if (targetLang != null) {
                    // Fetch the actual transcript
                    String transcriptUrl = String.format(
                            "https://www.youtube.com/api/timedtext?v=%s&lang=%s&fmt=json3",
                            videoId, targetLang
                    );

                    ResponseEntity<String> transcriptResponse = restTemplate.exchange(
                            transcriptUrl, HttpMethod.GET, entity, String.class
                    );

                    if (transcriptResponse.getStatusCode() == HttpStatus.OK && transcriptResponse.getBody() != null) {
                        return parseJson3Transcript(videoId, transcriptResponse.getBody(), targetLang);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Timedtext API method failed: {}", e.getMessage());
        }
        return null;
    }

    private VideoTranscript fetchTranscriptFromYouTubePage(String videoId, String language) {
        try {
            String videoUrl = "https://www.youtube.com/watch?v=" + videoId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.set("Accept-Language", language + ",en;q=0.9");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(videoUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String html = response.getBody();

                // Extract player response from YouTube page
                Pattern pattern = Pattern.compile("var ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\});", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(html);

                if (matcher.find()) {
                    String playerResponse = matcher.group(1);
                    JsonNode playerJson = objectMapper.readTree(playerResponse);

                    // Check for captions
                    JsonNode captionsNode = playerJson.path("captions").path("playerCaptionsTracklistRenderer");
                    if (!captionsNode.isMissingNode()) {
                        JsonNode captionTracks = captionsNode.path("captionTracks");
                        if (captionTracks.isArray() && captionTracks.size() > 0) {
                            // Find the best matching caption track
                            JsonNode selectedTrack = selectBestCaptionTrack(captionTracks, language);
                            if (selectedTrack != null) {
                                String captionUrl = selectedTrack.path("baseUrl").asText();
                                if (!captionUrl.isEmpty()) {
                                    return fetchAndParseTranscriptFromUrl(videoId, captionUrl, language);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("YouTube page parsing failed: {}", e.getMessage());
        }
        return null;
    }

    private VideoTranscript fetchTranscriptUsingDataAPI(String videoId, String language) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("YOUR_YOUTUBE_API_KEY_HERE")) {
            log.debug("YouTube API key not configured, skipping Data API method");
            return null;
        }

        try {
            // Note: YouTube Data API v3 doesn't directly provide transcript content
            // This method would require OAuth2 authentication and caption download permission
            // For now, we'll use this as a fallback to check if captions exist

            String apiUrl = String.format(
                    "https://www.googleapis.com/youtube/v3/captions?videoId=%s&part=snippet&key=%s",
                    videoId, apiKey
            );

            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.path("items");

                if (items.isArray() && items.size() > 0) {
                    // Captions exist but we can't download them without OAuth
                    log.info("Captions exist for video {} but require OAuth to download", videoId);
                }
            }
        } catch (Exception e) {
            log.debug("Data API method failed: {}", e.getMessage());
        }
        return null;
    }

    private VideoTranscript fetchAndParseTranscriptFromUrl(String videoId, String captionUrl, String language) {
        try {
            // Add format parameter if not present
            if (!captionUrl.contains("fmt=")) {
                captionUrl += (captionUrl.contains("?") ? "&" : "?") + "fmt=json3";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(captionUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseJson3Transcript(videoId, response.getBody(), language);
            }
        } catch (Exception e) {
            log.error("Failed to fetch transcript from URL: {}", e.getMessage());
        }
        return null;
    }

    private VideoTranscript parseJson3Transcript(String videoId, String jsonContent, String language) {
        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            JsonNode events = root.path("events");

            if (events.isArray()) {
                List<VideoTranscript.TranscriptEntry> entries = new ArrayList<>();

                for (JsonNode event : events) {
                    JsonNode segs = event.path("segs");
                    if (segs.isArray() && segs.size() > 0) {
                        double startTime = event.path("tStartMs").asDouble() / 1000.0;
                        double duration = event.path("dDurationMs").asDouble() / 1000.0;

                        StringBuilder text = new StringBuilder();
                        for (JsonNode seg : segs) {
                            String segText = seg.path("utf8").asText();
                            if (!segText.isEmpty()) {
                                text.append(segText);
                            }
                        }

                        String finalText = text.toString().trim();
                        if (!finalText.isEmpty()) {
                            entries.add(VideoTranscript.TranscriptEntry.builder()
                                    .start(startTime)
                                    .duration(duration)
                                    .text(finalText)
                                    .formattedTime(formatTime(startTime))
                                    .build());
                        }
                    }
                }

                if (!entries.isEmpty()) {
                    return VideoTranscript.builder()
                            .videoId(videoId)
                            .language(language)
                            .transcriptEntries(entries)
                            .availableLanguages(Arrays.asList(language))
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse JSON3 transcript: {}", e.getMessage());
        }
        return null;
    }

    private JsonNode selectBestCaptionTrack(JsonNode captionTracks, String preferredLanguage) {
        JsonNode selectedTrack = null;

        // First try to find exact language match
        for (JsonNode track : captionTracks) {
            String trackLang = track.path("languageCode").asText();
            if (trackLang.equalsIgnoreCase(preferredLanguage)) {
                // Prefer non-auto-generated captions
                if (!track.path("kind").asText().equals("asr")) {
                    return track;
                }
                selectedTrack = track;
            }
        }

        // If exact match found, return it
        if (selectedTrack != null) {
            return selectedTrack;
        }

        // Try to find language with same prefix (e.g., "en" for "en-US")
        String langPrefix = preferredLanguage.split("-")[0];
        for (JsonNode track : captionTracks) {
            String trackLang = track.path("languageCode").asText();
            if (trackLang.startsWith(langPrefix)) {
                if (!track.path("kind").asText().equals("asr")) {
                    return track;
                }
                selectedTrack = track;
            }
        }

        // If still no match, return first available track
        if (selectedTrack == null && captionTracks.size() > 0) {
            selectedTrack = captionTracks.get(0);
        }

        return selectedTrack;
    }

    private String findBestLanguageMatch(String xmlContent, String preferredLanguage) {
        try {
            // Parse XML to find available languages
            Pattern pattern = Pattern.compile("lang_code=\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(xmlContent);

            List<String> availableLanguages = new ArrayList<>();
            while (matcher.find()) {
                availableLanguages.add(matcher.group(1));
            }

            // Try exact match
            if (availableLanguages.contains(preferredLanguage)) {
                return preferredLanguage;
            }

            // Try prefix match
            String prefix = preferredLanguage.split("-")[0];
            for (String lang : availableLanguages) {
                if (lang.startsWith(prefix)) {
                    return lang;
                }
            }

            // Default to first available or "en"
            if (availableLanguages.contains("en")) {
                return "en";
            }

            return availableLanguages.isEmpty() ? null : availableLanguages.get(0);

        } catch (Exception e) {
            log.debug("Failed to find language match: {}", e.getMessage());
            return "en";
        }
    }

    private String formatTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%d:%02d", minutes, secs);
        }
    }

    private VideoTranscript createEmptyTranscript(String videoId, String language) {
        return VideoTranscript.builder()
                .videoId(videoId)
                .language(language)
                .transcriptEntries(Collections.emptyList())
                .availableLanguages(Collections.emptyList())
                .fullText("")
                .wordCount(0)
                .totalDuration(0)
                .build();
    }

    public String formatAsSRT(String content) {
        StringBuilder srt = new StringBuilder();
        String[] lines = content.split("\n");
        int counter = 1;

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            srt.append(counter++).append("\n");
            srt.append("00:00:00,000 --> 00:00:05,000").append("\n");
            srt.append(line).append("\n\n");
        }
        return srt.toString();
    }

    public String formatAsVTT(String content) {
        StringBuilder vtt = new StringBuilder();
        vtt.append("WEBVTT\n\n");
        String[] lines = content.split("\n");
        int counter = 1;

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            vtt.append(counter++).append("\n");
            vtt.append("00:00:00.000 --> 00:00:05.000").append("\n");
            vtt.append(line).append("\n\n");
        }
        return vtt.toString();
    }
}