package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.Users;
import com.douzone_internship.backend.dto.response.auth.OAuth2Attribute;
import com.douzone_internship.backend.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UsersRepository usersRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2Attribute attributes = OAuth2Attribute.of(registrationId, oAuth2User.getAttributes());

        saveOrUpdate(attributes);

        return oAuth2User;
    }

    private void saveOrUpdate(OAuth2Attribute attributes) {
        Users user = usersRepository.findByEmail(attributes.email())
                .map(entity -> entity.update(attributes.name()))
                .orElse(attributes.toEntity());
        usersRepository.save(user);
    }
}