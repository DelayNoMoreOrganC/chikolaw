package com.lawfirm.security;

import com.lawfirm.entity.User;
import com.lawfirm.entity.UserRole;
import com.lawfirm.entity.Role;
import com.lawfirm.enums.UserStatus;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import com.lawfirm.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义UserDetailsService实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        // 检查用户状态
        UserStatus userStatus = user.getUserStatus();
        if (!userStatus.isActive()) {
            log.warn("尝试登录已禁用用户: {}, status: {}", username, userStatus.getDescription());
            throw new DisabledException("账号已被禁用，请联系管理员");
        }

        // 加载用户角色
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 添加基础角色
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // 添加数据库中的角色权限
        for (UserRole userRole : userRoles) {
            Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
            if (role != null) {
                // 添加角色权限（ROLE_角色编码）
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()));

                // ADMIN用户拥有所有权限
                if ("ADMIN".equals(role.getRoleCode())) {
                    // 添加所有权限
                    authorities.add(new SimpleGrantedAuthority("CASE_VIEW"));
                    authorities.add(new SimpleGrantedAuthority("CASE_EDIT"));
                    authorities.add(new SimpleGrantedAuthority("CASE_DELETE"));
                    authorities.add(new SimpleGrantedAuthority("CASE_CREATE"));
                    authorities.add(new SimpleGrantedAuthority("CASE_ARCHIVE"));
                    authorities.add(new SimpleGrantedAuthority("CLIENT_VIEW"));
                    authorities.add(new SimpleGrantedAuthority("CLIENT_EDIT"));
                    authorities.add(new SimpleGrantedAuthority("CLIENT_DELETE"));
                    authorities.add(new SimpleGrantedAuthority("CLIENT_CREATE"));
                    authorities.add(new SimpleGrantedAuthority("STATISTICS_VIEW"));
                    authorities.add(new SimpleGrantedAuthority("STATISTICS_EXPORT"));
                    authorities.add(new SimpleGrantedAuthority("DOCUMENT_VIEW"));
                    authorities.add(new SimpleGrantedAuthority("DOCUMENT_EDIT"));
                    authorities.add(new SimpleGrantedAuthority("DOCUMENT_DELETE"));
                    authorities.add(new SimpleGrantedAuthority("CALENDAR_VIEW"));
                    authorities.add(new SimpleGrantedAuthority("CALENDAR_EDIT"));
                    authorities.add(new SimpleGrantedAuthority("CALENDAR_DELETE"));
                    authorities.add(new SimpleGrantedAuthority("TODO_VIEW"));
                    authorities.add(new SimpleGrantedAuthority("TODO_EDIT"));
                    authorities.add(new SimpleGrantedAuthority("TODO_DELETE"));
                    authorities.add(new SimpleGrantedAuthority("APPROVAL_VIEW"));
                    authorities.add(new SimpleGrantedAuthority("APPROVAL_EDIT"));
                    authorities.add(new SimpleGrantedAuthority("APPROVAL_DELETE"));
                    authorities.add(new SimpleGrantedAuthority("FINANCE_VIEW"));
                    authorities.add(new SimpleGrantedAuthority("FINANCE_EDIT"));
                    authorities.add(new SimpleGrantedAuthority("USER_VIEW"));
                    authorities.add(new SimpleGrantedAuthority("USER_EDIT"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_VIEW"));
                    authorities.add(new SimpleGrantedAuthority("ROLE_EDIT"));
                    authorities.add(new SimpleGrantedAuthority("AI_CONFIG"));
                    authorities.add(new SimpleGrantedAuthority("SYSTEM_CONFIG"));
                } else if ("DIRECTOR".equals(role.getRoleCode()) || "MANAGER".equals(role.getRoleCode())) {
                    addAuthorities(authorities, "CASE_CREATE", "CASE_VIEW", "CASE_EDIT", "CASE_ARCHIVE",
                            "CLIENT_VIEW", "CLIENT_EDIT", "STATISTICS_VIEW", "STATISTICS_EXPORT",
                            "DOCUMENT_VIEW", "DOCUMENT_EDIT", "CALENDAR_VIEW", "CALENDAR_EDIT",
                            "TODO_VIEW", "TODO_EDIT", "APPROVAL_VIEW", "APPROVAL_EDIT",
                            "FINANCE_VIEW", "USER_VIEW", "ROLE_VIEW");
                } else if ("LAWYER_MAIN".equals(role.getRoleCode())) {
                    addAuthorities(authorities, "CASE_CREATE", "CASE_VIEW", "CASE_EDIT", "CASE_ARCHIVE",
                            "CLIENT_VIEW", "CLIENT_EDIT", "STATISTICS_VIEW", "DOCUMENT_VIEW", "DOCUMENT_EDIT",
                            "CALENDAR_VIEW", "CALENDAR_EDIT", "TODO_VIEW", "TODO_EDIT", "APPROVAL_VIEW");
                } else if ("LAWYER_ASSIST".equals(role.getRoleCode()) || "ASSISTANT".equals(role.getRoleCode())) {
                    addAuthorities(authorities, "CASE_VIEW", "CASE_EDIT", "CLIENT_VIEW", "DOCUMENT_VIEW",
                            "DOCUMENT_EDIT", "CALENDAR_VIEW", "CALENDAR_EDIT", "TODO_VIEW", "TODO_EDIT");
                } else if ("FINANCE".equals(role.getRoleCode())) {
                    addAuthorities(authorities, "CASE_VIEW", "CLIENT_VIEW", "STATISTICS_VIEW",
                            "FINANCE_VIEW", "FINANCE_EDIT", "DOCUMENT_VIEW");
                }
            }
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountLocked(!userStatus.isActive())
                .disabled(!userStatus.isActive())
                .build();
    }

    private void addAuthorities(List<SimpleGrantedAuthority> authorities, String... codes) {
        for (String code : codes) {
            authorities.add(new SimpleGrantedAuthority(code));
        }
    }
}
