package org.esupportail.esupsignature.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.config.sms.SmsProperties;
import org.esupportail.esupsignature.entity.LiveWorkflow;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.Workflow;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.repository.WorkflowRepository;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessFactoryService;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.mail.MailService;
import org.esupportail.esupsignature.service.security.otp.OtpService;
import org.esupportail.esupsignature.service.utils.WebUtilsService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.esupportail.esupsignature.service.utils.sign.SignService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SignBookServiceUpdateRightsTest {

    private final SignBookRepository signBookRepository = mock(SignBookRepository.class);
    private final UserService userService = mock(UserService.class);
    private SignBookService signBookService;

    @BeforeEach
    void setUp() {
        signBookService = new SignBookService(
                mock(GlobalProperties.class),
                mock(MessageSource.class),
                mock(AuditTrailService.class),
                signBookRepository,
                mock(SignRequestService.class),
                userService,
                mock(FsAccessFactoryService.class),
                mock(WebUtilsService.class),
                mock(FileService.class),
                mock(PdfService.class),
                mock(WorkflowService.class),
                mock(MailService.class),
                mock(WorkflowStepService.class),
                mock(LiveWorkflowService.class),
                mock(LiveWorkflowStepService.class),
                mock(DataService.class),
                mock(LogService.class),
                mock(TargetService.class),
                mock(UserPropertieService.class),
                mock(CommentService.class),
                mock(OtpService.class),
                mock(DataRepository.class),
                mock(WorkflowRepository.class),
                mock(UserShareService.class),
                mock(RecipientService.class),
                mock(DocumentService.class),
                mock(SignRequestParamsService.class),
                mock(PreFillService.class),
                mock(ReportService.class),
                mock(ActionService.class),
                mock(SignRequestParamsRepository.class),
                new ObjectMapper(),
                mock(SignWithService.class),
                mock(SmsProperties.class),
                mock(SignService.class)
        );
    }

    @Test
    void checkUserUpdateRightsShouldDenyCreatorWhenWorkflowDisablesUpdate() {
        User creator = user("creator", "creator@example.org");
        Workflow workflow = workflow(true);
        SignBook signBook = signBook(creator, workflow);

        when(signBookRepository.findById(1L)).thenReturn(Optional.of(signBook));
        when(userService.getByEppn("creator")).thenReturn(creator);

        assertFalse(signBookService.checkUserUpdateRights(1L, "creator"));
    }

    @Test
    void checkUserUpdateRightsShouldAllowCreatorWhenWorkflowDoesNotDisableUpdate() {
        User creator = user("creator", "creator@example.org");
        Workflow workflow = workflow(false);
        SignBook signBook = signBook(creator, workflow);

        when(signBookRepository.findById(1L)).thenReturn(Optional.of(signBook));
        when(userService.getByEppn("creator")).thenReturn(creator);

        assertTrue(signBookService.checkUserUpdateRights(1L, "creator"));
    }

    @Test
    void checkUserUpdateRightsShouldAllowWorkflowManagerEvenWhenCreatorUpdatesAreDisabled() {
        User creator = user("creator", "creator@example.org");
        User manager = user("manager", "manager@example.org");
        Workflow workflow = workflow(true);
        workflow.setManagers(new HashSet<>(Set.of(manager.getEmail())));
        SignBook signBook = signBook(creator, workflow);

        when(signBookRepository.findById(1L)).thenReturn(Optional.of(signBook));
        when(userService.getByEppn("manager")).thenReturn(manager);

        assertTrue(signBookService.checkUserUpdateRights(1L, "manager"));
    }

    @Test
    void checkUserUpdateRightsShouldAllowAdminEvenWhenCreatorUpdatesAreDisabled() {
        User creator = user("creator", "creator@example.org");
        User admin = user("admin", "admin@example.org");
        admin.setRoles(new HashSet<>(Set.of("ROLE_ADMIN")));
        Workflow workflow = workflow(true);
        SignBook signBook = signBook(creator, workflow);

        when(signBookRepository.findById(1L)).thenReturn(Optional.of(signBook));
        when(userService.getByEppn("admin")).thenReturn(admin);

        assertTrue(signBookService.checkUserUpdateRights(1L, "admin"));
    }

    private SignBook signBook(User creator, Workflow workflow) {
        SignBook signBook = new SignBook();
        signBook.setId(1L);
        signBook.setCreateBy(creator);
        signBook.setSignRequests(new ArrayList<>(java.util.List.of(new SignRequest())));
        LiveWorkflow liveWorkflow = new LiveWorkflow();
        liveWorkflow.setWorkflow(workflow);
        signBook.setLiveWorkflow(liveWorkflow);
        return signBook;
    }

    private Workflow workflow(boolean disableUpdateByCreator) {
        Workflow workflow = new Workflow();
        workflow.setDisableUpdateByCreator(disableUpdateByCreator);
        workflow.setManagers(new HashSet<>());
        workflow.setDashboardRoles(new HashSet<>());
        return workflow;
    }

    private User user(String eppn, String email) {
        User user = new User();
        user.setEppn(eppn);
        user.setEmail(email);
        user.setRoles(new HashSet<>());
        return user;
    }
}

