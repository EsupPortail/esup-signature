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
package org.esupportail.esupsignature.entity;

import org.springframework.beans.factory.annotation.Configurable;

import jakarta.persistence.*;

@Configurable
@Entity
public class AppliVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

	String esupSignatureVersion;

    Boolean stopCheckSealCertificat = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEsupSignatureVersion() {
        return esupSignatureVersion;
    }

    public void setEsupSignatureVersion(String esupSignatureVersion) {
        this.esupSignatureVersion = esupSignatureVersion;
    }

    public Boolean isStopCheckSealCertificat() {
        if(stopCheckSealCertificat == null) return false;
        return stopCheckSealCertificat;
    }

    public void setStopCheckSealCertificat(Boolean stopCheckSealCertificat) {
        this.stopCheckSealCertificat = stopCheckSealCertificat;
    }
}
