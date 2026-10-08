package org.esupportail.esupsignature.web.ws;

import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.LiveWorkflowStepService;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.SignBookService;
import org.esupportail.esupsignature.service.SignRequestParamsService;
import org.esupportail.esupsignature.service.SignRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SignRequestWsControllerTest {

    @Test
    void lastFileReturnsNotFoundWhenSignRequestDoesNotExist() throws Exception {
        SignRequestRepository repository = mock(SignRequestRepository.class);
        when(repository.findById(1311953L)).thenReturn(Optional.empty());
        SignRequestService service = mock(SignRequestService.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "signRequestRepository", repository);
        SignRequestWsController controller = new SignRequestWsController(
                service, mock(RecipientService.class), mock(SignBookService.class),
                mock(SignRequestParamsService.class), mock(LiveWorkflowStepService.class));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/ws/signrequests/get-last-file/1311953"))
                .andExpect(status().isNotFound());
    }
}
