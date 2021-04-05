/*
 * Copyright (c) 2019. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.models;

import android.content.ContentUris;
import android.content.Context;
import android.provider.ContactsContract;

import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import androidx.annotation.NonNull;
import rs.highlande.highlanders_app.adapters.ContactsAdapter;
import rs.highlande.highlanders_app.utility.Utils;
import rs.highlande.highlanders_app.utility.helpers.JsonHelper;

public class ProfileContactToSend implements JsonHelper.JsonDeSerializer, Comparable<ProfileContactToSend> {

    private final long id;
    private final String name;

    private byte[] photo;

    // INFO: 3/6/19    Proguard and Gson issue: "phones" serialized as "e" -> using @SerializedName for "phones" fixes
    @SerializedName("phones")
    @Expose
    private List<String> phones = new ArrayList<>();

    @SerializedName("mails")
    @Expose private List<String> emails = new ArrayList<>();

    private boolean processed;


    public ProfileContactToSend(String id, String name) {
        this.id = Long.parseLong(id);
        this.name = name;
    }

    public ProfileContactToSend(Context context, ContactsAdapter adapter, long id, String name) {
        this.id = id;
        this.name = name;
//			setPhoto(context, adapter);
    }


    @Override
    public int hashCode() {
        return String.valueOf(id).hashCode();
    }

    @Override
    public int compareTo(@NonNull ProfileContactToSend contactToSend) {
        if (Utils.areStringsValid(name, contactToSend.name))
            return name.compareTo(contactToSend.name);

        return 0;
    }


    public boolean hasPhones() {
        return phones != null && !phones.isEmpty();
    }

    public boolean hasEmails() {
        return emails != null && !emails.isEmpty();
    }

    public String getFirstNumber() {
        if (phones != null && !phones.isEmpty())
            return phones.get(0);

        return null;
    }

    public String getFirstAddress() {
        if (emails != null && !emails.isEmpty()) {
            for (String email : emails) {
                if (Utils.isEmailValid(email))
                    return email;
            }
        }

        return null;
    }

    public void addEmail(String address) {
        if (Utils.isStringValid(address)) {
            if (emails == null) {
                emails = new ArrayList<>();
            }
            address = address.replaceAll("\\s", "").trim();
            emails.add(address);
        }
    }

    public void addPhone(String number) {
        if (Utils.isStringValid(number)) {
            if (phones == null) {
                phones = new ArrayList<>();
            }
            number = number.replaceAll("\\s", "").trim();
            phones.add(number);
        }
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


    private String getFormattedPhoneNumber(String phoneNumber) {
        // matches any non-digit character and replaces it with "".
        return phoneNumber.replaceAll("[^\\d]", "").trim();
    }


    //region == Getters and setters ==

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getPhones() {
        if (!processed) {
            List<String> filtered = new ArrayList<>();
            List<String> helper = new ArrayList<>();
            if (phones != null && !phones.isEmpty()) {
                for (String num : phones) {
                    String formatted = getFormattedPhoneNumber(num);
                    if (!helper.contains(formatted)) {
                        helper.add(formatted);
                        filtered.add(num);
                    }
                }
                phones.clear();
                phones.addAll(filtered);
                processed = true;
            }
        }

        return phones;
    }
    public void setPhones(List<String> phones) {
        this.phones = phones;
    }

    public List<String> getEmails() {
        return new ArrayList<>(new HashSet<>(emails));
    }
    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public byte[] getPhoto() {
        return photo;
    }
    public void setPhoto(ByteArrayInputStream photo) throws IOException {
        byte[] array = new byte[photo.available()];
        photo.read(array);
        this.photo = array;
    }
    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }
    public void setPhoto(Context context) {
        final InputStream stream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id));
        try {
            if (stream != null) {
                byte[] array = new byte[stream.available()];
                stream.read(array);
                setPhoto(array);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    //endregion
}