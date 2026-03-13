package com.aron.studio.util;

import com.aron.studio.data.rbac.entity.UserEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentUserUtil {

    public Optional<UserEntity> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return Optional.empty();
        }
        if (!authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserEntity)) {
            return Optional.empty();
        }
        return Optional.of((UserEntity) principal);
    }

    public Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(UserEntity::getUserId);
    }

    public Optional<String> getCurrentRole() {
        return getCurrentUser().map(UserEntity::getRole);
    }

    public Optional<String> getCurrentUsername() {
        return getCurrentUser().map(UserEntity::getUsername);
    }

}
