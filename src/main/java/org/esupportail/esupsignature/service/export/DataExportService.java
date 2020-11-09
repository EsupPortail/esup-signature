package org.esupportail.esupsignature.service.export;

import org.apache.commons.text.StringEscapeUtils;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.repository.LogRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataExportService {

    @Resource
    private DataRepository dataRepository;

    public InputStream getCsvDatasFromForms(List<Form> forms) {
        return csvStringToInputStream(toCSV(getDatasToExport(forms)));
    }

    public List<Map<String, String>> getDatasToExport(List<Form> forms) {
        List<Map<String, String>> dataDatas = new ArrayList<>();
        for(Form form: forms) {
            dataDatas.addAll(getDatasToExport(form));
        }
        return  dataDatas;
    }

    public List<LinkedHashMap<String, String>> getDatasToExport(Form form) {
        List<Data> datas = dataRepository.findByForm(form);
        List<LinkedHashMap<String, String>> dataDatas = new ArrayList<>();
        for(Data data : datas) {
            SignBook signBook = data.getSignBook();
            if(signBook != null && signBook.getSignRequests().size() > 0) {
                LinkedHashMap<String, String> toExportDatas = new LinkedHashMap<>();
                toExportDatas.put("form_name", form.getName());
                toExportDatas.put("form_create_date", data.getCreateDate().toString());
                toExportDatas.put("form_create_by", data.getCreateBy());
                toExportDatas.put("form_current_status", signBook.getStatus().name());
                Map<Recipient, Action> recipientHasSigned = signBook.getSignRequests().get(0).getRecipientHasSigned();
                if(recipientHasSigned != null && recipientHasSigned.size() > 0 && signBook.getStatus().equals(SignRequestStatus.completed) ||signBook.getStatus().equals(SignRequestStatus.exported) ||signBook.getStatus().equals(SignRequestStatus.archived)) {
                    Action lastActionHero = recipientHasSigned.values().stream().filter(action -> !action.getActionType().equals(ActionType.none)).findFirst().get();
                    toExportDatas.put("form_completed_date", lastActionHero.getDate().toString());
                    toExportDatas.put("form_completed_by", recipientHasSigned.entrySet().stream().filter(entry -> lastActionHero.equals(entry.getValue())).map(Map.Entry::getKey).findFirst().get().getUser().getEppn());
                } else {
                    toExportDatas.put("form_completed_date", "");
                    toExportDatas.put("form_completed_by", "");
                }
                for (Map.Entry<String, String> entry : data.getDatas().entrySet()) {
                    toExportDatas.put("form_data_" + entry.getKey(), entry.getValue());
                }
                int step = 1;
                for (Map.Entry<Recipient, Action> actions : recipientHasSigned.entrySet()) {
                    if (!actions.getValue().getActionType().equals(ActionType.none)) {
                        toExportDatas.put("sign_step_" + step + "_user_eppn", actions.getKey().getUser().getEppn());
                        toExportDatas.put("sign_step_" + step + "_type", actions.getValue().getActionType().name());
                        toExportDatas.put("sign_step_" + step + "_date", actions.getValue().getDate().toString());
                        step++;
                    }
                }
                dataDatas.add(toExportDatas);
            }
        }
        return dataDatas;
    }

    public  String toCSV(List<Map<String, String>> list) {
        List<String> headers = list.stream().flatMap(map -> map.keySet().stream()).distinct().collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            sb.append(headers.get(i));
            sb.append(i == headers.size()-1 ? "\n" : ",");
        }
        for (Map<String, String> map : list) {
            for (int i = 0; i < headers.size(); i++) {
                String value = "";
                if(map.get(headers.get(i)) != null) {
                    value = StringEscapeUtils.escapeCsv(map.get(headers.get(i)));
                }
                sb.append(value);
                sb.append(i == headers.size()-1 ? "\n" : ",");
            }
        }
        return sb.toString();
    }

    public InputStream csvStringToInputStream(String csvString) {
        byte[] bytes = csvString.getBytes();
        return new ByteArrayInputStream(bytes);
    }

}
