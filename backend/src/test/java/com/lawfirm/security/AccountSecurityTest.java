package com.lawfirm.security;

import com.lawfirm.controller.AuthController;
import com.lawfirm.dto.UserCreateRequest;
import com.lawfirm.entity.User;
import com.lawfirm.enums.UserStatus;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.service.UserService;
import com.lawfirm.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 账户系统安全功能测试套件
 * 测试内容：
 * 1. 登录失败锁定（5次失败锁定30分钟）
 * 2. 密码强度验证
 * 3. 用户状态管理（启用/禁用）
 */
@SpringBootTest
public class AccountSecurityTest {

    @Autowired
    private AuthController authController;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private LoginAttemptCache loginAttemptCache;

    private User testUser;

    @BeforeEach
    public void setUp() {
        // 清理测试数据
        loginAttemptCache.clearFailedAttempts("testuser_security");

        // 创建测试用户
        if (userRepository.existsByUsername("testuser_security")) {
            userRepository.deleteByUsername("testuser_security");
        }

        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser_security");
        request.setPassword("Test1234");
        request.setRealName("测试用户");
        request.setEmail("test@example.com");

        testUser = new User();
        testUser.setUsername("testuser_security");
        testUser.setPassword(passwordEncoder.encode("Test1234"));
        testUser.setRealName("测试用户");
        testUser.setEmail("test@example.com");
        testUser.setStatus(UserStatus.ACTIVE.getCode());
        testUser = userRepository.save(testUser);
    }

    /**
     * 测试1：登录失败锁定功能
     * 验证：连续5次错误密码后账户锁定30分钟
     */
    @Test
    public void testLoginFailureLock() {
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("testuser_security");
        request.setPassword("WrongPassword123");

        // 前4次失败应该返回错误但不会被锁定
        for (int i = 1; i <= 4; i++) {
            Exception exception = assertThrows(Exception.class, () -> {
                authController.login(request);
            });
            assertTrue(exception.getMessage().contains("用户名或密码错误") ||
                      exception.getMessage().contains("登录失败"));
            System.out.println("第" + i + "次登录失败，符合预期");
        }

        // 第5次失败应该触发锁定
        Exception lockException = assertThrows(Exception.class, () -> {
            authController.login(request);
        });
        assertTrue(lockException.getMessage().contains("锁定") ||
                  lockException.getMessage().contains("lock"));
        System.out.println("第5次登录失败后账户被锁定，符合预期");

        // 验证锁定状态
        assertTrue(loginAttemptCache.isLocked("testuser_security"),
                   "用户应该被锁定");
        System.out.println("✓ 登录失败锁定测试通过：5次失败后账户被锁定");
    }

    /**
     * 测试2：密码强度验证 - 创建用户
     * 验证：密码必须包含大小写字母和数字
     */
    @Test
    @WithMockUser(username = "admin", authorities = {"ADMIN"})
    public void testPasswordStrengthOnCreate() {
        UserCreateRequest request = new UserCreateRequest();

        // 测试缺少大写字母的密码
        request.setUsername("user1");
        request.setPassword("test1234");
        request.setRealName("用户1");
        assertThrows(Exception.class, () -> {
            userService.createUser(request);
        }, "缺少大写字母的密码应该被拒绝");

        // 测试缺少小写字母的密码
        request.setUsername("user2");
        request.setPassword("TEST1234");
        assertThrows(Exception.class, () -> {
            userService.createUser(request);
        }, "缺少小写字母的密码应该被拒绝");

        // 测试缺少数字的密码
        request.setUsername("user3");
        request.setPassword("TestTest");
        assertThrows(Exception.class, () -> {
            userService.createUser(request);
        }, "缺少数字的密码应该被拒绝");

        // 测试长度不足的密码
        request.setUsername("user4");
        request.setPassword("Te1");
        assertThrows(Exception.class, () -> {
            userService.createUser(request);
        }, "长度不足的密码应该被拒绝");

        // 测试符合要求的密码
        request.setUsername("user5_valid");
        request.setPassword("Test1234");
        request.setRealName("有效用户");
        try {
            userService.createUser(request);
            System.out.println("✓ 密码强度验证测试通过：符合要求的密码被接受");
        } catch (Exception e) {
            fail("符合要求的密码应该被接受: " + e.getMessage());
        } finally {
            // 清理
            userRepository.deleteByUsername("user5_valid");
        }
    }

    /**
     * 测试3：密码强度验证 - 修改密码
     * 验证：修改密码时同样需要符合强度要求
     */
    @Test
    public void testPasswordStrengthOnChange() {
        // 首先登录获取token
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest();
        loginRequest.setUsername("testuser_security");
        loginRequest.setPassword("Test1234");

        String token = null;
        try {
            var result = authController.login(loginRequest);
            token = (String) result.getData().get("token");
        } catch (Exception e) {
            fail("登录失败: " + e.getMessage());
        }

        assertNotNull(token, "Token不应为空");

        // 测试弱密码修改
        AuthController.ChangePasswordRequest changeRequest = new AuthController.ChangePasswordRequest();
        changeRequest.setOldPassword("Test1234");
        changeRequest.setNewPassword("weak");

        final String authToken = token;
        Exception exception = assertThrows(Exception.class, () -> {
            authController.changePassword("Bearer " + authToken, changeRequest);
        });
        assertTrue(exception.getMessage().contains("密码") ||
                  exception.getMessage().contains("password"),
                  "修改密码应该验证强度");

        System.out.println("✓ 修改密码强度验证测试通过");
    }

    /**
     * 测试4：用户状态管理 - 禁用用户无法登录
     * 验证：status=0的用户无法登录
     */
    @Test
    public void testDisabledUserCannotLogin() {
        // 禁用测试用户
        testUser.setStatus(UserStatus.DISABLED.getCode());
        userRepository.save(testUser);

        // 尝试登录
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("testuser_security");
        request.setPassword("Test1234");

        Exception exception = assertThrows(Exception.class, () -> {
            authController.login(request);
        });

        assertTrue(
            exception.getMessage().contains("禁用") ||
            exception.getMessage().contains("disabled") ||
            exception.getMessage().contains("Disabled"),
            "禁用用户登录应该被拒绝"
        );

        System.out.println("✓ 禁用用户登录测试通过：禁用用户无法登录");

        // 恢复用户状态
        testUser.setStatus(UserStatus.ACTIVE.getCode());
        userRepository.save(testUser);
    }

    /**
     * 测试5：用户状态管理 - 启用用户可以登录
     * 验证：status=1的用户可以正常登录
     */
    @Test
    public void testActiveUserCanLogin() {
        // 确保用户是启用状态
        testUser.setStatus(UserStatus.ACTIVE.getCode());
        userRepository.save(testUser);

        // 尝试登录
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("testuser_security");
        request.setPassword("Test1234");

        try {
            var result = authController.login(request);
            assertNotNull(result.getData().get("token"), "启用用户应该能够登录");
            System.out.println("✓ 启用用户登录测试通过：启用用户可以正常登录");
        } catch (Exception e) {
            fail("启用用户应该能够登录: " + e.getMessage());
        }
    }

    /**
     * 测试6：登录成功后清除失败记录
     * 验证：成功登录后应该清除之前的失败记录
     */
    @Test
    public void testLoginSuccessClearsFailedAttempts() {
        // 先失败几次
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("testuser_security");
        request.setPassword("WrongPassword");

        for (int i = 0; i < 3; i++) {
            try {
                authController.login(request);
            } catch (Exception e) {
                // 预期的失败
            }
        }

        // 验证有失败记录
        Integer failCount = loginAttemptCache.getFailedAttempts("testuser_security");
        assertTrue(failCount > 0, "应该有失败记录");

        // 使用正确密码登录
        request.setPassword("Test1234");
        try {
            authController.login(request);

            // 验证失败记录被清除
            Integer failCountAfter = loginAttemptCache.getFailedAttempts("testuser_security");
            assertEquals(0, failCountAfter, "登录成功后应该清除失败记录");

            System.out.println("✓ 登录成功清除失败记录测试通过");
        } catch (Exception e) {
            fail("正确密码应该能够登录: " + e.getMessage());
        }
    }

    /**
     * 测试7：锁定期间显示剩余时间
     * 验证：被锁定时应该提示剩余锁定时间
     */
    @Test
    public void testLockTimeDisplay() {
        // 先制造4次失败
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("testuser_security");
        request.setPassword("WrongPassword");

        for (int i = 0; i < 4; i++) {
            try {
                authController.login(request);
            } catch (Exception e) {
                // 预期的失败
            }
        }

        // 第5次失败触发锁定
        try {
            authController.login(request);
        } catch (Exception e) {
            // 验证错误消息包含锁定信息
            assertTrue(
                e.getMessage().contains("锁定") || e.getMessage().contains("lock") ||
                e.getMessage().contains("分钟") || e.getMessage().contains("minutes"),
                "锁定消息应该包含时间信息"
            );
        }

        // 验证可以获取剩余时间
        Long remainingTime = loginAttemptCache.getRemainingLockTime("testuser_security");
        assertTrue(remainingTime > 0 && remainingTime <= 30,
                   "剩余锁定时间应该大于0且不超过30分钟");

        System.out.println("✓ 锁定时间显示测试通过：剩余时间 = " + remainingTime + " 分钟");
    }
}
