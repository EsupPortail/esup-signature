package org.esupportail.esupsignature.dto.ui.global;

import java.util.List;
import java.util.Map;

public class UiCurrentUserDto {

    private UiUserDto user;
    private UiUserDto authUser;
    private List<SuUserDto> suUsers;
    private List<Long> userImagesIds;
    private String keystoreFileName;
    private Map<String, String> uiParams;
    private String securityServiceName;

    public UiCurrentUserDto() {
    }

    public UiCurrentUserDto(UiUserDto user,
                            UiUserDto authUser,
                            List<SuUserDto> suUsers,
                            List<Long> userImagesIds,
                            String keystoreFileName,
                            Map<String, String> uiParams,
                            String securityServiceName) {
        this.user = user;
        this.authUser = authUser;
        this.suUsers = suUsers;
        this.userImagesIds = userImagesIds;
        this.keystoreFileName = keystoreFileName;
        this.uiParams = uiParams;
        this.securityServiceName = securityServiceName;
    }

    public UiUserDto getUser() { return user; }
    public void setUser(UiUserDto user) { this.user = user; }
    public UiUserDto getAuthUser() { return authUser; }
    public void setAuthUser(UiUserDto authUser) { this.authUser = authUser; }
    public List<SuUserDto> getSuUsers() { return suUsers; }
    public void setSuUsers(List<SuUserDto> suUsers) { this.suUsers = suUsers; }
    public List<Long> getUserImagesIds() { return userImagesIds; }
    public void setUserImagesIds(List<Long> userImagesIds) { this.userImagesIds = userImagesIds; }
    public String getKeystoreFileName() { return keystoreFileName; }
    public void setKeystoreFileName(String keystoreFileName) { this.keystoreFileName = keystoreFileName; }
    public Map<String, String> getUiParams() { return uiParams; }
    public void setUiParams(Map<String, String> uiParams) { this.uiParams = uiParams; }
    public String getSecurityServiceName() { return securityServiceName; }
    public void setSecurityServiceName(String securityServiceName) { this.securityServiceName = securityServiceName; }

    public UiUserDto user() { return user; }
    public UiUserDto authUser() { return authUser; }
    public List<SuUserDto> suUsers() { return suUsers; }
    public List<Long> userImagesIds() { return userImagesIds; }
    public String keystoreFileName() { return keystoreFileName; }
    public Map<String, String> uiParams() { return uiParams; }
    public String securityServiceName() { return securityServiceName; }

    public static class UiUserDto {
        private Long id;
        private String eppn;
        private String firstname;
        private String name;
        private String email;
        private String userType;
        private Integer defaultSignImageNumber;
        private List<String> roles;

        public UiUserDto() {
        }

        public UiUserDto(Long id, String eppn, String firstname, String name, String email,
                         String userType, Integer defaultSignImageNumber, List<String> roles) {
            this.id = id;
            this.eppn = eppn;
            this.firstname = firstname;
            this.name = name;
            this.email = email;
            this.userType = userType;
            this.defaultSignImageNumber = defaultSignImageNumber;
            this.roles = roles;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getEppn() { return eppn; }
        public void setEppn(String eppn) { this.eppn = eppn; }
        public String getFirstname() { return firstname; }
        public void setFirstname(String firstname) { this.firstname = firstname; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
        public Integer getDefaultSignImageNumber() { return defaultSignImageNumber; }
        public void setDefaultSignImageNumber(Integer defaultSignImageNumber) { this.defaultSignImageNumber = defaultSignImageNumber; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }

        public Long id() { return id; }
        public String eppn() { return eppn; }
        public String firstname() { return firstname; }
        public String name() { return name; }
        public String email() { return email; }
        public String userType() { return userType; }
        public Integer defaultSignImageNumber() { return defaultSignImageNumber; }
        public List<String> roles() { return roles; }
    }

    public static class SuUserDto {
        private String eppn;
        private String firstname;
        private String name;
        private Long userShareId;

        public SuUserDto() {
        }

        public SuUserDto(String eppn, String firstname, String name, Long userShareId) {
            this.eppn = eppn;
            this.firstname = firstname;
            this.name = name;
            this.userShareId = userShareId;
        }

        public String getEppn() { return eppn; }
        public void setEppn(String eppn) { this.eppn = eppn; }
        public String getFirstname() { return firstname; }
        public void setFirstname(String firstname) { this.firstname = firstname; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getUserShareId() { return userShareId; }
        public void setUserShareId(Long userShareId) { this.userShareId = userShareId; }

        public String eppn() { return eppn; }
        public String firstname() { return firstname; }
        public String name() { return name; }
        public Long userShareId() { return userShareId; }
    }
}
