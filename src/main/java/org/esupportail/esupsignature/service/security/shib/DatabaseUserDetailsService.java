package org.esupportail.esupsignature.service.security.shib;

import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class DatabaseUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUserDetailsService.class);

    @Resource
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String eppn) throws UsernameNotFoundException {
        eppn = userService.buildEppn(eppn);
        User user = userService.getByEppn(eppn);
        if(user == null) {
            try {
                user = userService.createUserWithEppn(eppn);
            } catch (EsupSignatureUserException e) {
                logger.warn("unable to create user " + eppn);
            }
        }
        if(user != null) {
            return loadUserByUser(user);
        }
        return new org.springframework.security.core.userdetails.User("anonymous", "dummy",
                true, // enabled
                true, // account not expired
                true, // credentials not expired
                true, // account not locked
                new ArrayList<>());
    }

    public UserDetails loadUserByUser(User targetUser) throws UsernameNotFoundException {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for(String role : userService.getRoles(targetUser.getEppn())) {
            authorities.add(new SimpleGrantedAuthority(role));
        }
        return new org.springframework.security.core.userdetails.User(targetUser.getEppn(), "dummy",
                true, // enabled
                true, // account not expired
                true, // credentials not expired
                true, // account not locked
                authorities);
    }

}