package com.YouTubeTools.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VideoTranscript {
    private String videoId;
    private String videoTitle;
    private String language;
    private List<TranscriptEntry> transcriptEntries;
    private String fullText;
    private double totalDuration;
    private int wordCount;
    private List<String> availableLanguages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TranscriptEntry {
        private double start;
        private double duration;
        private String text;
        private String formattedTime;

        /**
         * Format start time for display
         */
        public String getFormattedTime() {
            if (formattedTime == null) {
                formattedTime = formatTime(start);
            }
            return formattedTime;
        }

        private String formatTime(double seconds) {
            int hours = (int) (seconds / 3600);
            int minutes = (int) ((seconds % 3600) / 60);
            int secs = (int) (seconds % 60);

            if (hours > 0) {
                return String.format("%02d:%02d:%02d", hours, minutes, secs);
            } else {
                return String.format("%02d:%02d", minutes, secs);
            }
        }
    }

    /**
     * Get full transcript text with timestamps
     */
    public String getFullText() {
        if (fullText == null && transcriptEntries != null && !transcriptEntries.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (TranscriptEntry entry : transcriptEntries) {
                sb.append("[").append(formatTime(entry.start)).append("] ");
                sb.append(entry.text).append("\n");
            }
            fullText = sb.toString();
        }
        return fullText;
    }

    /**
     * Get plain text without timestamps
     */
    public String getPlainText() {
        if (transcriptEntries == null || transcriptEntries.isEmpty()) {
            return "";
        }
        return transcriptEntries.stream()
                .map(TranscriptEntry::getText)
                .reduce("", (a, b) -> a + " " + b)
                .trim();
    }

    /**
     * Calculate total duration from entries
     */
    public double getTotalDuration() {
        if (totalDuration == 0 && transcriptEntries != null && !transcriptEntries.isEmpty()) {
            TranscriptEntry lastEntry = transcriptEntries.get(transcriptEntries.size() - 1);
            totalDuration = lastEntry.start + lastEntry.duration;
        }
        return totalDuration;
    }

    /**
     * Calculate word count from transcript entries
     */
    public int getWordCount() {
        if (wordCount == 0 && transcriptEntries != null) {
            wordCount = transcriptEntries.stream()
                    .mapToInt(entry -> {
                        if (entry.text == null || entry.text.trim().isEmpty()) {
                            return 0;
                        }
                        return entry.text.trim().split("\\s+").length;
                    })
                    .sum();
        }
        return wordCount;
    }

    /**
     * Format time in HH:MM:SS or MM:SS format
     */
    private String formatTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }

    /**
     * Get formatted transcript with timestamps
     */
    public String getFormattedTranscript() {
        if (transcriptEntries == null || transcriptEntries.isEmpty()) {
            return "No transcript available";
        }

        StringBuilder formatted = new StringBuilder();
        for (TranscriptEntry entry : transcriptEntries) {
            formatted.append("[").append(entry.getFormattedTime()).append("] ")
                    .append(entry.text).append("\n");
        }
        return formatted.toString();
    }

    /**
     * Search for text in transcript and return matching entries
     */
    public List<TranscriptEntry> searchInTranscript(String searchText) {
        if (transcriptEntries == null || searchText == null || searchText.trim().isEmpty()) {
            return List.of();
        }

        String searchLower = searchText.toLowerCase().trim();
        return transcriptEntries.stream()
                .filter(entry -> entry.text != null &&
                        entry.text.toLowerCase().contains(searchLower))
                .toList();
    }

    /**
     * Get transcript entries within a time range
     */
    public List<TranscriptEntry> getEntriesInTimeRange(double startTime, double endTime) {
        if (transcriptEntries == null) {
            return List.of();
        }

        return transcriptEntries.stream()
                .filter(entry -> entry.start >= startTime && entry.start <= endTime)
                .toList();
    }

    /**
     * Get transcript summary info
     */
    public TranscriptSummary getSummary() {
        return TranscriptSummary.builder()
                .videoId(videoId)
                .videoTitle(videoTitle)
                .language(language)
                .totalDuration(getTotalDuration())
                .wordCount(getWordCount())
                .entryCount(transcriptEntries != null ? transcriptEntries.size() : 0)
                .hasTranscript(transcriptEntries != null && !transcriptEntries.isEmpty())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptSummary {
        private String videoId;
        private String videoTitle;
        private String language;
        private double totalDuration;
        private int wordCount;
        private int entryCount;
        private boolean hasTranscript;
    }

    /**
     * Validate transcript data integrity
     */
    public boolean isValid() {
        return videoId != null && !videoId.trim().isEmpty() &&
                transcriptEntries != null && !transcriptEntries.isEmpty() &&
                transcriptEntries.stream().allMatch(entry ->
                        entry.text != null && !entry.text.trim().isEmpty() &&
                                entry.start >= 0 && entry.duration >= 0);
    }

    /**
     * Get transcript as SRT format
     */
    public String toSrtFormat() {
        if (transcriptEntries == null || transcriptEntries.isEmpty()) {
            return "";
        }

        StringBuilder srt = new StringBuilder();
        for (int i = 0; i < transcriptEntries.size(); i++) {
            TranscriptEntry entry = transcriptEntries.get(i);

            srt.append(i + 1).append("\n");
            srt.append(formatSrtTime(entry.start)).append(" --> ")
                    .append(formatSrtTime(entry.start + entry.duration)).append("\n");
            srt.append(entry.text).append("\n\n");
        }

        return srt.toString();
    }

    private String formatSrtTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds % 1) * 1000);

        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
    }
}