package org.esupportail.esupsignature.web.controller.admin;

import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.TLInfo;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import org.esupportail.esupsignature.dss.config.DSSBeanConfig;
import org.esupportail.esupsignature.dss.service.KeystoreService;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.dss.DSSService;
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

	@GetMapping
	public String tlInfoPage(Model model) {
		TLValidationJobSummary summary = dssService.getTrustedListsCertificateSource().getSummary();
		model.addAttribute("summary", summary);
		return "admin/dss/tl-summary";
	}

	@GetMapping(value = "/oj")
	public String showCertificates(Model model) {
		model.addAttribute("keystoreCertificates", keystoreService.getCertificatesDTOFromKeyStore(dssService.getTrustedListsCertificateSource().getCertificates()));
		OfficialJournalSchemeInformationURI ojUriInfo = (OfficialJournalSchemeInformationURI) dssService.getLotlSource().getSigningCertificatesAnnouncementPredicate();
		model.addAttribute("currentOjUrl", ojUriInfo.getOfficialJournalURL());
		model.addAttribute("actualOjUrl", dssService.getActualOjUrl());
		model.addAttribute("customCertificates", keystoreService.getCertificatesDTOFromKeyStore(dssService.getMyTrustedCertificateSource().getCertificates()));
		return "admin/dss/oj-certificates";
	}

	@GetMapping(value = "/lotl/{id}")
	public String lotlInfoPage(@PathVariable(value = "id") String id, Model model) throws EsupSignatureException {
		LOTLInfo lotlInfo = dssService.getLOTLInfoById(id);
		if (lotlInfo == null) {
			throw new EsupSignatureException(String.format("The LOTL with the specified id [%s] is not found!", id));
		}
		model.addAttribute("lotlInfo", lotlInfo);
		return "admin/dss/lotl-info";
	}

	@GetMapping(value = "/tl/{id}")
	public String tlInfoPageByCountry(@PathVariable(value = "id") String id, Model model) throws EsupSignatureException {
		TLInfo tlInfo = dssService.getTLInfoById(id);
		if (tlInfo == null) {
			throw new EsupSignatureException(String.format("The TL with the specified id [%s] is not found!", id));
		}
		model.addAttribute("tlInfo", tlInfo);
		return "admin/dss/tl-info-country";
	}


	@GetMapping(value = "/pivot-changes/{lotlId}")
	public String getPivotChangesPage(@PathVariable("lotlId") String lotlId, Model model) throws EsupSignatureException {
		LOTLInfo lotlInfo = dssService.getLOTLInfoById(lotlId);
		if (lotlInfo != null) {
			model.addAttribute("lotl", lotlInfo);
			model.addAttribute("originalKeystore", lotlInfo.getValidationCacheInfo().isResultExist() ?
					lotlInfo.getValidationCacheInfo().getPotentialSigners() : Collections.emptyList());
			return "admin/dss/pivot-changes";
		} else {
			throw new EsupSignatureException(String.format("The requested LOTL with id [%s] does not exist!", lotlId));
		}
	}


}