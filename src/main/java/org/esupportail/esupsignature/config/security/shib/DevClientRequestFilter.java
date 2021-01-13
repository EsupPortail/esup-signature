package org.esupportail.esupsignature.config.security.shib;

import org.esupportail.esupsignature.service.security.DevSecurityFilter;
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
	ShibProperties shibProperties;

	@Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper((HttpServletRequest) request) {
			@Override
			public String getHeader(String name) {
	            if ("REMOTE_USER".equals(name)) {
	              return shibProperties.getDev().getEppn();
	            }
	            if ("mail".equals(name)) {
		              return shibProperties.getDev().getMail();
		        }
	            if ("sn".equals(name)) {
		              return shibProperties.getDev().getSn();
		        }
	            if ("givenName".equals(name)) {
		              return shibProperties.getDev().getGivenName();
		         }
	            return super.getHeader(name);
			}		
        };
        chain.doFilter(wrapper, response);
    }  
}
