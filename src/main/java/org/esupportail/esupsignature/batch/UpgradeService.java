package org.esupportail.esupsignature.batch;

import org.esupportail.esupsignature.entity.AppliVersion;
import org.esupportail.esupsignature.entity.SignBook;
import org.esupportail.esupsignature.repository.AppliVersionRepository;
import org.esupportail.esupsignature.repository.SignBookRepository;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Service
public class UpgradeService {

    private static final Logger logger = LoggerFactory.getLogger(UpgradeService.class);

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private AppliVersionRepository appliVersionRepository;

    @Resource
    private FileService fileService;

    private final String[] updates = new String[] {"1.19"};

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
                if(appliVersions.size() > 0) {
                    appliVersions.get(0).setEsupSignatureVersion(update);
                } else {
                    AppliVersion appliVersion = new AppliVersion();
                    appliVersion.setEsupSignatureVersion(update);
                    appliVersionRepository.save(appliVersion);
                }
            } else {
                logger.info("##### Esup-signature is higher than " + update + " #####");
            }
        }
        logger.info("##### Esup-signature is up-to-date #####");
    }

    private int checkVersionUpToDate(String updateVersion) {
        List<AppliVersion> appliVersions = new ArrayList<>();
        appliVersionRepository.findAll().forEach(appliVersions::add);
        if(appliVersions.size() == 0) return -1;
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

    @SuppressWarnings("unused")
    public void update_1_19() {
        logger.info("#### Starting update subjets and workflowNames ####");
        List<SignBook> signBooks = signBookRepository.findBySubject(null);
        if(signBooks.size() > 0) {
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
                            if(signBook.getSignRequests().size() > 0) {
                                if(signBook.getSignRequests().get(0).getTitle() != null && signBook.getSignRequests().get(0).getTitle().isEmpty()) {
                                    signBook.setSubject(signBook.getSignRequests().get(0).getTitle());
                                } else {
                                    if(signBook.getSignRequests().get(0).getOriginalDocuments().size() > 0) {
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
                                if(signBook.getSignRequests().get(0).getTitle() != null && !signBook.getSignRequests().get(0).getTitle().isEmpty()) {
                                    if(signBook.getSignRequests().size() > 0) {
                                        if(signBook.getSignRequests().get(0).getTitle() != null && signBook.getSignRequests().get(0).getTitle().isEmpty()) {
                                            signBook.setSubject(signBook.getSignRequests().get(0).getTitle());
                                        } else {
                                            signBook.setSubject(fileService.getNameOnly(signBook.getSignRequests().get(0).getOriginalDocuments().get(0).getFileName()));
                                        }
                                    } else {
                                        signBook.setSubject("Sans titre");
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

}
