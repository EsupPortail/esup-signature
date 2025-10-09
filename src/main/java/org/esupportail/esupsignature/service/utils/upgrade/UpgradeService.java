package org.esupportail.esupsignature.service.utils.upgrade;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.lang3.BooleanUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.ArchiveStatus;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.AppliVersionRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UpgradeService {

    private static final Logger logger = LoggerFactory.getLogger(UpgradeService.class);

    @PersistenceContext
    private final EntityManager entityManager;
    private final GlobalProperties globalProperties;
    private final SignBookRepository signBookRepository;
    private final AppliVersionRepository appliVersionRepository;
    private final BuildProperties buildProperties;
    private final FileService fileService;
    private final FormService formService;

    private final String[] updates = new String[] {"1.19", "1.22", "1.23", "1.29.10", "1.30.5", "1.33.7", "1.34.0", "1.34.4"};

    public UpgradeService(EntityManager entityManager, GlobalProperties globalProperties, SignBookRepository signBookRepository, AppliVersionRepository appliVersionRepository, @Autowired(required = false) BuildProperties buildProperties, FileService fileService, FormService formService) {
        this.entityManager = entityManager;
        this.globalProperties = globalProperties;
        this.signBookRepository = signBookRepository;
        this.appliVersionRepository = appliVersionRepository;
        this.buildProperties = buildProperties;
        this.fileService = fileService;
        this.formService = formService;
    }

    @Transactional
    public void launch() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        logger.info("##### Esup-signature Upgrade #####");
        for(String update : updates) {
            if(checkVersionUpToDate(update) < 0) {
                logger.info("#### Starting update : " + update + " ####");
                Method method = UpgradeService.class.getDeclaredMethod("update_" + update.replaceAll("\\.", "_"));
                method.invoke(this);
                List<AppliVersion> appliVersions = new ArrayList<>();
                appliVersionRepository.findAll().forEach(appliVersions::add);
                if(!appliVersions.isEmpty()) {
                    appliVersions.get(0).setEsupSignatureVersion(update);
                } else {
                    AppliVersion appliVersion = new AppliVersion(update);
                    appliVersionRepository.save(appliVersion);
                }
            } else {
                logger.info("##### Esup-signature is higher than " + update + ", skip update #####");
            }
        }
        logger.info("##### Esup-signature is up-to-date #####");
    }

    public void checkVersion() {
        Thread checkVersion = new Thread(() -> {
            try {
                RestTemplate restTemplate = new RestTemplate();
                LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
                HttpHeaders headers = new HttpHeaders();
                headers.add("Referer", globalProperties.getDomain());
                String currentVersion = "0.0.0";
                if(buildProperties != null) {
                    currentVersion = buildProperties.getVersion();
                }
                headers.add("X-API-Version", currentVersion);
                HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
                String version = restTemplate.postForObject("https://esup-signature-demo.univ-rouen.fr/webhook", requestEntity, String.class);
                logger.info("##### Esup-signature last version : " + version + " #####");
                if (version != null && compareVersions(version.trim(), currentVersion) >= 0) {
                    logger.info("##### Esup-signature version is up-to-date #####");
                } else {
                    logger.info("##### Esup-signature version is not up-to-date #####");
                    globalProperties.newVersion = version;
                }
            } catch (Exception e) {
                logger.info("##### Unable to get last version #####", e);
            }
        });
        checkVersion.start();
    }

    private int checkVersionUpToDate(String updateVersion) {
        List<AppliVersion> appliVersions = new ArrayList<>();
        appliVersionRepository.findAll().forEach(appliVersions::add);
        if(appliVersions.isEmpty()) return -1;
        String databaseVersion = appliVersions.get(0).getEsupSignatureVersion().split("-")[0];
        return compareVersions(updateVersion, databaseVersion);
    }

    private static int compareVersions(String targetVersion, String currentVersion) {
        String[] codeVersionStrings = targetVersion.split("\\.");
        String[] databaseVersionStrings = currentVersion.split("-")[0].split("\\.");
        int length = Math.max(codeVersionStrings.length, databaseVersionStrings.length);

        for(int i = 0; i < length; i++) {
            int thisPart = i < codeVersionStrings.length ?
                    Integer.parseInt(codeVersionStrings[i]) : 0;
            int thatPart = i < databaseVersionStrings.length ?
                    Integer.parseInt(databaseVersionStrings[i]) : 0;
            if(thisPart < thatPart)
                return 1;
            if(thisPart > thatPart)
                return -1;
        }
        return 0;
    }

//    @SuppressWarnings("unused")
//    public void update_1_23_16() {
//        List<Document> documents = documentService.getAll().stream().filter(document -> document.getNbPages() == null).collect(Collectors.toList());
//        logger.info("#### Starting update documents nbPages for " + documents.size() + " documents ####");
//        for(Document document : documents) {
//            if(document.getContentType() != null && document.getContentType().equals("application/pdf") && document.getNbPages() == null) {
//                try {
//                    documentService.updateNbPages(document);
//                } catch (IOException e) {
//                    logger.warn("Document " + document.getId() + " : " + e.getMessage());
//                }
//            }
//        }
//        logger.info("#### Update update documents nbPages completed ####");
//    }

    @SuppressWarnings("unused")
    public void update_1_23() {
        logger.info("#### Starting update end dates of refused signBooks ####");
        List<SignBook> signBooks = signBookRepository.findAll(Pageable.unpaged()).getContent();
        for(SignBook signBook : signBooks.stream().filter(signBook -> signBook.getEndDate() == null && signBook.getStatus().equals(SignRequestStatus.refused)).toList()) {
            List<Action> actions = signBook.getSignRequests().stream().map(SignRequest::getRecipientHasSigned).map(Map::values).flatMap(Collection::stream).filter(action -> action.getDate() != null).sorted(Comparator.comparing(Action::getDate).reversed()).collect(Collectors.toList());
            if(!actions.isEmpty()) {
                signBook.setEndDate(actions.get(0).getDate());
            }
        }
        logger.info("#### Update end dates of refused signBooks completed ####");
    }

    @SuppressWarnings("unused")
    public void update_1_22() {
        logger.info("#### Starting update end dates of signBooks ####");
        List<SignBook> signBooks = signBookRepository.findAll(Pageable.unpaged()).getContent();
        for(SignBook signBook : signBooks.stream().filter(signBook -> signBook.getEndDate() == null).toList()) {
            if((signBook.getStatus().equals(SignRequestStatus.completed)
                    || signBook.getStatus().equals(SignRequestStatus.exported)
                    || signBook.getStatus().equals(SignRequestStatus.refused)
                    || signBook.getStatus().equals(SignRequestStatus.signed)
                    || signBook.getDeleted())) {
                List<Action> actions = signBook.getSignRequests().stream().map(SignRequest::getRecipientHasSigned).map(Map::values).flatMap(Collection::stream).filter(action -> action.getDate() != null).sorted(Comparator.comparing(Action::getDate).reversed()).collect(Collectors.toList());
                if(!actions.isEmpty()) {
                    signBook.setEndDate(actions.get(0).getDate());
                }
            }
        }
        logger.info("#### Update end dates of signBooks completed ####");
        logger.info("#### Starting update manager of workflows ####");
        List<Form> forms = formService.getAllForms();
        for(Form form : forms) {
            for(String manager : form.getManagers()) {
                if(form.getWorkflow() != null && !form.getWorkflow().getManagers().contains(manager)) {
                    form.getWorkflow().getManagers().add(manager);
                }
            }
        }
        logger.info("#### Update manager of workflows completed ####");
    }


    @SuppressWarnings("unused")
    public void update_1_19() {
        logger.info("#### Starting update subjets and workflowNames ####");
        List<SignBook> signBooks = signBookRepository.findBySubject(null);
        if(!signBooks.isEmpty()) {
            for(SignBook signBook : signBooks) {
                if(signBook.getSubject() == null) {
                    if(signBook.getTitle() != null
                            && !signBook.getTitle().isEmpty()
                            && !signBook.getTitle().equals("Auto signature")
                            && !signBook.getTitle().equals("Signature simple")
                            && !signBook.getTitle().equals("Demande simple")
                            && !signBook.getTitle().equals("Demande personnalisée")
                            && signBook.getTitle().replaceAll("\\W+", "_").equals(signBook.getName())) {
                        signBook.setSubject(signBook.getTitle());
                    } else {
                        if(signBook.getName().isEmpty()) {
                            if(!signBook.getSignRequests().isEmpty()) {
                                if(signBook.getSignRequests().get(0).getTitle() != null && signBook.getSignRequests().get(0).getTitle().isEmpty()) {
                                    signBook.setSubject(signBook.getSignRequests().get(0).getTitle());
                                } else {
                                    if(!signBook.getSignRequests().get(0).getOriginalDocuments().isEmpty()) {
                                        signBook.setSubject(fileService.getNameOnly(signBook.getSignRequests().get(0).getOriginalDocuments().get(0).getFileName()));
                                    } else {
                                        signBook.setSubject("Sans titre");
                                    }
                                }
                            } else {
                                signBook.setSubject("Sans titre");
                            }
                        } else {
                            if(!signBook.getTitle().equals(signBook.getName())
                                    && !signBook.getName().equals("Auto signature".replaceAll("\\W+", "_"))
                                    && !signBook.getName().equals("Signature simple".replaceAll("\\W+", "_"))
                                    && !signBook.getName().equals("Demande simple".replaceAll("\\W+", "_"))
                                    && !signBook.getName().equals("Demande personnalisée".replaceAll("\\W+", "_"))) {
                                signBook.setSubject(signBook.getName());
                            } else {
                                if(!signBook.getSignRequests().isEmpty()) {
                                    if(signBook.getSignRequests().get(0).getTitle() != null && signBook.getSignRequests().get(0).getTitle().isEmpty()) {
                                        signBook.setSubject(signBook.getSignRequests().get(0).getTitle());
                                    } else {
                                        if(!signBook.getSignRequests().get(0).getOriginalDocuments().isEmpty()) {
                                            signBook.setSubject(fileService.getNameOnly(signBook.getSignRequests().get(0).getOriginalDocuments().get(0).getFileName()));
                                        } else {
                                            signBook.setSubject("Sans titre");
                                        }
                                    }
                                } else {
                                    signBook.setSubject("Sans titre");
                                }
                            }
                        }
                    }
                }
                if(signBook.getWorkflowName() == null) {
                    if(signBook.getTitle().equals("Auto signature") || signBook.getTitle().equals("Signature simple") || signBook.getTitle().equals("Demande simple") || signBook.getTitle().equals("Demande personnalisée")) {
                        if(signBook.getTitle().equals("Signature simple")) {
                            signBook.setWorkflowName("Auto signature");
                        } else {
                            signBook.setWorkflowName(signBook.getTitle());
                        }
                    } else {
                        if(signBook.getLiveWorkflow().getTitle() != null && !signBook.getLiveWorkflow().getTitle().isEmpty()) {
                            signBook.setWorkflowName(signBook.getLiveWorkflow().getTitle());
                        } else {
                            if(signBook.getLiveWorkflow().getWorkflow() != null) {
                                if(signBook.getLiveWorkflow().getWorkflow().getDescription() != null) {
                                    signBook.setWorkflowName(signBook.getLiveWorkflow().getWorkflow().getDescription());
                                } else if(signBook.getLiveWorkflow().getWorkflow().getName() != null) {
                                    signBook.setWorkflowName(signBook.getLiveWorkflow().getWorkflow().getName());
                                } else {
                                    signBook.setWorkflowName("Sans nom");
                                }
                            } else {
                                signBook.setWorkflowName("Sans nom");
                            }
                        }
                    }
                }
            }
            logger.info("#### Update subjets and workflowNames completed ####");
        } else {
            logger.info("#### Update subjets and workflowNames skipped ####");
        }
    }

    @SuppressWarnings("unused")
    public void update_1_29_10() {
        logger.info("#### Starting update deleted flag ####");
        entityManager.createNativeQuery("update sign_request set deleted = true where status = 'deleted'").executeUpdate();
        entityManager.createNativeQuery("update sign_book set deleted = true where status = 'deleted'").executeUpdate();
        logger.info("#### Update deleted flag completed ####");
    }

    @SuppressWarnings("unused")
    public void update_1_30_5() {
        logger.info("#### Starting update signRequestParams ####");
        entityManager.createNativeQuery("alter table sign_request_params alter column x_pos drop not null").executeUpdate();
        entityManager.createNativeQuery("alter table sign_request_params alter column y_pos drop not null").executeUpdate();
        logger.info("#### Update signRequestParams completed ####");
    }

    @SuppressWarnings("unused")
    public void update_1_33_7() {
        logger.info("#### Starting update workflow ####");
        entityManager.createNativeQuery(
                "DO $$ BEGIN " +
                        "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'workflow' AND column_name = 'autorize_clone') THEN " +
                        "UPDATE workflow SET authorize_clone = autorize_clone WHERE authorize_clone IS NULL; " +
                        "ALTER TABLE workflow DROP COLUMN autorize_clone; " +
                        "END IF; " +
                        "END $$;"
        ).executeUpdate();

        entityManager.createNativeQuery(
                "DO $$ DECLARE " +
                        "rec RECORD; " +
                        "base TEXT; " +
                        "suffix INT; " +
                        "new_token TEXT; " +
                        "BEGIN " +
                        "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'workflow' AND column_name = 'title') THEN " +
                        "FOR rec IN SELECT id, title FROM workflow LOOP " +
                        "IF rec.title IS NOT NULL THEN " +
                        "base := rec.title; " +
                        "suffix := 1; " +
                        "new_token := base; " +
                        "WHILE EXISTS (SELECT 1 FROM workflow WHERE token = new_token) LOOP " +
                        "new_token := base || '_' || suffix; " +
                        "suffix := suffix + 1; " +
                        "END LOOP; " +
                        "UPDATE workflow SET token = new_token WHERE id = rec.id; " +
                        "END IF; " +
                        "END LOOP; " +
                        "ALTER TABLE workflow DROP COLUMN title; " +
                        "END IF; " +
                        "END $$;"
        ).executeUpdate();
        logger.info("#### Update workflow completed ####");
    }

    @SuppressWarnings("unused")
    public void update_1_34_0() {
        logger.info("#### Starting update archive status ####");

        List<SignBook> signBooks = signBookRepository.findByStatus(SignRequestStatus.archived);
        signBooks.addAll(signBookRepository.findByStatus(SignRequestStatus.cleaned));

        for(SignBook signBook : signBooks) {
            if(signBook.getStatus().equals(SignRequestStatus.archived)) signBook.setArchiveStatus(ArchiveStatus.archived);
            if(signBook.getStatus().equals(SignRequestStatus.cleaned)) signBook.setArchiveStatus(ArchiveStatus.cleaned);
            for(SignRequest signRequest : signBook.getSignRequests()) {
                if(signRequest.getStatus().equals(SignRequestStatus.archived)) signRequest.setArchiveStatus(ArchiveStatus.archived);
                if(signRequest.getStatus().equals(SignRequestStatus.cleaned)) signRequest.setArchiveStatus(ArchiveStatus.cleaned);
                if(signRequest.getRecipientHasSigned().values().stream().anyMatch(a -> a.getActionType().equals(ActionType.refused))) {
                    signRequest.setStatus(SignRequestStatus.refused);
                } else if(!signBook.getLiveWorkflow().getTargets().isEmpty() && signBook.getLiveWorkflow().getTargets().stream().allMatch(Target::getTargetOk)){
                    signRequest.setStatus(SignRequestStatus.exported);
                } else {
                    signRequest.setStatus(SignRequestStatus.completed);
                }
            }
            if((BooleanUtils.isTrue(signBook.getForceAllDocsSign()) || signBook.getSignRequests().size() == 1) && signBook.getSignRequests().stream().anyMatch(sr -> sr.getRecipientHasSigned().values().stream().anyMatch(a -> a.getActionType().equals(ActionType.refused)))) {
                signBook.setStatus(SignRequestStatus.refused);
            } else if(!signBook.getLiveWorkflow().getTargets().isEmpty() && signBook.getLiveWorkflow().getTargets().stream().allMatch(Target::getTargetOk)){
                signBook.setStatus(SignRequestStatus.exported);
            } else {
                signBook.setStatus(SignRequestStatus.completed);
            }
        }
        logger.info("#### Update archive status completed ####");
    }

    @SuppressWarnings("unused")
    public void update_1_34_4() {
        logger.info("#### Starting update sign types ####");
        entityManager.createNativeQuery("""
        
                        DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM pg_constraint
                    WHERE conname = 'workflow_step_repeatable_sign_type_check'
                      AND conrelid = 'public.workflow_step'::regclass
                ) THEN
                    ALTER TABLE public.workflow_step
                        DROP CONSTRAINT workflow_step_repeatable_sign_type_check;
                END IF;
            END;
        $$;
        
        DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM pg_constraint
                    WHERE conname = 'workflow_step_sign_type_check'
                      AND conrelid = 'public.workflow_step'::regclass
                ) THEN
                    ALTER TABLE public.workflow_step
                        DROP CONSTRAINT workflow_step_sign_type_check;
                END IF;
            END;
        $$;
        
        DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM pg_constraint
                    WHERE conname = 'live_workflow_step_repeatable_sign_type_check'
                      AND conrelid = 'public.live_workflow_step'::regclass
                ) THEN
                    ALTER TABLE public.live_workflow_step
                        DROP CONSTRAINT live_workflow_step_repeatable_sign_type_check;
                END IF;
            END;
        $$;
        
        DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM pg_constraint
                    WHERE conname = 'live_workflow_step_sign_type_check'
                      AND conrelid = 'public.live_workflow_step'::regclass
                ) THEN
                    ALTER TABLE public.live_workflow_step
                        DROP CONSTRAINT live_workflow_step_sign_type_check;
                END IF;
            END;
        $$;
        
        update live_workflow_step set min_sign_level = 'advanced' where repeatable_sign_type = 'certSign' or repeatable_sign_type = 'nexuSign';
        update workflow_step set min_sign_level = 'advanced' where repeatable_sign_type = 'certSign' or repeatable_sign_type = 'nexuSign';
        
        update live_workflow_step set max_sign_level = 'qualified' where repeatable_sign_type = 'certSign' or repeatable_sign_type = 'nexuSign';
        update workflow_step set max_sign_level = 'qualified' where repeatable_sign_type = 'certSign' or repeatable_sign_type = 'nexuSign';
        
        update live_workflow_step set sign_type = 'signature' where sign_type = 'pdfImageStamp' or sign_type = 'certSign' or sign_type = 'nexuSign';
        update workflow_step set sign_type = 'signature' where sign_type = 'pdfImageStamp' or sign_type = 'certSign' or sign_type = 'nexuSign';
        
        update live_workflow_step set repeatable_sign_type = 'signature' where repeatable_sign_type = 'pdfImageStamp' or repeatable_sign_type = 'certSign' or repeatable_sign_type = 'nexuSign';
        update workflow_step set repeatable_sign_type = 'signature' where repeatable_sign_type = 'pdfImageStamp' or repeatable_sign_type = 'certSign' or repeatable_sign_type = 'nexuSign';
        
        alter table public.workflow_step
            add constraint workflow_step_repeatable_sign_type_check
                check ((repeatable_sign_type)::text = ANY
                       ((ARRAY ['hiddenVisa'::character varying, 'visa'::character varying, 'signature'::character varying])::text[]));
        
        
        alter table public.live_workflow_step
            add constraint live_workflow_step_repeatable_sign_type_check
                check ((repeatable_sign_type)::text = ANY
                       ((ARRAY ['hiddenVisa'::character varying, 'visa'::character varying, 'signature'::character varying])::text[]));
        
        alter table public.workflow_step
            add constraint workflow_step_sign_type_check
                check ((sign_type)::text = ANY
                       ((ARRAY ['hiddenVisa'::character varying, 'visa'::character varying, 'signature'::character varying])::text[]));
        
        alter table public.live_workflow_step
            add constraint live_workflow_step_sign_type_check
                check ((sign_type)::text = ANY
                       ((ARRAY ['hiddenVisa'::character varying, 'visa'::character varying, 'signature'::character varying])::text[]));
        """
        ).executeUpdate();
        logger.info("#### Update sign types done ####");
    }

}
