package org.esupportail.esupsignature.web.controller.admin;

import eu.europa.esig.dss.spi.tsl.*;
import eu.europa.esig.dss.tsl.function.OfficialJournalSchemeInformationURI;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import eu.europa.esig.dss.tsl.source.LOTLSource;
import eu.europa.esig.dss.utils.Utils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.esupportail.esupsignature.dss.config.DSSBeanConfig;
import org.esupportail.esupsignature.dss.service.DSSService;
import org.esupportail.esupsignature.dss.service.KeystoreService;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping(value = "/admin/dss" )
@ConditionalOnBean(DSSBeanConfig.class)
@PreAuthorize("hasRole('ROLE_ADMIN')")
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
	private TLValidationJob tlValidationJob;

	@Qualifier("european-lotl-source")
	private final LOTLSource lotlSource;

	@Qualifier("european-trusted-list-certificate-source")
	private final TrustedListsCertificateSource trustedCertificateSource;

	private final KeystoreService keystoreService;

	public DSSController(LOTLSource lotlSource, TrustedListsCertificateSource trustedCertificateSource, KeystoreService keystoreService) {
		this.lotlSource = lotlSource;
		this.trustedCertificateSource = trustedCertificateSource;
		this.keystoreService = keystoreService;
	}

	@GetMapping
	public String tlInfoPage(Model model) {
		TLValidationJobSummary summary = tlValidationJob.getSummary();
		model.addAttribute("summary", summary);
		try {
			model.addAttribute("keystoreCertificates", summary.getLOTLInfos().stream()
					.flatMap(lotlInfo -> lotlInfo.getTLInfos().stream())
					.flatMap(tlInfo -> tlInfo.getParsingCacheInfo().getTrustServiceProviders().stream())
					.flatMap(trustServiceProvider -> trustServiceProvider.getServices().stream())
					.flatMap(service -> service.getCertificates().stream()).distinct().count());
		} catch (Exception e) {
			model.addAttribute("keystoreCertificates", 0);
		}
		model.addAttribute("currentOjUrl", dssService.getCurrentOjUrl());
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

	@GetMapping(value = "/oj-certificates")
	public String showCertificates(Model model, HttpServletRequest request) {
		// From Config
		model.addAttribute("keystoreCertificates", keystoreService.getCertificatesDTOFromKeyStore(lotlSource.getCertificateSource().getCertificates()));

		OfficialJournalSchemeInformationURI ojUriInfo = (OfficialJournalSchemeInformationURI) lotlSource.getSigningCertificatesAnnouncementPredicate();
		model.addAttribute("currentOjUrl", ojUriInfo.getUri());

		// From Job
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