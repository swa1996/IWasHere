package ru.isu.swa.diplom.model;

import java.util.Set;

/**
 * Created by swa on 10.12.2017.
 */

//Группа пользователей
public class Group {
    private Integer id;
    private String name;
    private Set<User> administrators;
    private Set<User> members;

    public Group() {
    }

    public Group(String name, Set<User> administrators, Set<User> members) {
        this.name = name;
        this.administrators = administrators;
        this.members = members;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<User> getAdministrators() {
        return administrators;
    }

    public void setAdministrators(Set<User> administrators) {
        this.administrators = administrators;
    }

    public Set<User> getMembers() {
        return members;
    }

    public void setMembers(Set<User> members) {
        this.members = members;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return id.equals(group.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
