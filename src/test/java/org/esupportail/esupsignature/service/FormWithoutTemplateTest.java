package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Form;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.web.controller.admin.FormAdminController;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class FormWithoutTemplateTest {
    @Test
    void createsWithoutDocumentForEmptyAndAbsentFiles() throws Exception {
        verifyCreation(null);
        verifyCreation(new MockMultipartFile("multipartFile", new byte[0]));
    }

    private void verifyCreation(MultipartFile file) throws Exception {
        DocumentService documentService = mock(DocumentService.class);
        FormService service = spy(new FormService(null, null, null, null, null, null,
                documentService, null, null, null, null, null, null, null, null));
        Form form = new Form();
        doReturn(form).when(service).createForm(null, "name", "title", 1L, "none",
                null, false, null, null, "user", null);
        assertSame(form, service.generateForm(file, "name", "title", 1L, "none", null, false, "user", null));
        verifyNoInteractions(documentService);
        verify(service, never()).updateSignRequestParams(any(), any());
    }

    @Test
    void redirectsToSettingsWithoutDocumentAndToFieldsWithDocument() throws Exception {
        verifyRedirect(null, "ROLE_ADMIN", "redirect:/admin/forms/update/42");
        verifyRedirect(null, "ROLE_MANAGER", "redirect:/manager/forms/update/42");
        verifyRedirect(new Document(), "ROLE_ADMIN", "redirect:/admin/forms/42/fields");
        verifyRedirect(new Document(), "ROLE_MANAGER", "redirect:/manager/forms/42/fields");
    }

    private void verifyRedirect(Document document, String role, String expected) throws Exception {
        FormService service = mock(FormService.class);
        UserService userService = mock(UserService.class);
        Form form = new Form();
        form.setId(42L);
        form.setDocument(document);
        when(service.generateForm(null, "name", "title", 1L, "none", null, false, "user", null)).thenReturn(form);
        when(userService.getRoles("user")).thenReturn(Set.of(role));
        FormAdminController controller = new FormAdminController(service, userService, null, null, null, null, null, null, null);
        assertEquals(expected, controller.generate("user", null, "name", "title", 1L, "none", null,
                null, false, new RedirectAttributesModelMap()));
    }
}
