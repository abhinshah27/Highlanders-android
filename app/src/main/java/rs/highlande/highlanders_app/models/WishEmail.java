/*
 * Copyright (c) 2019. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.models;

import com.google.gson.JsonElement;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import rs.highlande.highlanders_app.utility.helpers.JsonHelper;

public class WishEmail implements JsonHelper.JsonDeSerializer {

    private String id;
    private String emailTo;
    private String emailFrom;
    private String subject;
    private String message;

    private boolean profilePictureAttached;

    private List<String> attachments;

    {
        attachments = new ArrayList<>();
    }

    public WishEmail(String emailTo, String emailFrom, String subject, String message) {
        this.emailTo = emailTo;
        this.emailFrom = emailFrom;
        this.subject = subject;
        this.message = message;
    }

    @Override
    public JsonElement serializeWithExpose() {
        return JsonHelper.serializeWithExpose(this);
    }

    @Override
    public String serializeToStringWithExpose() {
        return JsonHelper.serializeToStringWithExpose(this);
    }

    @Override
    public JsonElement serialize() {
        return JsonHelper.serialize(this);
    }

    @Override
    public String serializeToString() {
        return JsonHelper.serializeToString(this);
    }

    @Override
    public JsonHelper.JsonDeSerializer deserialize(JSONObject json, Class myClass) {
        return null;
    }

    @Override
    public JsonHelper.JsonDeSerializer deserialize(JsonElement json, Class myClass) {
        return null;
    }

    @Override
    public JsonHelper.JsonDeSerializer deserialize(String jsonString, Class myClass) {
        return null;
    }

    @Override
    public Object getSelfObject() {
        return this;
    }


    //region == Getters and setters

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getEmailTo() {
        return emailTo;
    }
    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public String getEmailFrom() {
        return emailFrom;
    }
    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getAttachments() {
        return attachments;
    }
    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }

    public boolean isProfilePictureAttached() {
        return profilePictureAttached;
    }
    public void setProfilePictureAttached(boolean profilePictureAttached) {
        this.profilePictureAttached = profilePictureAttached;
    }

    //endregion
}
