package com.lawfirm.config;

import com.lawfirm.entity.*;
import com.lawfirm.repository.*;
import com.lawfirm.repository.AIConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据初始化器 - 应用启动时创建默认用户和测试数据
 */
@Slf4j
@Component
@Order(1) // 最高优先级，首先执行
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TodoRepository todoRepository;
    private final CalendarRepository calendarRepository;
    private final AIConfigRepository aiConfigRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    public void run(String... args) {
        try {
            ensureDefaultRoles();
            ensureDefaultDepartments();

            if (userRepository.count() == 0) {
                log.info("数据库为空，创建默认用户和测试数据...");

                // 1. 创建admin用户
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRealName("系统管理员");
                admin.setEmail("admin@lawfirm.com");
                admin.setPhone("13800138000");
                admin.setStatus(1);
                admin.setDeleted(false);
                User savedAdmin = userRepository.save(admin);

                // 2. 获取ADMIN角色
                Role savedRole = roleRepository.findByRoleCode("ADMIN")
                        .orElseThrow(() -> new IllegalStateException("ADMIN角色初始化失败"));

                // 3. 分配角色
                UserRole userRole = new UserRole();
                userRole.setUserId(savedAdmin.getId());
                userRole.setRoleId(savedRole.getId());
                userRoleRepository.save(userRole);

                log.info("默认用户创建成功: username=admin");

                // 4. 创建AI配置
                createDefaultAIConfig();

                // 5. 创建测试数据
                createTestData(savedAdmin);

                log.info("测试数据创建完成！");
            } else {
                log.info("数据库已有用户数据，跳过用户初始化");

                // 即使数据库已有用户，也检查是否需要创建AI配置
                if (aiConfigRepository.count() == 0) {
                    log.info("检测到没有AI配置，创建默认AI配置...");
                    createDefaultAIConfig();
                }
            }
        } catch (Exception e) {
            log.error("数据初始化失败", e);
        }
    }

    private void ensureDefaultRoles() {
        Map<String, String> roles = Map.of(
                "ADMIN", "管理员",
                "DIRECTOR", "主任",
                "LAWYER_MAIN", "主办律师",
                "LAWYER_ASSIST", "协办律师",
                "ASSISTANT", "律师助理",
                "FINANCE", "财务"
        );

        roles.forEach((code, name) -> {
            if (!roleRepository.existsByRoleCode(code)) {
                Role role = new Role();
                role.setRoleCode(code);
                role.setRoleName(name);
                role.setDescription(name + "预置角色");
                roleRepository.save(role);
                log.info("创建预置角色: {}({})", name, code);
            }
        });
    }

    private void ensureDefaultDepartments() {
        if (departmentRepository.count() > 0) {
            return;
        }

        String[] departments = {"诉讼清收部", "执行保全部", "金融客户部", "财务部", "行政部"};
        for (String name : departments) {
            Department department = new Department();
            department.setDeptName(name);
            department.setParentId(0L);
            department.setDescription(name + "默认部门");
            departmentRepository.save(department);
        }
        log.info("默认部门初始化完成");
    }

    private void createTestData(User admin) {
        log.info("开始创建测试数据...");

        // 创建测试待办事项
        createTestTodos(admin.getId());

        // 创建测试日程
        createTestEvents(admin.getId());

        log.info("测试数据创建完成！");
    }

    private void createDefaultAIConfig() {
        log.info("创建默认AI配置...");

        AIConfig config = new AIConfig();
        config.setConfigName("Ollama本地配置");
        config.setProviderType("ollama");
        config.setApiKey(""); // Ollama不需要API key
        config.setApiUrl("http://localhost:11434");
        config.setModelName("qwen3:8b");
        config.setTemperature(0.1);
        config.setMaxTokens(2000);
        config.setTimeoutSeconds(60);
        config.setIsDefault(true);
        config.setIsEnabled(true);
        config.setCategory("DOCUMENT");
        config.setDescription("默认Ollama配置，用于本地AI文档识别。确保Ollama服务在localhost:11434运行并已拉取qwen3:8b模型。");
        config.setDeleted(false);

        aiConfigRepository.save(config);

        log.info("默认AI配置创建成功！使用Ollama本地模型进行文档识别。");
    }

    private void createTestTodos(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // 待办1：明天到期（3天内）
        Todo todo1 = new Todo();
        todo1.setTitle("准备张三案件证据材料");
        todo1.setDescription("收集合同、付款凭证等证据材料");
        todo1.setPriority("HIGH");
        todo1.setDueDate(now.plusDays(1));
        todo1.setStatus("PENDING");
        todo1.setAssigneeId(userId);
        todo1.setReminder(false);
        todoRepository.save(todo1);

        // 待办2：3天后到期（3天内，标红）
        Todo todo2 = new Todo();
        todo2.setTitle("提交李四公司仲裁申请");
        todo2.setDescription("准备仲裁申请书和证据清单");
        todo2.setPriority("NORMAL");
        todo2.setDueDate(now.plusDays(3));
        todo2.setStatus("PENDING");
        todo2.setAssigneeId(userId);
        todo2.setReminder(false);
        todoRepository.save(todo2);

        // 待办3：7天后到期（7天内，标橙）
        Todo todo3 = new Todo();
        todo3.setTitle("王五案件庭前准备");
        todo3.setDescription("准备开庭陈述和证据清单");
        todo3.setPriority("LOW");
        todo3.setDueDate(now.plusDays(7));
        todo3.setStatus("PENDING");
        todo3.setAssigneeId(userId);
        todo3.setReminder(false);
        todoRepository.save(todo3);

        // 待办4：逾期（标红+置顶）
        Todo todo4 = new Todo();
        todo4.setTitle("已逾期：客户面谈");
        todo4.setDescription("与客户王五进行初步案情沟通");
        todo4.setPriority("HIGH");
        todo4.setDueDate(now.minusDays(2));
        todo4.setStatus("PENDING");
        todo4.setAssigneeId(userId);
        todo4.setReminder(false);
        todoRepository.save(todo4);

        log.info("创建4个测试待办事项");
    }

    private void createTestEvents(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // 日程1：明天开庭
        com.lawfirm.entity.Calendar event1 = new com.lawfirm.entity.Calendar();
        event1.setTitle("张三案件庭前会议");
        event1.setStartTime(now.plusDays(1).withHour(9).withMinute(0));
        event1.setEndTime(now.plusDays(1).withHour(11).withMinute(0));
        event1.setLocation("北京市朝阳区人民法院第三法庭");
        event1.setCalendarType("hearing");
        event1.setReminder(true);
        event1.setReminderMinutes(30);
        event1.setCreatedBy(userId);
        calendarRepository.save(event1);

        // 日程2：后天调解
        com.lawfirm.entity.Calendar event2 = new com.lawfirm.entity.Calendar();
        event2.setTitle("李四案件调解会议");
        event2.setStartTime(now.plusDays(2).withHour(14).withMinute(0));
        event2.setEndTime(now.plusDays(2).withHour(16).withMinute(0));
        event2.setLocation("律所会议室A");
        event2.setCalendarType("meeting");
        event2.setReminder(false);
        event2.setCreatedBy(userId);
        calendarRepository.save(event2);

        log.info("创建2个测试日程");
    }
}