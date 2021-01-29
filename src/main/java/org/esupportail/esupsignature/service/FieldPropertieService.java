package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.FieldPropertie;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.FieldPropertieRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class FieldPropertieService {

    @Resource
    private FieldPropertieRepository fieldPropertieRepository;

    public void createFieldPropertie(User user, Field field, String value) {
        FieldPropertie fieldPropertie = getFieldPropertie(user.getEppn(), field.getId());
        if(value != null && !value.isEmpty()) {
            if (fieldPropertie == null) {
                addPropertie(user, field, value);
            } else {
                fieldPropertie.getFavorites().put(value, new Date());
                fieldPropertieRepository.save(fieldPropertie);
            }
        }
    }

    private void addPropertie(User user, Field field, String value) {
        FieldPropertie fieldPropertie = new FieldPropertie();
        fieldPropertie.getFavorites().put(value, new Date());
        fieldPropertie.setUser(user);
        fieldPropertie.setField(field);
        fieldPropertieRepository.save(fieldPropertie);
    }

    public List<String> getFavoritesValues(String userEppn, Long id) {
        List<String> favoriteValues = new ArrayList<>();
        FieldPropertie fieldPropertie = getFieldPropertie(userEppn, id);
        if(fieldPropertie != null) {
            Map<String, Date> favorites = fieldPropertie.getFavorites();
            if (favorites.size() > 0) {
                List<Map.Entry<String, Date>> entrySet = new ArrayList<>(favorites.entrySet());
                entrySet.sort(Map.Entry.<String, Date>comparingByValue().reversed());
                for (int i = 0; i < Math.min(entrySet.size(), 5); i++) {
                    favoriteValues.add(entrySet.get(i).getKey());
                }
            }
        }
        return favoriteValues;
    }

    public FieldPropertie getFieldPropertie(String userEppn, Long fieldId) {
        return fieldPropertieRepository.findByFieldIdAndUserEppn(fieldId, userEppn);
    }

    public void delete(FieldPropertie fieldPropertie) {
        fieldPropertieRepository.delete(fieldPropertie);
    }
}
