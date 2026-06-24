package org.esupportail.esupsignature.dto.ui.global;

public class UiUserLookupDto {

    private String email;
    private String firstname;
    private String name;
    private String hidedPhone;

    public UiUserLookupDto() {
    }

    public UiUserLookupDto(String email, String firstname, String name, String hidedPhone) {
        this.email = email;
        this.firstname = firstname;
        this.name = name;
        this.hidedPhone = hidedPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getHidedPhone() {
        return hidedPhone;
    }

    public void setHidedPhone(String hidedPhone) {
        this.hidedPhone = hidedPhone;
    }

    public String email() { return email; }
    public String firstname() { return firstname; }
    public String name() { return name; }
    public String hidedPhone() { return hidedPhone; }
}
