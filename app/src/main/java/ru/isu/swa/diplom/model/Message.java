package ru.isu.swa.diplom.model;

import java.util.*;

/**
 * Created by swa on 23.11.2017.
 */

//Сообщения
public class Message {
    private Integer id;
    private User author;
    private Set<User> receivers; //получатели. Их может быть много
    private Group groupReceiver; //получатель-группа (для групповых чатов)
    private String message; //Сообщение
    private String date;
    private Mark mark; // Ссылка на метку. Может быть null
    private Map<Integer, String> key; // Карта прикрепленных ключей шифрования. Используется для зашифрованных меток. Имеет вид <ID Открытого ключа RSA получателя, Зашифрованное на этом ключе сообщение с ключом от метки(можно использовать AES)> Может быть null


    public Message() {
    }

    public Message(User author, Set<User> receivers, Group groupReceiver, String message, String date, Mark mark, Map<Integer, String> key) {
        this.author = author;
        this.receivers = receivers;
        this.groupReceiver = groupReceiver;
        this.message = message;
        this.date = date;
        this.mark = mark;
        this.key = key;
    }


    public Group getGroupReceiver() {
        return groupReceiver;
    }

    public void setGroupReceiver(Group groupReceiver) {
        this.groupReceiver = groupReceiver;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Set<User> getReceivers() {
        return receivers;
    }

    public void setReceivers(Set<User> receivers) {
        this.receivers = receivers;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Mark getMark() {
        return mark;
    }

    public void setMark(Mark mark) {
        this.mark = mark;
    }

    public Map<Integer, String> getKey() {
        return key;
    }

    public void setKey(Map<Integer, String> key) {
        this.key = key;
    }
}
