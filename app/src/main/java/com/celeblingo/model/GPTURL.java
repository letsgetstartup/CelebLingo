package com.celeblingo.model;

import java.util.ArrayList;

public class GPTURL {
    String text, title, url, image;
    ArrayList<BGImages> bgImagesArrayList;

    public GPTURL() {
    }

    public GPTURL(String text, String title, String url, String image, ArrayList<BGImages> bgImagesArrayList) {
        this.text = text;
        this.title = title;
        this.url = url;
        this.image = image;
        this.bgImagesArrayList = bgImagesArrayList;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public ArrayList<BGImages> getBgImagesArrayList() {
        return bgImagesArrayList;
    }

    public void setBgImagesArrayList(ArrayList<BGImages> bgImagesArrayList) {
        this.bgImagesArrayList = bgImagesArrayList;
    }
}
