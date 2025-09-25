package com.YouTubeTools.Service;

import com.YouTubeTools.Model.SearchVideo;
import com.YouTubeTools.Model.Video;
import com.YouTubeTools.Model.VideoDetails;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeService {

    private final WebClient.Builder webClient;

    @Value("${youtube.api.key}")
    private String apiKey;

    @Value("${youtube.api.base.url}")
    private String baseUrl;

    @Value("${youtube.api.max.related.videos}")
    private int maxRelatedVideos;

    public SearchVideo searchVideos(String videoTitle) {
        log.info("Searching videos for title: {}", videoTitle);

        List<String> videoIds = searchForVideoIds(videoTitle);

        if (videoIds.isEmpty()){
            log.warn("No videos found for title: {}", videoTitle);
            return SearchVideo.builder()
                    .primaryVideo(null)
                    .relatedVideos(Collections.emptyList())
                    .build();
        }

        String primaryVideoId = videoIds.get(0);
        List<String> relatedVideoIds = videoIds.subList(1, Math.min(videoIds.size(), maxRelatedVideos + 1));

        log.info("Found {} related videos for primary video: {}", relatedVideoIds.size(), primaryVideoId);

        Video primaryVideo = getVideoById(primaryVideoId);

        List<Video> relatedVideos = new ArrayList<>();

        for (String id : relatedVideoIds) {
            Video video = getVideoById(id);
            if (video != null){
                relatedVideos.add(video);
            }
        }

        log.info("Successfully processed {} related videos", relatedVideos.size());

        return SearchVideo.builder()
                .primaryVideo(primaryVideo)
                .relatedVideos(relatedVideos)
                .build();
    }

    public VideoDetails getVideoDetails(String videoId){
        log.info("Fetching video details for ID: {}", videoId);

        try {
            VideoApiResponse response = webClient.baseUrl(baseUrl).build()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path("/videos")
                            .queryParam("part", "snippet")
                            .queryParam("id", videoId)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(VideoApiResponse.class)
                    .block();

            if (response == null || response.items == null || response.items.isEmpty()){
                log.warn("No video details found for ID: {}", videoId);
                return null;
            }

            Snippet snippet = response.items.get(0).snippet;
            String thumbnailUrl = snippet.thumbnails != null ? snippet.thumbnails.getBestThumbnailUrl() : null;

            VideoDetails details = VideoDetails.builder()
                    .id(videoId)
                    .title(snippet.getTitle())
                    .description(snippet.getDescription())
                    .channelTitle(snippet.getChannelTitle())
                    .publishedAt(snippet.getPublishedAt())
                    .tags(snippet.getTags() != null ? snippet.getTags() : Collections.emptyList())
                    .thumbnailUrl(thumbnailUrl)
                    .build();

            log.info("Successfully fetched details for video: {}", details.getTitle());
            return details;

        } catch (Exception e) {
            log.error("Error fetching video details for ID: {}", videoId, e);
            return null;
        }
    }

    private Video getVideoById(String videoId) {
        log.debug("Fetching video by ID: {}", videoId);

        try {
            VideoApiResponse response = webClient.baseUrl(baseUrl)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path("/videos")
                            .queryParam("part", "snippet")
                            .queryParam("id", videoId)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(VideoApiResponse.class)
                    .block();

            if (response == null || response.items == null || response.items.isEmpty()) {
                log.warn("No video found with ID: {}", videoId);
                return null;
            }

            Snippet snippet = response.items.get(0).snippet;
            return Video.builder()
                    .id(videoId)
                    .channelTitle(snippet.channelTitle)
                    .title(snippet.title)
                    .tags(snippet.tags == null ? Collections.emptyList() : snippet.tags)
                    .build();

        } catch (Exception e) {
            log.error("Error fetching video by ID: {}", videoId, e);
            return null;
        }
    }

    private List<String> searchForVideoIds(String videoTitle) {
        log.debug("Searching for video IDs with title: {}", videoTitle);

        try {
            SearchApiResponse response = webClient.baseUrl(baseUrl).build()
                    .get()
                    .uri(uriBuilder -> uriBuilder.path("/search")
                            .queryParam("part", "snippet")
                            .queryParam("q", videoTitle)
                            .queryParam("type", "video")
                            .queryParam("maxResults", maxRelatedVideos + 1)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(SearchApiResponse.class)
                    .block();

            if (response == null || response.items == null) {
                log.warn("No search results for title: {}", videoTitle);
                return Collections.emptyList();
            }

            List<String> videoIds = new ArrayList<>();

            for (SearchItem item : response.items) {
                if (item.id != null && item.id.videoId != null) {
                    videoIds.add(item.id.videoId);
                }
            }

            log.info("Found {} video IDs for search: {}", videoIds.size(), videoTitle);
            return videoIds;

        } catch (Exception e) {
            log.error("Error searching for videos with title: {}", videoTitle, e);
            return Collections.emptyList();
        }
    }

    @Data
    static class SearchApiResponse {
        List<SearchItem> items;
    }

    @Data
    static class SearchItem {
        Id id;

        @Data
        static class Id {
            String videoId;
        }
    }

    @Data
    static class VideoApiResponse {
        List<VideoItem> items;
    }

    @Data
    static class VideoItem {
        Snippet snippet;
    }

    @Data
    static class Snippet {
        String title;
        String description;
        String channelTitle;
        String publishedAt;
        List<String> tags;
        Thumbnails thumbnails;
    }

    @Data
    static class Thumbnails {
        Thumbnail maxres;
        Thumbnail high;
        Thumbnail medium;
        Thumbnail standard;
        Thumbnail _default;

        String getBestThumbnailUrl() {
            if (maxres != null && maxres.url != null) return maxres.url;
            if (high != null && high.url != null) return high.url;
            if (standard != null && standard.url != null) return standard.url;
            if (medium != null && medium.url != null) return medium.url;
            return _default != null ? _default.url : null;
        }
    }

    @Data
    static class Thumbnail {
        String url;
        int width;
        int height;
    }
}