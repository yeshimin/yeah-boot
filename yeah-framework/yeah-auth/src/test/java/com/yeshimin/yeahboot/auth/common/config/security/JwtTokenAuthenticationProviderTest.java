package com.yeshimin.yeahboot.auth.common.config.security;

import com.yeshimin.yeahboot.auth.domain.vo.AuthVo;
import com.yeshimin.yeahboot.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtTokenAuthenticationProviderTest {

    @Test
    void unauthenticatedTokenUsesAuthenticationException() {
        AuthService authService = mock(AuthService.class);
        AuthVo authVo = new AuthVo();
        authVo.setAuthenticated(false);
        when(authService.auth(any())).thenReturn(authVo);
        JwtTokenAuthenticationProvider provider = new JwtTokenAuthenticationProvider(authService);

        assertThrows(BadCredentialsException.class,
                () -> provider.authenticate(new JwtTokenAuthenticationToken("invalid-token")));
    }
}
