package com.YouTubeTools.Controller;

import com.YouTubeTools.Service.ThumbnailService;
import com.YouTubeTools.Service.YouTubeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;
import java.util.*;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DownloadController {

    private final ThumbnailService thumbnailService;
    private final YouTubeService youTubeService;
    private final WebClient.Builder webClientBuilder;

    @GetMapping("/download")
    public String showDownloadPage() {
        return "download";
    }

    @PostMapping("/get-download-links")
    @ResponseBody
    public ResponseEntity<?> getDownloadLinks(@RequestParam("videoUrlOrId") String videoUrlOrId) {
        try {
            String videoId = thumbnailService.extractVideoId(videoUrlOrId);
            if (videoId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid YouTube URL or video ID"));
            }

            // Get video details
            var videoDetails = youTubeService.getVideoDetails(videoId);
            if (videoDetails == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Video not found or unavailable"));
            }

            // Generate safe download options (no external YouTube page parsing)
            List<Map<String, Object>> downloadOptions = generateSafeDownloadOptions(videoId);

            Map<String, Object> response = new HashMap<>();
            response.put("videoId", videoId);
            response.put("title", videoDetails.getTitle());
            response.put("thumbnail", videoDetails.getThumbnailUrl());
            response.put("channelTitle", videoDetails.getChannelTitle());
            response.put("youtubeUrl", "https://www.youtube.com/watch?v=" + videoId);
            response.put("downloadOptions", downloadOptions);

            log.info("Generated {} download options for video: {}", downloadOptions.size(), videoDetails.getTitle());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting download links for: {}", videoUrlOrId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred while processing your request"));
        }
    }

    @GetMapping("/download-video")
    public ResponseEntity<Resource> downloadVideo(
            @RequestParam String videoId,
            @RequestParam String quality,
            @RequestParam String format) {

        try {
            log.info("Download request - VideoID: {}, Quality: {}, Format: {}", videoId, quality, format);

            // Get video details for filename
            var videoDetails = youTubeService.getVideoDetails(videoId);
            String videoTitle = videoDetails != null ? videoDetails.getTitle() : "Unknown";

            // Generate a demonstration file with instructions
            String demonstrationContent = generateDemonstrationFile(videoId, quality, format, videoTitle);

            InputStream inputStream = new ByteArrayInputStream(demonstrationContent.getBytes());
            InputStreamResource resource = new InputStreamResource(inputStream);

            String filename = sanitizeFilename(videoTitle) + "_" + quality + "." + format;

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, getContentType(format));

            log.info("Serving demonstration file: {}", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(getContentType(format)))
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading video", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<Map<String, Object>> generateSafeDownloadOptions(String videoId) {
        List<Map<String, Object>> options = new ArrayList<>();

        // Video qualities with realistic options
        String[][] videoOptions = {
                {"2160p", "mp4", "~500-800MB"},
                {"1080p", "mp4", "~200-400MB"},
                {"720p", "mp4", "~100-200MB"},
                {"720p", "webm", "~80-150MB"},
                {"480p", "mp4", "~50-100MB"},
                {"360p", "mp4", "~25-50MB"}
        };

        for (String[] option : videoOptions) {
            Map<String, Object> videoOption = new HashMap<>();
            videoOption.put("type", "video");
            videoOption.put("quality", option[0]);
            videoOption.put("format", option[1]);
            videoOption.put("size", option[2]);
            videoOption.put("videoId", videoId);
            videoOption.put("downloadUrl", "/download-video?videoId=" + videoId +
                    "&quality=" + option[0] + "&format=" + option[1]);
            options.add(videoOption);
        }

        // Audio options
        String[][] audioOptions = {
                {"320kbps", "mp3", "~8-12MB"},
                {"256kbps", "mp3", "~6-10MB"},
                {"128kbps", "mp3", "~3-6MB"},
                {"256kbps", "m4a", "~6-10MB"},
                {"128kbps", "webm", "~3-6MB"}
        };

        for (String[] option : audioOptions) {
            Map<String, Object> audioOption = new HashMap<>();
            audioOption.put("type", "audio");
            audioOption.put("quality", option[0]);
            audioOption.put("format", option[1]);
            audioOption.put("size", option[2]);
            audioOption.put("videoId", videoId);
            audioOption.put("downloadUrl", "/download-video?videoId=" + videoId +
                    "&quality=" + option[0] + "&format=" + option[1]);
            options.add(audioOption);
        }

        return options;
    }

    private String generateDemonstrationFile(String videoId, String quality, String format, String title) {
        return String.format("""
                =================================
                YOUTUBE TOOLS - DOWNLOAD DEMO
                =================================
                
                Video: %s
                Video ID: %s
                Quality: %s
                Format: %s
                
                =================================
                IMPLEMENTATION INSTRUCTIONS:
                =================================
                
                This is a demonstration file. To enable actual video downloads:
                
                1. INSTALL YT-DLP:
                   pip install yt-dlp
                
                2. UPDATE DownloadController.java:
                   Replace generateDemonstrationFile() method with:
                   
                   private ResponseEntity<Resource> streamActualVideo(String videoId, String quality, String format) {
                       try {
                           // Execute yt-dlp command
                           ProcessBuilder pb = new ProcessBuilder(
                               "yt-dlp",
                               "-f", "best[height<=" + quality.replace("p", "") + "]",
                               "--output", "temp_%%(id)s.%%(ext)s",
                               "https://www.youtube.com/watch?v=" + videoId
                           );
                           
                           Process process = pb.start();
                           process.waitFor();
                           
                           // Stream the downloaded file
                           File videoFile = new File("temp_" + videoId + "." + format);
                           if (videoFile.exists()) {
                               FileInputStream fis = new FileInputStream(videoFile);
                               InputStreamResource resource = new InputStreamResource(fis);
                               
                               // Clean up file after streaming
                               videoFile.deleteOnExit();
                               
                               return ResponseEntity.ok()
                                   .contentType(MediaType.parseMediaType(getContentType(format)))
                                   .body(resource);
                           }
                       } catch (Exception e) {
                           log.error("Error downloading actual video", e);
                       }
                       return ResponseEntity.status(500).build();
                   }
                
                3. LEGAL CONSIDERATIONS:
                   - Ensure compliance with YouTube Terms of Service
                   - Respect copyright laws and content creator rights
                   - Consider implementing rate limiting
                   - Add user authentication if needed
                
                4. PERFORMANCE OPTIMIZATIONS:
                   - Implement background download queue
                   - Add caching mechanism
                   - Stream files directly without saving to disk
                   - Add progress tracking for large files
                
                =================================
                CURRENT STATUS: DEMONSTRATION MODE
                =================================
                
                YouTube URL: https://www.youtube.com/watch?v=%s
                Generated: %s
                
                """, title, videoId, quality, format, videoId, new Date().toString());
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "video";
        return filename.replaceAll("[^a-zA-Z0-9\\s\\-_.]", "")
                .replaceAll("\\s+", "_")
                .substring(0, Math.min(filename.length(), 50));
    }

    private String getContentType(String format) {
        return switch (format.toLowerCase()) {
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mp3" -> "audio/mpeg";
            case "m4a" -> "audio/mp4";
            default -> "application/octet-stream";
        };
    }
}