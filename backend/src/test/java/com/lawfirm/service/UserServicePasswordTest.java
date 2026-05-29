package com.lawfirm.service;

import com.lawfirm.dto.UserCreateRequest;
import com.lawfirm.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServicePasswordTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void weakPasswordRejectedOnCreate() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("pwd_test_" + System.nanoTime() % 1_000_000);
        request.setPassword("weak123");
        request.setRealName("测试");
        assertThrows(Exception.class, () -> userService.createUser(request));
    }

    @Test
    void strongPasswordAcceptedOnCreate() {
        String username = "pwd_ok_" + (System.nanoTime() % 1_000_000);
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername(username);
        request.setPassword("Test1234");
        request.setRealName("测试");
        userService.createUser(request);
        userRepository.findByUsername(username).ifPresent(u -> userRepository.deleteById(u.getId()));
    }
}
