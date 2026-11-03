package org.esupportail.esupsignature.web.controller.user;

import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.interfaces.listsearch.UserListService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerListTest {

    @Test
    void listResponseAlwaysContainsJsonAndFailuresHaveAnErrorStatus() throws Exception {
        UserListService service = mock(UserListService.class);
        UserController controller = new UserController(null, null, null, null, null, null, null,
                service, null, null, null, null, null);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(service.getUsersEmailFromList("team@example.org")).thenReturn(List.of("alice@example.org"));
        mvc.perform(get("/user/users/search-user-list").param("searchString", "team@example.org"))
                .andExpect(status().isOk()).andExpect(content().json("[\"alice@example.org\"]"));
        when(service.getUsersEmailFromList("person@example.org")).thenReturn(List.of());
        mvc.perform(get("/user/users/search-user-list").param("searchString", "person@example.org"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        when(service.getUsersEmailFromList("team@example.org")).thenThrow(new EsupSignatureRuntimeException("error.sympa.soap"));
        mvc.perform(get("/user/users/search-user-list").param("searchString", "team@example.org"))
                .andExpect(status().isInternalServerError()).andExpect(content().json("[]"));
    }
}
