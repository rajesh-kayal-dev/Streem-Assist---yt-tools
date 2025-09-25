package com.YouTubeTools.Controller;

import com.YouTubeTools.Service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ThumbnailController {

    private final ThumbnailService thumbnailService;

    @GetMapping("/thumbnail")
    public String getThumbnailPage() {
        return "thumbnails";
    }

    @PostMapping("/get-thumbnail")
    public String showThumbnail(@RequestParam("videoUrlOrId") String videoUrlOrId, Model model) {
        try {
            if (videoUrlOrId == null || videoUrlOrId.trim().isEmpty()) {
                model.addAttribute("error", "Please enter a YouTube video URL or ID");
                return "thumbnails";
            }

            String videoId = thumbnailService.extractVideoId(videoUrlOrId.trim());

            if (videoId == null) {
                model.addAttribute("error", "Invalid YouTube URL or ID. Please check the format and try again.");
                model.addAttribute("videoUrlOrId", videoUrlOrId);
                return "thumbnails";
            }

            // Generate thumbnail URLs for different qualities
            String maxResThumbnail = "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg";
            String highQualityThumbnail = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";

            model.addAttribute("thumbnailUrl", maxResThumbnail);
            model.addAttribute("fallbackThumbnailUrl", highQualityThumbnail);
            model.addAttribute("videoId", videoId);
            model.addAttribute("videoUrlOrId", videoUrlOrId);

            log.info("Generated thumbnail for video ID: {}", videoId);
            return "thumbnails";

        } catch (Exception e) {
            log.error("Error generating thumbnail: {}", e.getMessage(), e);
            model.addAttribute("error", "An error occurred while generating the thumbnail. Please try again.");
            model.addAttribute("videoUrlOrId", videoUrlOrId);
            return "thumbnails";
        }
    }
}
