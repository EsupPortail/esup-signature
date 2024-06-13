package org.esupportail.esupsignature.web.controller.admin;

import eu.europa.esig.dss.spi.tsl.LOTLInfo;
import eu.europa.esig.dss.spi.tsl.TLInfo;
import eu.europa.esig.dss.spi.tsl.TLValidationJobSummary;
import eu.europa.esig.dss.tsl.job.TLValidationJob;
import jakarta.annotation.Resource;
import org.esupportail.esupsignature.dss.config.DSSBeanConfig;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.dss.service.DSSService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;

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

	@GetMapping
	public String tlInfoPage(Model model) {
		TLValidationJobSummary summary = tlValidationJob.getSummary();
		model.addAttribute("summary", summary);
		model.addAttribute("keystoreCertificates", summary.getLOTLInfos().stream()
				.flatMap(lotlInfo -> lotlInfo.getTLInfos().stream())
				.flatMap(tlInfo -> tlInfo.getParsingCacheInfo().getTrustServiceProviders().stream())
				.flatMap(trustServiceProvider -> trustServiceProvider.getServices().stream())
				.flatMap(service -> service.getCertificates().stream()).distinct().count());
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


}