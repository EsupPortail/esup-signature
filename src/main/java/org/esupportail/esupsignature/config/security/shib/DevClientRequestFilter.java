package org.esupportail.esupsignature.config.security.shib;

import org.esupportail.esupsignature.service.security.DevSecurityFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.filter.GenericFilterBean;

import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

public class DevClientRequestFilter extends GenericFilterBean  implements DevSecurityFilter {
	
	@Resource
	private DevShibProperties devShibProperties;

	@Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper((HttpServletRequest) request) {
			@Override
			public String getHeader(String name) {
	            if ("REMOTE_USER".equals(name)) {
	              return devShibProperties.getEppn();
	            }
	            if ("mail".equals(name)) {
		              return devShibProperties.getMail();
		        }
	            if ("sn".equals(name)) {
		              return devShibProperties.getSn();
		        }
	            if ("givenName".equals(name)) {
		              return devShibProperties.getGivenName();
		         }
	            return super.getHeader(name);
			}		
        };
        chain.doFilter(wrapper, response);
    }  
}
