package org.esupportail.esupsignature;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;

@ImportResource({"classpath:META-INF/spring/applicationContext*.xml"})
@ComponentScan(basePackages = {"org.esupportail.esupsignature"}, excludeFilters= {
		@ComponentScan.Filter(type=FilterType.REGEX, pattern=".*_Roo_.*"),
		@ComponentScan.Filter(type=FilterType.ANNOTATION, classes={org.springframework.stereotype.Controller.class})
		})
public class EsupSignatureComponentScan {

}
