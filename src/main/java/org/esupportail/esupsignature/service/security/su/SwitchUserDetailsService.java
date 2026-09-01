package org.esupportail.esupsignature.service.security.su;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SwitchUserDetailsService implements UserDetailsService {

    public static final String USER_NEVER_LOGGED_IN_MESSAGE = "Impossible de changer d’utilisateur : cet utilisateur ne s’est encore jamais connecté à l’application.";

    private final UserService userService;

    public SwitchUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String eppn = userService.buildEppn(username);
        User user = userService.getByEppn(eppn);
        if (user == null) {
            throw new UsernameNotFoundException(USER_NEVER_LOGGED_IN_MESSAGE);
        }
        return new org.springframework.security.core.userdetails.User(
                user.getEppn(),
                "dummy",
                userService.getRoles(user.getEppn()).stream().map(SimpleGrantedAuthority::new).toList()
        );
    }
}
