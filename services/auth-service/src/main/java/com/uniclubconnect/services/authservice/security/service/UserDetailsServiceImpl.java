package com.uniclubconnect.services.authservice.security.service;

import com.uniclubconnect.services.authservice.entity.User;
import com.uniclubconnect.services.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Biz 'username' olarak 'email' kullanıyoruz.
        // Veritabanından e-postaya göre kullanıcıyı bul.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + email));

        // Bizim User entity'miz zaten UserDetails'i implemente ettiği için
        // doğrudan onu dönebiliriz. Spring Security gerisini halleder.
        return user;
    }
}