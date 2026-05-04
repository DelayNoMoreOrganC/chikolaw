package com.lawfirm.controller;

import com.lawfirm.annotation.AuditLog;
import com.lawfirm.dto.DepartmentCreateRequest;
import com.lawfirm.dto.DepartmentDTO;
import com.lawfirm.service.DepartmentService;
import com.lawfirm.util.Result;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 */
@RestController
@RequestMapping("department")
@RequiredArgsConstructor
public class DepartmentController {

    private static final Logger log = LoggerFactory.getLogger(DepartmentController.class);

    private final DepartmentService departmentService;

    /**
     * 创建部门
     * POST /api/department
     */
    @PostMapping
    @AuditLog(value = "创建部门", operationType = "CREATE")
    public Result<DepartmentDTO> createDepartment(@Valid @RequestBody DepartmentCreateRequest request) {
        try {
            DepartmentDTO result = departmentService.createDepartment(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建部门失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新部门
     * PUT /api/department/{id}
     */
    @PutMapping("/{id}")
    @AuditLog(value = "更新部门", operationType = "UPDATE")
    public Result<DepartmentDTO> updateDepartment(@PathVariable Long id,
                                                 @Valid @RequestBody DepartmentCreateRequest request) {
        try {
            DepartmentDTO result = departmentService.updateDepartment(id, request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("更新部门失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除部门
     * DELETE /api/department/{id}
     */
    @DeleteMapping("/{id}")
    @AuditLog(value = "删除部门", operationType = "DELETE")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        try {
            departmentService.deleteDepartment(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除部门失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取部门树
     * GET /api/department/tree
     */
    @GetMapping("/tree")
    public Result<List<DepartmentDTO>> getDepartmentTree() {
        try {
            List<DepartmentDTO> result = departmentService.getDepartmentTree();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取部门树失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取部门列表（平铺）
     * GET /api/department
     */
    @GetMapping
    public Result<List<DepartmentDTO>> getDepartmentList() {
        try {
            List<DepartmentDTO> result = departmentService.getDepartmentList();
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取部门列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取部门详情
     * GET /api/department/{id}
     */
    @GetMapping("/{id}")
    public Result<DepartmentDTO> getDepartmentDetail(@PathVariable Long id) {
        try {
            DepartmentDTO result = departmentService.getDepartmentDetail(id);
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取部门详情失败", e);
            return Result.error(e.getMessage());
        }
    }
}
