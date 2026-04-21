package com.cs2trade.service.impl;

import com.cs2trade.dto.RegisterRequest;
import com.cs2trade.entity.User;
import com.cs2trade.mapper.UserMapper;
import com.cs2trade.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userMapper, passwordEncoder, jwtUtils);
    }

    @Test
    void registerAllowsBlankEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("tester");
        request.setEmail("   ");
        request.setPassword("secret123");
        request.setConfirmPassword("secret123");

        when(userMapper.countByUsername("tester")).thenReturn(0);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });

        User user = userService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        verify(userMapper, never()).countByEmail(any());
        assertNull(userCaptor.getValue().getEmail());
        assertNull(user.getEmail());
    }

    @Test
    void updateProfileRejectsDuplicateEmail() {
        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail(null);

        when(userMapper.selectById(1L)).thenReturn(currentUser);
        when(userMapper.countByEmail("tester@example.com")).thenReturn(1);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.updateProfile(1L, "tester", null, "tester@example.com")
        );

        assertEquals("该邮箱已被使用", exception.getMessage());
        verify(userMapper, never()).updateById(any(User.class));
    }
}
