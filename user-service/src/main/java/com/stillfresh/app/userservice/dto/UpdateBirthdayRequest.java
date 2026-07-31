package com.stillfresh.app.userservice.dto;

public class UpdateBirthdayRequest {

    /**
     * Birthday in ISO-8601 format (yyyy-MM-dd).
     */
    private String birthday;

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }
}

