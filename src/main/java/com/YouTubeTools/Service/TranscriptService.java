package com.YouTubeTools.Service;

import com.YouTubeTools.Model.VideoTranscript;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TranscriptService {

    @Value("${youtube.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public TranscriptService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }



    public VideoTranscript getTranscript(String videoId, String language) {
        return getVideoTranscript(videoId);
    }

    public VideoTranscript getVideoTranscript(String videoId) {
        try {
            log.info("Attempting to fetch transcript for video: {}", videoId);

            // Method 1: Try using YouTube's internal transcript API
            VideoTranscript transcript = fetchTranscriptViaInternalAPI(videoId);
            if (transcript != null && !transcript.getTranscriptEntries().isEmpty()) {
                return transcript;
            }

            // Method 2: Try alternative approach
            transcript = fetchTranscriptAlternative(videoId);
            if (transcript != null && !transcript.getTranscriptEntries().isEmpty()) {
                return transcript;
            }

            return VideoTranscript.builder()
                    .videoId(videoId)
                    .language("unknown")
                    .availableLanguages(Collections.emptyList())
                    .transcriptEntries(Collections.emptyList())
                    .build();

        } catch (Exception e) {
            log.error("Error fetching transcript for video: {}", videoId, e);
            return VideoTranscript.builder()
                    .videoId(videoId)
                    .language("unknown")
                    .availableLanguages(Collections.emptyList())
                    .transcriptEntries(Collections.emptyList())
                    .build();
        }
    }

    private VideoTranscript fetchTranscriptViaInternalAPI(String videoId) {
        try {
            String url = "https://video.google.com/timedtext?hl=en&v=" + videoId + "&type=list";
            String response = restTemplate.getForObject(url, String.class);

            if (response != null && response.contains("track")) {
                // If list API works, try to get English transcript
                String transcriptUrl = "https://video.google.com/timedtext?hl=en&v=" + videoId + "&lang=en";
                String transcriptXml = restTemplate.getForObject(transcriptUrl, String.class);

                if (transcriptXml != null) {
                    return parseTranscriptXML(videoId, transcriptXml);
                }
            }
        } catch (Exception e) {
            log.debug("Internal API method failed: {}", e.getMessage());
        }
        return null;
    }

    private VideoTranscript fetchTranscriptAlternative(String videoId) {
        try {
            // Try to fetch from YouTube page
            String videoUrl = "https://www.youtube.com/watch?v=" + videoId;
            String htmlContent = restTemplate.getForObject(videoUrl, String.class);

            if (htmlContent != null) {
                // Extract transcript data from YouTube page
                return extractTranscriptFromHTML(videoId, htmlContent);
            }
        } catch (Exception e) {
            log.debug("Alternative method failed: {}", e.getMessage());
        }
        return null;
    }

    private VideoTranscript extractTranscriptFromHTML(String videoId, String htmlContent) {
        try {
            Document doc = Jsoup.parse(htmlContent);

            // Look for transcript data in script tags
            Elements scriptTags = doc.select("script");
            for (Element script : scriptTags) {
                String scriptContent = script.html();
                if (scriptContent.contains("captionTracks")) {
                    // Parse YouTube's player config for captions
                    return parsePlayerConfig(videoId, scriptContent);
                }
            }
        } catch (Exception e) {
            log.debug("HTML extraction failed: {}", e.getMessage());
        }
        return null;
    }

    private VideoTranscript parsePlayerConfig(String videoId, String scriptContent) {
        try {
            // Extract caption tracks from YouTube player config
            Pattern pattern = Pattern.compile("captionTracks\"\\s*:\\s*\\[([^]]+)\\]");
            Matcher matcher = pattern.matcher(scriptContent);

            if (matcher.find()) {
                String tracksJson = matcher.group(1);
                // Simple parsing - look for baseUrl
                Pattern urlPattern = Pattern.compile("baseUrl\"\\s*:\\s*\"([^\"]+)\"");
                Matcher urlMatcher = urlPattern.matcher(tracksJson);

                if (urlMatcher.find()) {
                    String transcriptUrl = urlMatcher.group(1).replace("\\u0026", "&");
                    String transcriptXml = restTemplate.getForObject(transcriptUrl, String.class);

                    if (transcriptXml != null) {
                        return parseTranscriptXML(videoId, transcriptXml);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Player config parsing failed: {}", e.getMessage());
        }
        return null;
    }

    private VideoTranscript parseTranscriptXML(String videoId, String xmlContent) {
        List<VideoTranscript.TranscriptEntry> entries = new ArrayList<>();

        try {
            Document doc = Jsoup.parse(xmlContent);
            Elements textElements = doc.select("text");

            for (Element element : textElements) {
                String start = element.attr("start");
                String duration = element.attr("dur");
                String text = element.text();

                if (!text.isEmpty()) {
                    double startTime = Double.parseDouble(start);
                    double dur = duration.isEmpty() ? 5.0 : Double.parseDouble(duration); // Default 5 seconds

                    entries.add(VideoTranscript.TranscriptEntry.builder()
                            .start(startTime)
                            .duration(dur)
                            .text(text)
                            .formattedTime(formatTime(startTime))
                            .build());
                }
            }

            if (!entries.isEmpty()) {
                return VideoTranscript.builder()
                        .videoId(videoId)
                        .language("en")
                        .transcriptEntries(entries)
                        .availableLanguages(List.of("en"))
                        .build();
            }
        } catch (Exception e) {
            log.debug("XML parsing failed: {}", e.getMessage());
        }

        return null;
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

    public String formatAsSRT(String content) {
        // Implementation remains the same
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
        // Implementation remains the same
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