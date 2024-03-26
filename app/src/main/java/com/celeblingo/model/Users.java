package com.celeblingo.model;

public class Users {
    String id, email, name, historyHtml;

    public Users() {
    }

    public Users(String id, String email, String name, String historyHtml) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.historyHtml = historyHtml;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHistoryHtml() {
        return historyHtml;
    }

    public void setHistoryHtml(String historyHtml) {
        this.historyHtml = historyHtml;
    }
}
