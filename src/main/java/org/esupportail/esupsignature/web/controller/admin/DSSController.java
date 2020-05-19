package org.esupportail.esupsignature.web.controller.admin;

import eu.europa.esig.dss.model.identifier.Identifier;
import eu.europa.esig.dss.spi.tsl.*;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.utils.Utils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.KeystoreService;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping(value = "/admin/dss" )
public class DSSController {

	private static final String TL_SUMMARY = "tl-summary";
	private static final String PIVOT_CHANGES = "pivot-changes";
	private static final String TL_DATA = "tl-info-country";
	private static final String LOTL_DATA = "lotl-info";

	@ModelAttribute("adminMenu")
	String getCurrentMenu() {
		return "active";
	}

	@ModelAttribute(value = "user", binding = false)
	public User getUser() {
		return userService.getCurrentUser();
	}

	@ModelAttribute(value = "authUser", binding = false)
	public User getAuthUser() {
		return userService.getUserFromAuthentication();
	}

	@ModelAttribute(value = "globalProperties")
	public GlobalProperties getGlobalProperties() {
		return this.globalProperties;
	}

	@Resource
	private GlobalProperties globalProperties;

	@Resource
	private UserService userService;

	@Resource
	@Qualifier("european-lotl-source")
	private LOTLSource lotlSource;

	@Resource
	@Qualifier("european-trusted-list-certificate-source")
	private TrustedListsCertificateSource trustedListsCertificateSource;

	@Resource
	private KeystoreService keystoreService;

	@GetMapping(value = "/oj")
	public String showCertificates(Model model, HttpServletRequest request) {
		model.addAttribute("keystoreCertificates", keystoreService.getCertificatesDTOFromKeyStore(lotlSource.getCertificateSource().getCertificates()));
		OfficialJournalSchemeInformationURI ojUriInfo = (OfficialJournalSchemeInformationURI) lotlSource.getSigningCertificatesAnnouncementPredicate();
		model.addAttribute("currentOjUrl", ojUriInfo.getOfficialJournalURL());
		model.addAttribute("actualOjUrl", getActualOjUrl());
		return "admin/dss/oj-certificates";
	}

	@GetMapping
	public String tlInfoPage(Model model, HttpServletRequest request) {
		TLValidationJobSummary summary = trustedListsCertificateSource.getSummary();
		model.addAttribute("summary", summary);
		return "admin/dss/tl-summary";
	}


	@GetMapping(value = "/lotl/{id}")
	public String lotlInfoPage(@PathVariable(value = "id") String id, Model model, HttpServletRequest request) throws EsupSignatureException {
		LOTLInfo lotlInfo = getLOTLInfoById(id);
		if (lotlInfo == null) {
			throw new EsupSignatureException(String.format("The LOTL with the specified id [%s] is not found!", id));
		}
		model.addAttribute("lotlInfo", lotlInfo);
		return "admin/dss/lotl-info";
	}

	@GetMapping(value = "/tl/{id}")
	public String tlInfoPageByCountry(@PathVariable(value = "id") String id, Model model, HttpServletRequest request) throws EsupSignatureException {
		TLInfo tlInfo = getTLInfoById(id);
		if (tlInfo == null) {
			throw new EsupSignatureException(String.format("The TL with the specified id [%s] is not found!", id));
		}
		model.addAttribute("tlInfo", tlInfo);
		return "admin/dss/tl-info-country";
	}


	@GetMapping(value = "/pivot-changes/{lotlId}")
	public String getPivotChangesPage(@PathVariable("lotlId") String lotlId, Model model) throws EsupSignatureException {
		LOTLInfo lotlInfo = getLOTLInfoById(lotlId);
		if (lotlInfo != null) {
			model.addAttribute("lotl", lotlInfo);
			model.addAttribute("originalKeystore", lotlInfo.getValidationCacheInfo().isResultExist() ?
					lotlInfo.getValidationCacheInfo().getPotentialSigners() : Collections.emptyList());
			return "admin/dss/pivot-changes";
		} else {
			throw new EsupSignatureException(String.format("The requested LOTL with id [%s] does not exist!", lotlId));
		}
	}

	private LOTLInfo getLOTLInfoById(String lotlId) {
		TLValidationJobSummary summary = trustedListsCertificateSource.getSummary();
		List<LOTLInfo> lotlInfos = summary.getLOTLInfos();
		for (LOTLInfo lotlInfo : lotlInfos) {
			Identifier identifier = lotlInfo.getIdentifier();
			String xmlId = identifier.asXmlId();
			if (xmlId.equals(lotlId)) {
				return lotlInfo;
			}
		}
		return null;
	}

	private TLInfo getTLInfoById(String tlId) {
		TLValidationJobSummary summary = trustedListsCertificateSource.getSummary();
		List<LOTLInfo> lotlInfos = summary.getLOTLInfos();
		for (LOTLInfo lotlInfo : lotlInfos) {
			TLInfo tlInfo = getTLInfoByIdFromList(tlId, lotlInfo.getTLInfos());
			if (tlInfo != null) {
				return tlInfo;
			}
		}
		List<TLInfo> otherTLInfos = summary.getOtherTLInfos();
		TLInfo tlInfo = getTLInfoByIdFromList(tlId, otherTLInfos);
		if (tlInfo != null) {
			return tlInfo;
		}
		return null;
	}

	private TLInfo getTLInfoByIdFromList(String tlId, List<TLInfo> tlInfos) {
		for (TLInfo tlInfo: tlInfos) {
			if (tlInfo.getIdentifier().asXmlId().equals(tlId)) {
				return tlInfo;
			}
		}
		return null;
	}

	private String getActualOjUrl() {
		TLValidationJobSummary summary = trustedListsCertificateSource.getSummary();
		if (summary != null) {
			List<LOTLInfo> lotlInfos = summary.getLOTLInfos();
			for (LOTLInfo lotlInfo : lotlInfos) {
				if (Utils.areStringsEqual(lotlSource.getUrl(), lotlInfo.getUrl())) {
					ParsingInfoRecord parsingCacheInfo = lotlInfo.getParsingCacheInfo();
					if (parsingCacheInfo != null) {
						return parsingCacheInfo.getSigningCertificateAnnouncementUrl();
					}
				}
			}
		}
		return null;
	}

}