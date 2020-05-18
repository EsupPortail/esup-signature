package org.esupportail.esupsignature.web.controller.admin;

import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.ParsingInfoRecord;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.utils.Utils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.dss.service.KeystoreService;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping(value = "/admin/dss" )
public class OJCertificatesController {

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
	private TrustedListsCertificateSource trustedCertificateSource;

	@Resource
	private KeystoreService keystoreService;

	@RequestMapping(method = RequestMethod.GET)
	public String showCertificates(Model model, HttpServletRequest request) {
		model.addAttribute("keystoreCertificates", keystoreService.getCertificatesDTOFromKeyStore(lotlSource.getCertificateSource().getCertificates()));
		OfficialJournalSchemeInformationURI ojUriInfo = (OfficialJournalSchemeInformationURI) lotlSource.getSigningCertificatesAnnouncementPredicate();
		model.addAttribute("currentOjUrl", ojUriInfo.getOfficialJournalURL());
		model.addAttribute("actualOjUrl", getActualOjUrl());
		return "admin/dss/oj-certificates";
	}

	private String getActualOjUrl() {
		TLValidationJobSummary summary = trustedCertificateSource.getSummary();
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