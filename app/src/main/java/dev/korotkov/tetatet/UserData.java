package dev.korotkov.tetatet;

import java.util.ArrayList;

public class UserData {

    private String name;
    private Integer age;
    private String avatarId;
    private String description;
    private ArrayList<String> interestsIds;
    private ArrayList<String> languagesIds;

    public UserData() { }

    public UserData(String name, Integer age, String avatarId, String description, ArrayList<String> interestsIds, ArrayList<String> languagesIds) {
        this.name = name;
        this.age = age;
        this.avatarId = avatarId;
        this.description = description;
        this.interestsIds = interestsIds;
        this.languagesIds = languagesIds;
    }

    public UserData(String name, Integer age, String avatarId, ArrayList<String> interestsIds, ArrayList<String> languagesIds) {
        this.name = name;
        this.age = age;
        this.avatarId = avatarId;
        this.description = "";
        this.interestsIds = interestsIds;
        this.languagesIds = languagesIds;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public String getAvatarId() {
        return avatarId;
    }

    public String getDescription() {
        return description;
    }

    public ArrayList<String> getInterestsIds() {
        return interestsIds;
    }

    public ArrayList<String> getLanguagesIds() {
        return languagesIds;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setAvatarId(String avatarId) {
        this.avatarId = avatarId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setInterestsIds(ArrayList<String> interestsIds) {
        this.interestsIds = interestsIds;
    }

    public void setLanguagesIds(ArrayList<String> languagesIds) {
        this.languagesIds = languagesIds;
    }
}
