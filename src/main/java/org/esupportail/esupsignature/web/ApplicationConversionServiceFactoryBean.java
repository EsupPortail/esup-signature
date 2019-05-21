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
package org.esupportail.esupsignature.web;

import org.esupportail.esupsignature.domain.Document;
import org.esupportail.esupsignature.domain.Log;
import org.esupportail.esupsignature.domain.SignBook;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.support.FormattingConversionServiceFactoryBean;

@Configurable
/**
 * A central place to register application converters and formatters. 
 */
public class ApplicationConversionServiceFactoryBean extends FormattingConversionServiceFactoryBean {


	public Converter<Document, String> getDocumentToStringConverter() {
        return new org.springframework.core.convert.converter.Converter<org.esupportail.esupsignature.domain.Document, java.lang.String>() {
            public String convert(Document document) {
                return new StringBuilder().append(document.getFileName()).append(' ').append(document.getSize()).append(' ').append(document.getParentId()).append(' ').append(document.getContentType()).toString();
            }
        };
    }

	public Converter<Long, Document> getIdToDocumentConverter() {
        return new org.springframework.core.convert.converter.Converter<java.lang.Long, org.esupportail.esupsignature.domain.Document>() {
            public org.esupportail.esupsignature.domain.Document convert(java.lang.Long id) {
                return Document.findDocument(id);
            }
        };
    }

	public Converter<String, Document> getStringToDocumentConverter() {
        return new org.springframework.core.convert.converter.Converter<java.lang.String, org.esupportail.esupsignature.domain.Document>() {
            public org.esupportail.esupsignature.domain.Document convert(String id) {
                return getObject().convert(getObject().convert(id, Long.class), Document.class);
            }
        };
    }

	public Converter<Log, String> getLogToStringConverter() {
        return new org.springframework.core.convert.converter.Converter<org.esupportail.esupsignature.domain.Log, java.lang.String>() {
            public String convert(Log log) {
                return new StringBuilder().append(log.getLogDate()).append(' ').append(log.getEppn()).append(' ').append(log.getAction()).append(' ').append(log.getInitialStatus()).toString();
            }
        };
    }

	public Converter<Long, Log> getIdToLogConverter() {
        return new org.springframework.core.convert.converter.Converter<java.lang.Long, org.esupportail.esupsignature.domain.Log>() {
            public org.esupportail.esupsignature.domain.Log convert(java.lang.Long id) {
                return Log.findLog(id);
            }
        };
    }

	public Converter<String, Log> getStringToLogConverter() {
        return new org.springframework.core.convert.converter.Converter<java.lang.String, org.esupportail.esupsignature.domain.Log>() {
            public org.esupportail.esupsignature.domain.Log convert(String id) {
                return getObject().convert(getObject().convert(id, Long.class), Log.class);
            }
        };
    }

	public Converter<SignBook, String> getSignBookToStringConverter() {
        return new org.springframework.core.convert.converter.Converter<org.esupportail.esupsignature.domain.SignBook, java.lang.String>() {
            public String convert(SignBook signBook) {
                return new StringBuilder().append(signBook.getName()).append(' ').append(signBook.getCreateDate()).append(' ').append(signBook.getCreateBy()).append(' ').append(signBook.getUpdateDate()).toString();
            }
        };
    }

	public Converter<Long, SignBook> getIdToSignBookConverter() {
        return new org.springframework.core.convert.converter.Converter<java.lang.Long, org.esupportail.esupsignature.domain.SignBook>() {
            public org.esupportail.esupsignature.domain.SignBook convert(java.lang.Long id) {
                return SignBook.findSignBook(id);
            }
        };
    }

	public Converter<String, SignBook> getStringToSignBookConverter() {
        return new org.springframework.core.convert.converter.Converter<java.lang.String, org.esupportail.esupsignature.domain.SignBook>() {
            public org.esupportail.esupsignature.domain.SignBook convert(String id) {
                return getObject().convert(getObject().convert(id, Long.class), SignBook.class);
            }
        };
    }

	public void installLabelConverters(FormatterRegistry registry) {
        registry.addConverter(getDocumentToStringConverter());
        registry.addConverter(getIdToDocumentConverter());
        registry.addConverter(getStringToDocumentConverter());
        registry.addConverter(getLogToStringConverter());
        registry.addConverter(getIdToLogConverter());
        registry.addConverter(getStringToLogConverter());
        registry.addConverter(getSignBookToStringConverter());
        registry.addConverter(getIdToSignBookConverter());
        registry.addConverter(getStringToSignBookConverter());
    }

	public void afterPropertiesSet() {
        super.afterPropertiesSet();
        installLabelConverters(getObject());
    }
}
