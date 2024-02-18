package com.celeblingo.model;

import com.google.api.client.util.DateTime;

import java.util.List;

public class Meetings {
    private String id;
    private String summary;
    private String description;
    private String startTime;
    private String endTime;
    private List<Attendee> attendees;
    private Organizer organizer;

    public Meetings() {
    }

    public Meetings(String id, String summary, String description, String startTime,
                    String endTime, List<Attendee> attendees, Organizer organizer) {
        this.id = id;
        this.summary = summary;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
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

    public void setEndTime(String endTime) {
        this.endTime = endTime;
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

    // Nested class for attendees
    public static class Attendee {
        private String email;
        private String responseStatus;

        // Constructor, getters, and setters

        public Attendee() {
        }

        public Attendee(String email, String responseStatus) {
            this.email = email;
            this.responseStatus = responseStatus;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
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
        private String displayName;
        private boolean self; // True if the organizer is the authenticated user

        // Constructor
        public Organizer(String email, String displayName, boolean self) {
            this.email = email;
            this.displayName = displayName;
            this.self = self;
        }

        // Getters and Setters
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

        public boolean isSelf() {
            return self;
        }

        public void setSelf(boolean self) {
            this.self = self;
        }
    }

}
