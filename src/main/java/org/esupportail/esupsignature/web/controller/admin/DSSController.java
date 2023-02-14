package org.esupportail.esupsignature.web.controller.admin;

import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.TLInfo;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import org.esupportail.esupsignature.dss.config.DSSBeanConfig;
import org.esupportail.esupsignature.dss.service.KeystoreService;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.service.dss.DSSService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import java.util.Collections;

@Controller
@RequestMapping(value = "/admin/dss" )
@ConditionalOnBean(DSSBeanConfig.class)
public class DSSController {

	@ModelAttribute("adminMenu")
	String getCurrentMenu() {
		return "active";
	}

	@ModelAttribute("activeMenu")
	public String getActiveMenu() {
		return "dss";
	}

	@Resource
	private DSSService dssService;

	@Resource
	private KeystoreService keystoreService;

	@Resource
	@Qualifier("european-trusted-list-certificate-source")
	private TrustedListsCertificateSource trustedListsCertificateSource;

	@GetMapping
	public String tlInfoPage(Model model) {
		TLValidationJobSummary summary = trustedListsCertificateSource.getSummary();
		model.addAttribute("summary", summary);
		model.addAttribute("keystoreCertificates", keystoreService.getCertificatesDTOFromKeyStore(dssService.getTrustedListsCertificateSource().getCertificates()));
		OfficialJournalSchemeInformationURI ojUriInfo = (OfficialJournalSchemeInformationURI) dssService.getLotlSource().getSigningCertificatesAnnouncementPredicate();
		model.addAttribute("currentOjUrl", ojUriInfo.getOfficialJournalURL());
		model.addAttribute("actualOjUrl", dssService.getActualOjUrl());
		return "admin/dss/tl-summary";
	}

	@GetMapping(value = "/lotl/{id}")
	public String lotlInfoPage(@PathVariable(value = "id") String id, Model model) throws EsupSignatureRuntimeException {
		LOTLInfo lotlInfo = dssService.getLOTLInfoById(id);
		if (lotlInfo == null) {
			throw new EsupSignatureRuntimeException(String.format("The LOTL with the specified id [%s] is not found!", id));
		}
		model.addAttribute("lotlInfo", lotlInfo);
		return "admin/dss/lotl-info";
	}

	@GetMapping(value = "/tl/{id}")
	public String tlInfoPageByCountry(@PathVariable(value = "id") String id, Model model) throws EsupSignatureRuntimeException {
		TLInfo tlInfo = dssService.getTLInfoById(id);
		if (tlInfo == null) {
			throw new EsupSignatureRuntimeException(String.format("The TL with the specified id [%s] is not found!", id));
		}
		model.addAttribute("tlInfo", tlInfo);
		return "admin/dss/tl-info-country";
	}


	@GetMapping(value = "/pivot-changes/{lotlId}")
	public String getPivotChangesPage(@PathVariable("lotlId") String lotlId, Model model) throws EsupSignatureRuntimeException {
		LOTLInfo lotlInfo = dssService.getLOTLInfoById(lotlId);
		if (lotlInfo != null) {
			model.addAttribute("lotl", lotlInfo);
			model.addAttribute("originalKeystore", lotlInfo.getValidationCacheInfo().isResultExist() ?
					lotlInfo.getValidationCacheInfo().getPotentialSigners() : Collections.emptyList());
			return "admin/dss/pivot-changes";
		} else {
			throw new EsupSignatureRuntimeException(String.format("The requested LOTL with id [%s] does not exist!", lotlId));
		}
	}


}