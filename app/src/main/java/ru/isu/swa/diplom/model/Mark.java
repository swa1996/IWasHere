package ru.isu.swa.diplom.model;


/**
 * Created by swa on 23.11.2017.
 */

//Метка
public class Mark {
    private Integer id;
    private String message;
    private User author;
    private boolean isAnonymed; //запись анонимна(в БД всё равно хранится поле Автор)
    private boolean isEncrypted; // Сообщение зашифровано и не должно показываться без наличия ключа на устройстве юзера
    private String date;
    private Coordinate coords; //координаты(широта, долгота)

    public Mark() {
    }

    public Mark(String message, User author, boolean isAnonymed, boolean isEncrypted, String date, Coordinate coords) {
        this.message = message;
        this.author = author;
        this.isAnonymed = isAnonymed;
        this.isEncrypted = isEncrypted;
        this.date = date;
        this.coords = coords;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public boolean isAnonymed() {
        return isAnonymed;
    }

    public void setAnonymed(boolean anonymed) {
        isAnonymed = anonymed;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Coordinate getCoords() {
        return coords;
    }

    public void setCoords(Coordinate coords) {
        this.coords = coords;
    }
}
