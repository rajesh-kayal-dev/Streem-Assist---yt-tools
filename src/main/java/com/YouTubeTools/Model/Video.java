package com.YouTubeTools.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Collections;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Video {
    private String id;
    private String channelTitle;
    private String title;
    private List<String> tags;
    private String thumbnailUrl;
    private String publishedAt;
    private String description;

    // Utility methods
    public List<String> getTags() {
        return tags != null ? tags : Collections.emptyList();
    }

    public boolean hasTags() {
        return tags != null && !tags.isEmpty();
    }

    public String getFormattedTitle() {
        return title != null ? title.trim() : "Untitled Video";
    }

    public String getFormattedChannelTitle() {
        return channelTitle != null ? channelTitle.trim() : "Unknown Channel";
    }
}