package com.stillfresh.app.userservice.service;

import com.stillfresh.app.userservice.model.User;
import com.stillfresh.app.userservice.repository.UserRepository;
import com.stillfresh.app.userservice.security.CustomUserDetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;
    

    @Autowired
    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Override
//    @Cacheable(value = "users", key = "#username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.findUserByUsername(username);
        return new CustomUserDetails(user);
    }

//    @CacheEvict(value = "users", key = "#user.username")
    public void evictUserCache(User user) {
        logger.info("Evicting cache for user: {}", user.getUsername());
    }
}
