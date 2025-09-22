package org.esupportail.esupsignature.service;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.service.interfaces.prefill.PreFillService;
import org.esupportail.esupsignature.service.utils.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * La classe DataService fournit des services pour gérer les objets Data
 * et effectuer des opérations connexes telles que la récupération, la mise à jour,
 * la suppression et la génération de fichiers PDF basés sur des formulaires et leurs données associées.
 */
@Service
public class DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataService.class);

    @Resource
    private DataRepository dataRepository;

    @Resource
    private FormService formService;

    @Resource
    private PreFillService preFillService;

    @Resource
    private PdfService pdfService;

    @Resource
    private UserService userService;

    @Resource
    private FieldPropertieService fieldPropertieService;

    /**
     * Récupère un objet Data à partir de son identifiant.
     * Si aucune donnée n'est trouvée pour l'identifiant donné, une exception est levée.
     *
     * @param dataId l'identifiant unique de l'objet Data à récupérer
     * @return l'objet Data trouvé correspondant à l'identifiant donné
     * @throws NoSuchElementException si aucune donnée correspondante n'est trouvée
     */
    public Data getById(Long dataId) {
        return dataRepository.findById(dataId).orElseThrow();
    }

    /**
     * Récupère un objet Data associé à une SignRequest donnée en utilisant
     * son SignBook parent pour effectuer la recherche.
     *
     * @param signRequest l'objet SignRequest pour lequel récupérer l'objet Data associé
     * @return l'objet Data correspondant au SignBook parent de la SignRequest donnée
     */
    public Data getBySignRequest(SignRequest signRequest) {
        return getBySignBook(signRequest.getParentSignBook());
    }

    /**
     * Récupère les données associées à un livre des signatures donné.
     *
     * @param signBook le livre des signatures dont les données doivent être récupérées
     * @return les données liées au livre des signatures donné
     */
    public Data getBySignBook(SignBook signBook) {
        return dataRepository.findBySignBook(signBook);
    }

    /**
     * Supprime l'entité Data associée à un SignBook spécifique.
     *
     * @param signBook le SignBook pour lequel l'entité Data associée doit être supprimée
     */
    public void deleteBySignBook(SignBook signBook) {
        Data data = getBySignBook(signBook);
        if(data != null) {
            dataRepository.delete(data);
        }
    }

    /**
     * Met à jour les données fournies en fonction du formulaire, des données déjà existantes et de l'utilisateur authentifié.
     *
     * @param form Le formulaire contenant les champs et les métadonnées nécessaires.
     * @param data L'objet Data existant à mettre à jour.
     * @param formDatas Une carte clé-valeur contenant les données saisies ou générées liées au formulaire.
     * @param user L'utilisateur principal effectuant l'opération.
     * @param authUser L'utilisateur authentifié exécutant la mise à jour.
     * @return Un objet Data mis à jour avec les nouvelles informations.
     */
    public Data updateDatas(Form form, Data data, Map<String, String> formDatas, User user, User authUser) {
        SignBook signBook = data.getSignBook();
        List<Field> fields = preFillService.getPreFilledFieldsByServiceName(form.getPreFillType(), form.getFields(), user, data.getSignBook().getSignRequests().get(0));
        for(Field field : fields) {
            if(field.getWorkflowSteps() != null && field.getWorkflowSteps().stream().noneMatch(workflowStep -> signBook.getLiveWorkflow().getCurrentStep().getWorkflowStep() != null && workflowStep.getId().equals(signBook.getLiveWorkflow().getCurrentStep().getWorkflowStep().getId()))) {
                String newData = data.getDatas().get(field.getName());
                if(StringUtils.hasText(newData) && !StringUtils.hasText(formDatas.get(field.getName()))) {
                    formDatas.put(field.getName(), data.getDatas().get(field.getName()));
                }
            }
            if(!field.getStepZero()) {
                field.setDefaultValue("");
            }
            if (field.getFavorisable()) {
                fieldPropertieService.createFieldPropertie(authUser, field, formDatas.get(field.getName()));
            }
            if(field.getExtValueType() != null && field.getExtValueType().equals("system") && !field.getDefaultValue().isEmpty()) {
                formDatas.put(field.getName(), field.getDefaultValue());
            }
        }
        for(String savedDataKeys : data.getDatas().keySet()) {
            if(!formDatas.containsKey(savedDataKeys)) {
                formDatas.put(savedDataKeys, "");
            }
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        data.setName(form.getTitle() + "_" + format.format(new Date()));
        for(Map.Entry<String, String> entry : formDatas.entrySet()) {
            data.getDatas().put(entry.getKey(), entry.getValue());
        }
        data.setForm(form);
        data.setFormName(form.getName());
        data.setFormVersion(form.getId().intValue());
        data.setUpdateBy(authUser);
        data.setUpdateDate(new Date());
        if(data.getId() == null) {
            dataRepository.save(data);
        }
        return data;
    }

    /**
     * Génère un fichier PDF basé sur un modèle et des données fournies.
     * Si un InputStream est disponible, il sera utilisé comme modèle.
     * Sinon, le document associé au formulaire des données sera utilisé comme modèle.
     *
     * @param data l'objet contenant les données et le formulaire à utiliser pour générer le fichier
     * @param inputStream le flux d'entrée des données du modèle PDF (peut être null)
     * @return un tableau de bytes représentant le fichier PDF généré, ou null si aucun modèle PDF n'est disponible
     * @throws IOException si une erreur d'entrée/sortie se produit lors de la génération du fichier
     */
    public byte[] generateFile(Data data, InputStream inputStream) throws IOException {
        Form form = data.getForm();
        if(inputStream != null && inputStream.available() > 0) {
            return pdfService.fill(inputStream, data.getDatas(), false, true);
        } else  if(form.getDocument() != null) {
            return pdfService.fill(pdfService.removeSignField(form.getDocument().getInputStream(), data.getForm().getWorkflow()), data.getDatas(), false, true);
        } else {
            logger.error("no pdf model");
        }
        return null;
    }

    /**
     * Ajoute ou met à jour des données dans un formulaire.
     *
     * @param id L'identifiant du formulaire auquel les données sont associées.
     * @param dataId L'identifiant des données à mettre à jour, ou null si de nouvelles données doivent être créées.
     * @param datas Une map contenant les données à ajouter ou à mettre à jour, avec les clés et valeurs correspondantes.
     * @param userEppn L'identifiant eppn de l'utilisateur initiateur de l'opération.
     * @param authUserEppn L'identifiant eppn de l'utilisateur authentifié réalisant l'opération.
     * @return L'objet Data mis à jour ou créé, correspondant aux données fournies.
     */
    @Transactional
    public Data addData(Long id, Long dataId, Map<String, String> datas, String userEppn, String authUserEppn) {
        Form form = formService.getById(id);
        Data data;
        if(dataId != null) {
            data = getById(dataId);
        } else {
            data = new Data();
        }
        User user = userService.getByEppn(userEppn);
        User authUser = userService.getByEppn(authUserEppn);
        return updateDatas(form, data, datas, user, authUser);
    }

    /**
     * Ajoute une nouvelle instance de Data en la liant à un formulaire existant et à un utilisateur authentifié.
     *
     * @param formId l'identifiant du formulaire auquel la donnée sera associée
     * @param authUserEppn l'identifiant ePPN de l'utilisateur authentifié qui crée la donnée
     * @return l'objet Data nouvellement créé et enregistré en base de données
     */
    @Transactional
    public Data addData(Long formId, String authUserEppn) {
        User authUser = userService.getByEppn(authUserEppn);
        Form form = formService.getById(formId);
        Data data = new Data();
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        data.setName(form.getTitle() + "_" + format.format(new Date()));
        data.setForm(form);
        data.setFormName(form.getName());
        data.setFormVersion(form.getId().intValue());
        data.setStatus(SignRequestStatus.draft);
        data.setCreateBy(authUser);
        data.setCreateDate(new Date());
        dataRepository.save(data);
        return data;
    }

    /**
     * Supprime une instance de Data identifiée par son ID.
     *
     * @param id l'identifiant unique de l'instance de Data à supprimer
     */
    @Transactional
    public void delete(Long id) {
        Data data = dataRepository.findById(id).get();
        data.setForm(null);
        dataRepository.delete(data);
    }

    /**
     * Anonymise les données associées à un utilisateur spécifique en modifiant les références
     * à cet utilisateur par une entité utilisateur anonyme.
     *
     * @param userEppn Identifiant unique (eppn) de l'utilisateur dont les données doivent être anonymisées.
     * @param anonymous Entité utilisateur anonyme qui remplacera l'utilisateur existant dans les données.
     */
    @Transactional
    public void anonymize(String userEppn, User anonymous) {
        User user = userService.getByEppn(userEppn);
        for (Data data : dataRepository.findByCreateBy(user)) {
            data.setCreateBy(anonymous);
        }
        for (Data data : dataRepository.findByUpdateBy(user)) {
            data.setUpdateBy(anonymous);
        }
    }

}