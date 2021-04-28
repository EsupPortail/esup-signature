package org.esupportail.esupsignature.service.export;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.ActionType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.repository.DataRepository;
import org.esupportail.esupsignature.service.utils.file.FileService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataExportService {

    @Resource
    private DataRepository dataRepository;

    @Resource
    private FileService fileService;

    public InputStream getCsvDatasFromForms(List<Form> forms) throws SQLException, IOException {
        return mapListToCSV(getDatasToExport(forms));
    }

    public List<Map<String, String>> getDatasToExport(List<Form> forms) {
        List<Map<String, String>> dataDatas = new ArrayList<>();
        for(Form form: forms) {
            dataDatas.addAll(getDatasToExport(form));
        }
        return  dataDatas;
    }

    public List<LinkedHashMap<String, String>> getDatasToExport(Form form) {
        List<Data> datas = dataRepository.findByFormId(form.getId());
        List<LinkedHashMap<String, String>> dataDatas = new ArrayList<>();
        for(Data data : datas) {
            SignBook signBook = data.getSignBook();
            if(signBook != null && signBook.getSignRequests().size() > 0) {
                LinkedHashMap<String, String> toExportDatas = new LinkedHashMap<>();
                toExportDatas.put("form_name", form.getName());
                toExportDatas.put("form_create_date", data.getCreateDate().toString());
                toExportDatas.put("form_create_by", data.getCreateBy().getEppn());
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
                for(Field field : form.getFields()) {
                    toExportDatas.put("form_data_" + field.getName(), data.getDatas().get(field.getName()));
                }
                int step = 1;
                for (Map.Entry<Recipient, Action> actions : recipientHasSigned.entrySet()) {
                    if (!actions.getValue().getActionType().equals(ActionType.none)) {
                        toExportDatas.put("sign_step_" + step + "_user_eppn", actions.getKey().getUser().getEppn());
                        toExportDatas.put("sign_step_" + step + "_type", actions.getValue().getActionType().name());
                        toExportDatas.put("sign_step_" + step + "_date", actions.getValue().getDate().toString());
                    } else {
                        toExportDatas.put("sign_step_" + step + "_user_eppn", "");
                        toExportDatas.put("sign_step_" + step + "_type", "");
                        toExportDatas.put("sign_step_" + step + "_date", "");
                    }
                    step++;
                }
                dataDatas.add(toExportDatas);
            }
        }
        return dataDatas;
    }

    public InputStream mapListToCSV(List<Map<String, String>> list) throws IOException, SQLException {
        File csvFile = fileService.getTempFile("export.csv");
        FileWriter out = new FileWriter(csvFile);
        String[] headers = list.stream().flatMap(map -> map.keySet().stream()).distinct().toArray(String[]::new);
        CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withHeader(headers));
        for (Map<String, String> map : list) {
            printer.printRecord(map.values());
        }
        out.flush();
        InputStream inputStream = new FileInputStream(csvFile);
        csvFile.delete();
        return inputStream;
    }

}
