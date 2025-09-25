package com.YouTubeTools.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Collections;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VideoDetails {
    private String id;
    private String title;
    private String description;
    private List<String> tags;
    private String thumbnailUrl;
    private String channelTitle;
    private String publishedAt;
    private Long viewCount;
    private Long likeCount;
    private String duration;

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

    public String getFormattedDescription() {
        if (description == null || description.trim().isEmpty()) {
            return "No description available.";
        }
        return description.trim();
    }

    public String getShortDescription() {
        String desc = getFormattedDescription();
        if (desc.length() > 200) {
            return desc.substring(0, 197) + "...";
        }
        return desc;
    }

    public String getFormattedPublishDate() {
        if (publishedAt == null) {
            return "Unknown date";
        }

        try {
            LocalDateTime dateTime = LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME);
            return dateTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        } catch (DateTimeParseException e) {
            return publishedAt; // Return as-is if parsing fails
        }
    }
}