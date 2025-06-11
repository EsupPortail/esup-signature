package org.esupportail.esupsignature.service.security.jwt;

import jakarta.transaction.Transactional;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

@Service
public class JwtAuthService {

	private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthService.class);

	private final GlobalProperties globalProperties;
	private final UserService userService;

    public JwtAuthService(GlobalProperties globalProperties, UserService userService) {
        this.globalProperties = globalProperties;
        this.userService = userService;
    }


    public void createOrUpdateLdapUser(Jwt jwt) {
		String eppn = jwt.getClaimAsString("uid") + "@" + globalProperties.getDomain();
		userService.createUserWithEppn(eppn);
	}

	@Transactional
	public Collection<GrantedAuthority> getGrantedAuthorities(Jwt jwt) {

		String uid = jwt.getClaimAsString("uid");

		Collection<GrantedAuthority> authorities = new ArrayList<>();

		authorities.addAll(extractFromJWT(jwt));
		authorities.addAll(extractFromApp(uid));

		return authorities;
	}

	private Collection<GrantedAuthority> extractFromApp(String uid) {
        User user = userService.getByEppn(userService.buildEppn(uid));
        return new ArrayList<>(getUserAppAuthorities(user));
	}

	private Collection<GrantedAuthority> getUserAppAuthorities(User user) {
		Collection<GrantedAuthority> roles = new HashSet<>();
		roles.add(new SimpleGrantedAuthority("ROLE_USER"));
		return roles;
	}


	private Collection<GrantedAuthority> extractFromJWT(Jwt jwt) {
		return new ArrayList<>();
	}
}
