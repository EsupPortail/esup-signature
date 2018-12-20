/**
 * Licensed to ESUP-Portail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * ESUP-Portail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.esupnfccarteculture.domain;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.validation.constraints.NotNull;

import org.springframework.roo.addon.javabean.RooJavaBean;
import org.springframework.roo.addon.jpa.activerecord.RooJpaActiveRecord;
import org.springframework.roo.addon.tostring.RooToString;

@RooJavaBean
@RooToString
@RooJpaActiveRecord(finders = { "findSallesByNomEquals", "findSallesByTypeSalle" })
public class Salle {
	
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String lieu;

    @NotNull
    private String typeSalle;

    private int tarif;

    public void setTarifString(String tarifString) {
        String tarifOld = "0";
        if (tarifString != null) {
            tarifOld = tarifString.replace(",", ".");
        }
        BigDecimal tarifBig = new BigDecimal(tarifOld);
        this.tarif = tarifBig.multiply(BigDecimal.valueOf(100)).intValue();
    }

    public String getTarifString() {
        if (this.tarif > 0) {
            BigDecimal tarifBig = BigDecimal.valueOf(this.tarif);
            DecimalFormat decimalFormat = new java.text.DecimalFormat("####.00");
            return decimalFormat.format(tarifBig.divide(BigDecimal.valueOf(100)));
        } else {
            return "0";
        }
    }

    public static List<Salle> findAllSalles() {
        return entityManager().createQuery("SELECT o FROM Salle o ORDER BY o.nom", Salle.class).getResultList();
    }
    
    public static Long countFindSallesByTypeSalle(TypeSalle typeSalle) {
        if (typeSalle == null) throw new IllegalArgumentException("The typeSalle argument is required");
        EntityManager em = Salle.entityManager();
        TypedQuery q = em.createQuery("SELECT COUNT(o) FROM Salle AS o WHERE o.typeSalle = :typeSalle", Long.class);
        q.setParameter("typeSalle", typeSalle.getNom());
        return ((Long) q.getSingleResult());
    }
    
    public static TypedQuery<Salle> findSallesByTypeSalle(TypeSalle typeSalle) {
        if (typeSalle == null) throw new IllegalArgumentException("The typeSalle argument is required");
        EntityManager em = Salle.entityManager();
        TypedQuery<Salle> q = em.createQuery("SELECT o FROM Salle AS o WHERE o.typeSalle = :typeSalle", Salle.class);
        q.setParameter("typeSalle", typeSalle.getNom());
        return q;
    }

    public static TypedQuery<Salle> findSalles() {
        EntityManager em = Salle.entityManager();
        TypedQuery<Salle> q = em.createQuery("SELECT o FROM Salle AS o WHERE o.typeSalle != 'inscription' AND o.typeSalle != 'joker'", Salle.class);
        return q;
    }

}
