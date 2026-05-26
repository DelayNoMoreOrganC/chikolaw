package com.lawfirm.controller;

import com.lawfirm.dto.ClientDTO;
import com.lawfirm.dto.ClientImportResultDTO;
import com.lawfirm.security.SecurityUtils;
import com.lawfirm.service.ClientImportService;
import com.lawfirm.service.ClientService;
import com.lawfirm.util.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

/**
 * 客户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final ClientImportService clientImportService;
    private final SecurityUtils securityUtils;
    private final com.lawfirm.repository.UserRepository userRepository;

    /**
     * 创建客户
     * POST /api/clients
     */
    @PostMapping
    public Result<ClientDTO> createClient(@Valid @RequestBody ClientDTO dto) {
        try {
            Long userId = getCurrentUserId();
            ClientDTO result = clientService.createClient(dto, userId);
            return Result.success("客户创建成功", result);
        } catch (IllegalArgumentException e) {
            log.error("创建客户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建客户异常", e);
            return Result.error("创建客户失败");
        }
    }

    /**
     * 更新客户
     * PUT /api/clients/{id}
     */
    @PutMapping("/{id}")
    public Result<ClientDTO> updateClient(@PathVariable Long id, @Valid @RequestBody ClientDTO dto) {
        try {
            Long userId = getCurrentUserId();
            ClientDTO result = clientService.updateClient(id, dto, userId, securityUtils.isAdmin());
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("更新客户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新客户异常", e);
            return Result.error("更新客户失败");
        }
    }

    /**
     * 删除客户
     * DELETE /api/clients/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteClient(@PathVariable Long id) {
        try {
            clientService.deleteClient(id);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.error("删除客户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除客户异常", e);
            return Result.error("删除客户失败");
        }
    }

    /**
     * 查询客户详情
     * GET /api/clients/{id}
     */
    @GetMapping("/{id}")
    public Result<ClientDTO> getClient(@PathVariable Long id) {
        try {
            ClientDTO result = clientService.getClientById(id);
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            log.error("查询客户失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("查询客户异常", e);
            return Result.error("查询客户失败");
        }
    }

    /**
     * 搜索客户
     * GET /api/clients/search?keyword={keyword}
     */
    @GetMapping("/search")
    public Result<List<ClientDTO>> searchClients(@RequestParam String keyword) {
        try {
            List<ClientDTO> result = clientService.searchClients(keyword);
            return Result.success(result);
        } catch (Exception e) {
            log.error("搜索客户异常", e);
            return Result.error("搜索客户失败");
        }
    }

    /**
     * 按类型查询客户
     * GET /api/clients/type/{clientType}
     */
    @GetMapping("/type/{clientType}")
    public Result<List<ClientDTO>> getClientsByType(@PathVariable String clientType) {
        try {
            List<ClientDTO> result = clientService.getClientsByType(clientType);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询客户异常", e);
            return Result.error("查询客户失败");
        }
    }

    /**
     * 按状态查询客户
     * GET /api/clients/status/{status}
     */
    @GetMapping("/status/{status}")
    public Result<List<ClientDTO>> getClientsByStatus(@PathVariable String status) {
        try {
            List<ClientDTO> result = clientService.getClientsByStatus(status);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询客户异常", e);
            return Result.error("查询客户失败");
        }
    }

    /**
     * 查询用户的客户
     * GET /api/clients/owner/{ownerId}
     */
    @GetMapping("/owner/{ownerId}")
    public Result<List<ClientDTO>> getClientsByOwner(@PathVariable Long ownerId) {
        try {
            List<ClientDTO> result = clientService.getClientsByOwner(ownerId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询客户异常", e);
            return Result.error("查询客户失败");
        }
    }

    /**
     * 利益冲突检索
     * GET /api/clients/{id}/conflict-check
     */
    @GetMapping("/{id}/conflict-check")
    public Result<ClientDTO> checkConflict(@PathVariable Long id) {
        try {
            ClientDTO result = clientService.checkConflict(id);
            return Result.success(result);
        } catch (Exception e) {
            log.error("利益冲突检索异常", e);
            return Result.error("利益冲突检索失败");
        }
    }

    /**
     * 获取客户的案件列表
     * GET /api/clients/{id}/cases
     */
    @GetMapping("/{id}/cases")
    public Result<List<com.lawfirm.vo.CaseListVO>> getClientCases(@PathVariable Long id) {
        try {
            List<com.lawfirm.vo.CaseListVO> cases = clientService.getClientCases(id);
            return Result.success(cases);
        } catch (Exception e) {
            log.error("获取客户案件异常", e);
            return Result.error("获取客户案件失败");
        }
    }

    /**
     * 获取客户的沟通记录
     * GET /api/clients/{id}/communications
     */
    @GetMapping("/{id}/communications")
    public Result<List<com.lawfirm.entity.CommunicationRecord>> getCommunications(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<com.lawfirm.entity.CommunicationRecord> records = clientService.getCommunications(id, page, size);
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取沟通记录异常", e);
            return Result.error("获取沟通记录失败");
        }
    }

    /**
     * 创建沟通记录
     * POST /api/clients/{id}/communications
     */
    @PostMapping("/{id}/communications")
    public Result<com.lawfirm.entity.CommunicationRecord> createCommunication(
            @PathVariable Long id,
            @RequestBody com.lawfirm.dto.CommunicationRecordDTO dto) {
        try {
            Long currentUserId = getCurrentUserId();
            com.lawfirm.entity.CommunicationRecord record = clientService.createCommunication(id, dto, currentUserId);
            return Result.success("沟通记录创建成功", record);
        } catch (Exception e) {
            log.error("创建沟通记录异常", e);
            return Result.error("创建沟通记录失败");
        }
    }

    /**
     * Excel 批量导入客户（导入前逐行利冲审查）
     * POST /api/clients/import
     */
    @PostMapping("/import")
    public Result<ClientImportResultDTO> importClients(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "skipConflictRows", defaultValue = "true") boolean skipConflictRows) {
        try {
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }
            Long userId = getCurrentUserId();
            ClientImportResultDTO result = clientImportService.importClients(file, userId, skipConflictRows);
            return Result.success("导入完成", result);
        } catch (Exception e) {
            log.error("客户导入失败", e);
            return Result.error("客户导入失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询客户
     * GET /api/clients?page={page}&size={size}
     */
    @GetMapping
    public Result<com.lawfirm.util.PageResult<ClientDTO>> getClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            var result = clientService.getClients(page, size);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询客户异常", e);
            return Result.error("分页查询客户失败");
        }
    }

    /**
     * 从 Spring Security Context 中获取当前用户ID
     */
    private Long getCurrentUserId() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("用户未登录");
        }

        // JwtAuthenticationFilter 将 userId (Long类型) 设置为 principal
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        } else if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                // 如果是字符串格式的用户名，查询数据库
                return userRepository.findByUsername((String) principal)
                        .map(user -> user.getId())
                        .orElseThrow(() -> new IllegalArgumentException("无效的用户信息"));
            }
        } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            org.springframework.security.core.userdetails.UserDetails userDetails =
                    (org.springframework.security.core.userdetails.UserDetails) principal;
            String username = userDetails.getUsername();
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                return userRepository.findByUsername(username)
                        .map(user -> user.getId())
                        .orElseThrow(() -> new IllegalArgumentException("无效的用户信息"));
            }
        }

        throw new IllegalArgumentException("无法识别的用户信息类型");
    }
}
