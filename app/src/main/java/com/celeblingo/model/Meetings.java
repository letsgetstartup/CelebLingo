package com.celeblingo.model;

import com.google.api.client.util.DateTime;

import java.util.List;

public class Meetings {
    private String id;
    private String summary;
    private String description;
    private String startTime;
    private String endTime;
    private String gptUrl, driveUrl, videoUrl, htmlUrl;
    private List<Attendee> attendees;
    private Organizer organizer;

    public Meetings() {
    }

    public Meetings(String id, String summary, String description, String startTime, String endTime,
                    String gptUrl, String driveUrl, String videoUrl, String htmlUrl,
                    List<Attendee> attendees, Organizer organizer) {
        this.id = id;
        this.summary = summary;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.gptUrl = gptUrl;
        this.driveUrl = driveUrl;
        this.videoUrl = videoUrl;
        this.htmlUrl = htmlUrl;
        this.attendees = attendees;
        this.organizer = organizer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getGptUrl() {
        return gptUrl;
    }

    public void setGptUrl(String gptUrl) {
        this.gptUrl = gptUrl;
    }

    public String getDriveUrl() {
        return driveUrl;
    }

    public void setDriveUrl(String driveUrl) {
        this.driveUrl = driveUrl;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public List<Attendee> getAttendees() {
        return attendees;
    }

    public void setAttendees(List<Attendee> attendees) {
        this.attendees = attendees;
    }

    public Organizer getOrganizer() {
        return organizer;
    }

    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }

    public static class Attendee {
        private String email;
        private String displayName;
        private String responseStatus;

        public Attendee() {
        }

        public Attendee(String email, String displayName, String responseStatus) {
            this.email = email;
            this.displayName = displayName;
            this.responseStatus = responseStatus;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getResponseStatus() {
            return responseStatus;
        }

        public void setResponseStatus(String responseStatus) {
            this.responseStatus = responseStatus;
        }

    }

    // Nested class for organizer
    public static class Organizer {
        private String email;
        private boolean self;

        public Organizer() {
        }

        public Organizer(String email, boolean self) {
            this.email = email;
            this.self = self;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public boolean isSelf() {
            return self;
        }

        public void setSelf(boolean self) {
            this.self = self;
        }
    }

}
