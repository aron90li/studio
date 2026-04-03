package com.aron.studio.config;

import com.aron.studio.util.CurrentUserUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeignHeaderInterceptor implements RequestInterceptor {

    @Autowired
    private CurrentUserUtil currentUserUtil;

    @Override
    public void apply(RequestTemplate requestTemplate) {

        Long userId = currentUserUtil.getCurrentUserId().get();
        String role = currentUserUtil.getCurrentRole().get();

        requestTemplate.header("X-User-ID", userId.toString());
        requestTemplate.header("X-Role", role);
    }
}
