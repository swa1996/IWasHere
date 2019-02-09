package ru.isu.swa.diplom.model;

import java.util.*;

/**
 * Created by swa on 23.11.2017.
 */
//Пользователи
public class User {
    private Integer id;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private Set<Key> keys; //открытые ключи RSA, может быть много, если 1 аккаунт используется на несколькх устройствах
    private Set<User> friends; //список друзей. Может быть null

    public User() {
    }

    public User(String email, String phoneNumber, String firstName, String lastName, Set<Key> keys, Set<User> friends) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.keys = keys;
        this.friends = friends;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Set<Key> getKeys() {
        return keys;
    }

    public void setKeys(Set<Key> keys) {
        this.keys = keys;
    }

    public Set<User> getFriends() {
        return friends;
    }

    public void setFriends(Set<User> friends) {
        this.friends = friends;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return firstName + " " + lastName;
    }
}
