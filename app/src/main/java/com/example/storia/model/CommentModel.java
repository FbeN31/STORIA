package com.example.storia.model;

public class CommentModel {
    String id, storyUserId, myUserId, date, comment;
    Boolean isRead;

    public CommentModel() {
    }

    public CommentModel(String id, String storyUserId, String myUserId, String date, String comment) {
        this.id = id;
        this.storyUserId = storyUserId;
        this.myUserId = myUserId;
        this.date = date;
        this.comment = comment;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStoryUserId() {
        return storyUserId;
    }

    public void setStoryUserId(String storyUserId) {
        this.storyUserId = storyUserId;
    }

    public String getMyUserId() {
        return myUserId;
    }

    public void setMyUserId(String myUserId) {
        this.myUserId = myUserId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean isRead() {
        return isRead != null ? isRead : false;
    }

    public void setRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
