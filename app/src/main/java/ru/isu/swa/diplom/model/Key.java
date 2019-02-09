package ru.isu.swa.diplom.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by swa on 02.12.2017.
 */
//Открытый ключ RSA пользователя
public class Key {
    private Integer id;
    @SerializedName("pkey")
    private String key;

    public Key() {
    }

    public Key(String key) {
        this.key = key;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}

