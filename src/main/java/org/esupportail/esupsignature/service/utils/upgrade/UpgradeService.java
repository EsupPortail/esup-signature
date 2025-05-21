package org.esupportail.esupsignature.service.utils.upgrade;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.*;
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

    private final String[] updates = new String[] {"1.19", "1.22", "1.23", "1.29.10", "1.30.5", "1.33.7"};

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
                logger.debug("##### Esup-signature  last version : " + version + " #####");
                if (version != null && currentVersion.contains(version.trim())) {
                    logger.debug("##### Esup-signature version is up-to-date #####");
                } else {
                    logger.debug("##### Esup-signature version is not up-to-date #####");
                }
            } catch (Exception e) {
                logger.info("##### Unable to get last version #####", e);
            }
        });
        checkVersion.start();
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
                    AppliVersion appliVersion = new AppliVersion();
                    appliVersion.setEsupSignatureVersion(update);
                    appliVersionRepository.save(appliVersion);
                }
            } else {
                logger.debug("##### Esup-signature is higher than " + update + ", skip update #####");
            }
        }
        logger.info("##### Esup-signature is up-to-date #####");
    }

    private int checkVersionUpToDate(String updateVersion) {
        List<AppliVersion> appliVersions = new ArrayList<>();
        appliVersionRepository.findAll().forEach(appliVersions::add);
        if(appliVersions.isEmpty()) return -1;
        String databaseVersion = appliVersions.get(0).getEsupSignatureVersion().split("-")[0];
        String[] codeVersionStrings = updateVersion.split("\\.");
        String[] databaseVersionStrings = databaseVersion.split("\\.");
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
                    || signBook.getStatus().equals(SignRequestStatus.archived)
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
                                } else if(signBook.getLiveWorkflow().getWorkflow().getTitle() != null) {
                                    signBook.setWorkflowName(signBook.getLiveWorkflow().getWorkflow().getTitle());
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
        logger.info("#### Starting update workflow workflow ####");
        entityManager.createNativeQuery(
                "DO $$ BEGIN " +
                        "IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'workflow' AND column_name = 'autorize_clone') THEN " +
                        "UPDATE workflow SET authorize_clone = autorize_clone WHERE authorize_clone IS NULL; " +
                        "ALTER TABLE workflow DROP COLUMN autorize_clone; " +
                        "END IF; " +
                        "END $$;"
        ).executeUpdate();
        logger.info("#### Update workflow workflow completed ####");
    }
}
