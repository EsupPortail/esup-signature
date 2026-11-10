package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.dto.ui.global.UiMessageDto;
import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

class FormAdminControllerTest {

    @ParameterizedTest
    @CsvSource({
            "ROLE_ADMIN,/admin,false,admin.forms.trash.moved",
            "ROLE_ADMIN,/admin,true,admin.forms.trash.deleted",
            "ROLE_MANAGER,/manager,false,admin.forms.trash.moved",
            "ROLE_MANAGER,/manager,true,admin.forms.trash.deleted"
    })
    void redirectsToListAndDistinguishesDeletionStages(String role, String prefix, boolean deleted, String messageKey) throws Exception {
        FormService formService = mock(FormService.class);
        UserService userService = mock(UserService.class);
        Form form = new Form();
        form.setDeleted(deleted);
        when(formService.getById(7705L)).thenReturn(form);
        when(userService.getRoles("user@example.org")).thenReturn(Set.of(role));
        StaticMessageSource messages = new StaticMessageSource();
        messages.addMessage(messageKey, Locale.FRENCH, messageKey);
        FormAdminController controller = new FormAdminController(formService, userService, null, null, null, null, null, null, null, messages);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(delete(prefix + "/forms/7705").locale(Locale.FRENCH)
                        .flashAttr("authUserEppn", "user@example.org"))
                .andExpect(redirectedUrl(prefix + "/forms"))
                .andExpect(result -> {
                    UiMessageDto message = (UiMessageDto) result.getFlashMap().get("message");
                    assertEquals(messageKey, message.getText());
                });
        verify(formService).deleteForm(7705L);
    }
}
