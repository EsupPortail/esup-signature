package org.esupportail.esupsignature.dto.ui.global;

import java.util.List;

public class UserSignatureStateDto {

    private String firstname;
    private String name;
    private String email;
    private List<Long> signImageIds;
    private List<String> signImages;
    private String defaultSignImage;

    public UserSignatureStateDto() {
    }

    public UserSignatureStateDto(String firstname, String name, String email, List<Long> signImageIds, List<String> signImages, String defaultSignImage) {
        this.firstname = firstname;
        this.name = name;
        this.email = email;
        this.signImageIds = signImageIds;
        this.signImages = signImages;
        this.defaultSignImage = defaultSignImage;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Long> getSignImageIds() {
        return signImageIds;
    }

    public void setSignImageIds(List<Long> signImageIds) {
        this.signImageIds = signImageIds;
    }

    public List<String> getSignImages() {
        return signImages;
    }

    public void setSignImages(List<String> signImages) {
        this.signImages = signImages;
    }

    public String getDefaultSignImage() {
        return defaultSignImage;
    }

    public void setDefaultSignImage(String defaultSignImage) {
        this.defaultSignImage = defaultSignImage;
    }
}


