package com.lawfirm.security;

import com.lawfirm.entity.Role;
import com.lawfirm.entity.User;
import com.lawfirm.entity.UserRole;
import com.lawfirm.repository.UserRepository;
import com.lawfirm.repository.UserRoleRepository;
import com.lawfirm.repository.RoleRepository;
import com.lawfirm.util.JwtUtil;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    private static final String HEADER_NAME = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 设置请求和响应的字符编码为UTF-8
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
                Long userId = jwtUtil.getUserIdFromToken(jwt);
                String username = jwtUtil.getUsernameFromToken(jwt);

                if (userId != null && username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // 加载用户权限
                    List<SimpleGrantedAuthority> authorities = loadUserAuthorities(userId);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("已设置用户认证信息: userId={}, username={}, authorities={}", userId, username, authorities.size());
                }
            }
        } catch (Exception e) {
            log.error("无法设置用户认证: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 加载用户权限
     */
    private List<SimpleGrantedAuthority> loadUserAuthorities(Long userId) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 添加基础角色
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // 加载用户角色和权限
        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        for (UserRole userRole : userRoles) {
            Role role = roleRepository.findById(userRole.getRoleId()).orElse(null);
            if (role != null) {
                // 添加角色权限（ROLE_角色编码）
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()));

                // ADMIN用户拥有所有权限
                if ("ADMIN".equals(role.getRoleCode())) {
                    authorities.add(new SimpleGrantedAuthority("CASE_CREATE"));
                    authorities.add(new SimpleGrantedAuthority("CASE_VIEW"));
                    authorities.add(new SimpleGrantedAuthority("CASE_EDIT"));
                    authorities.add(new SimpleGrantedAuthority("CASE_DELETE"));
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

        return authorities;
    }

    private void addAuthorities(List<SimpleGrantedAuthority> authorities, String... codes) {
        for (String code : codes) {
            authorities.add(new SimpleGrantedAuthority(code));
        }
    }

    /**
     * 从请求中提取JWT Token
     *
     * @param request HTTP请求
     * @return JWT Token字符串（不含前缀）
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_NAME);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
