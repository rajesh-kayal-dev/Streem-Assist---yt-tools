package com.YouTubeTools.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Collections;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchVideo {
    private Video primaryVideo;
    private List<Video> relatedVideos;
    private String searchQuery;
    private Integer totalResults;

    // Utility methods
    public List<Video> getRelatedVideos() {
        return relatedVideos != null ? relatedVideos : Collections.emptyList();
    }

    public boolean hasResults() {
        return primaryVideo != null || (relatedVideos != null && !relatedVideos.isEmpty());
    }

    public boolean hasPrimaryVideo() {
        return primaryVideo != null;
    }

    public boolean hasRelatedVideos() {
        return relatedVideos != null && !relatedVideos.isEmpty();
    }

    public int getRelatedVideosCount() {
        return relatedVideos != null ? relatedVideos.size() : 0;
    }
}
