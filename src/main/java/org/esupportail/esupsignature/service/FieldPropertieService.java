package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Field;
import org.esupportail.esupsignature.entity.FieldPropertie;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.FieldPropertieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class FieldPropertieService {

    @Resource
    private FieldPropertieRepository fieldPropertieRepository;

    public FieldPropertie getById(Long id) {
        return fieldPropertieRepository.findById(id).get();
    }

    public void createFieldPropertie(User user, Field field, String value) {
        FieldPropertie fieldPropertie = getFieldPropertie(field.getId(), user.getEppn());
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
        FieldPropertie fieldPropertie = getFieldPropertie(id, userEppn);
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

    public FieldPropertie getFieldPropertie(Long fieldId, String userEppn) {
        return fieldPropertieRepository.findByFieldIdAndUserEppn(fieldId, userEppn);
    }

    public List<FieldPropertie> getFieldPropertie(Long fieldId) {
        return fieldPropertieRepository.findByFieldId(fieldId);
    }

    public List<FieldPropertie> getFieldProperties(String userEppn) {
        return fieldPropertieRepository.findByUserEppn(userEppn);
    }

    @Transactional
    public void delete(Long id) {
        FieldPropertie fieldPropertie = getById(id);
        fieldPropertieRepository.delete(fieldPropertie);
    }

    @Transactional
    public void delete(Long id, String key) {
        FieldPropertie fieldPropertie = getById(id);
        fieldPropertie.getFavorites().remove(key);
    }

    @Transactional
    public void deleteAll(String userEppn) {
        List<FieldPropertie> fieldProperties = fieldPropertieRepository.findByUserEppn(userEppn);
        fieldPropertieRepository.deleteAll(fieldProperties);
    }

}
