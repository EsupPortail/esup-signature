package org.esupportail.esupsignature.config.security.shib;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.esupportail.esupsignature.service.security.DevSecurityFilter;
import org.springframework.web.filter.GenericFilterBean;

public class DevClientRequestFilter extends GenericFilterBean  implements DevSecurityFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper((HttpServletRequest) request) {
			@Override
			public String getHeader(String name) {
	            if ("REMOTE_USER".equals(name)) {
	              return "testju@esup-portail.org";
	            }
	            if ("mail".equals(name)) {
		              return "justin.test@esup-portail.org";
		        }
	            if ("sn".equals(name)) {
		              return "Test";
		        }
	            if ("givenName".equals(name)) {
		              return "Justin";
		         }
	            return super.getHeader(name);
			}		
        };
        chain.doFilter(wrapper, response);
    }  
}
