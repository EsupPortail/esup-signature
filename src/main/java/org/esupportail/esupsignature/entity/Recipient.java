package org.esupportail.esupsignature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.esupportail.esupsignature.dto.json.RecipientWsDto;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(indexes =  {
        @Index(name = "recipient_user_id", columnList = "user_id")
})
public class Recipient {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private Boolean signed = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Boolean getSigned() {
        return signed;
    }

    public void setSigned(Boolean signed) {
        this.signed = signed;
    }

    @JsonIgnore
    public RecipientWsDto getRecipientDto() {
        RecipientWsDto recipientWsDto = new RecipientWsDto();
        recipientWsDto.setId(id);
        if (user != null) {
            recipientWsDto.setEmail(user.getEmail());
            recipientWsDto.setPhone(user.getPhone());
            recipientWsDto.setName(user.getName());
            recipientWsDto.setFirstName(user.getFirstname());
        }
        return recipientWsDto;
    }
}
