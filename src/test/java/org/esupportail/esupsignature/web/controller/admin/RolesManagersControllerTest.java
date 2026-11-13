package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.service.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class RolesManagersControllerTest {

    @ParameterizedTest
    @CsvSource({"ROLE_ADMIN,/admin", "ROLE_MANAGER,/manager"})
    void exposesProfilePrefixAndRedirectsAfterChanges(String role, String prefix) throws Exception {
        UserService userService = mock(UserService.class);
        when(userService.getRoles("user@example.org")).thenReturn(Set.of(role));
        RolesManagersController controller = new RolesManagersController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get(prefix + "/roles-managers").flashAttr("authUserEppn", "user@example.org"))
                .andExpect(view().name("admin/roles-managers"))
                .andExpect(model().attribute("urlPrefix", prefix));
        mockMvc.perform(post(prefix + "/roles-managers/edit-role").flashAttr("authUserEppn", "user@example.org")
                        .param("role", "ROLE_USER").param("rolesManagers", "manager@example.org"))
                .andExpect(redirectedUrl(prefix + "/roles-managers"));
        verify(userService).updateRole("ROLE_USER", List.of("manager@example.org"));
        mockMvc.perform(post(prefix + "/roles-managers/create-role").flashAttr("authUserEppn", "user@example.org")
                        .param("role", "ROLE_USER").param("rolesManagers", "manager@example.org"))
                .andExpect(redirectedUrl(prefix + "/roles-managers"));
        verify(userService).createRole("ROLE_USER", List.of("manager@example.org"));
    }
}
