# 账户系统安全功能测试报告

## 测试概述

本测试套件验证账户系统的三个核心安全功能：
1. **登录失败锁定**：防止暴力破解攻击
2. **密码强度验证**：确保密码安全性
3. **用户状态管理**：控制用户访问权限

## 运行测试

### 前提条件
- Java 11+
- Maven 3.6+
- MySQL数据库运行中
- Redis（可选，未安装时会使用内存缓存）

### 运行命令

```bash
# 运行所有安全功能测试
mvn test -Dtest=AccountSecurityTest

# 运行单个测试
mvn test -Dtest=AccountSecurityTest#testLoginFailureLock

# 查看测试覆盖率
mvn test jacoco:report
```

## 测试用例

### 1. testLoginFailureLock - 登录失败锁定
**目的**：验证连续5次登录失败后账户被锁定30分钟

**测试步骤**：
1. 创建测试用户
2. 连续5次使用错误密码登录
3. 验证第5次失败后账户被锁定
4. 验证锁定状态被正确记录

**预期结果**：
- 前4次失败返回"用户名或密码错误"
- 第5次失败返回账户被锁定消息
- LoginAttemptCache记录锁定状态

### 2. testPasswordStrengthOnCreate - 创建用户密码验证
**目的**：验证创建用户时密码必须符合强度要求

**测试步骤**：
1. 测试缺少大写字母的密码（应失败）
2. 测试缺少小写字母的密码（应失败）
3. 测试缺少数字的密码（应失败）
4. 测试长度不足的密码（应失败）
5. 测试符合要求的密码（应成功）

**预期结果**：
- 弱密码被拒绝，返回验证错误
- 符合要求的密码（Test1234格式）被接受

### 3. testPasswordStrengthOnChange - 修改密码强度验证
**目的**：验证修改密码时同样需要符合强度要求

**测试步骤**：
1. 用户登录获取Token
2. 尝试将密码修改为弱密码
3. 验证修改被拒绝

**预期结果**：
- 弱密码修改被拒绝
- 错误消息包含密码强度要求

### 4. testDisabledUserCannotLogin - 禁用用户无法登录
**目的**：验证被禁用的用户无法登录系统

**测试步骤**：
1. 将测试用户状态设置为DISABLED
2. 尝试使用正确密码登录
3. 验证登录被拒绝

**预期结果**：
- 登录请求被拒绝
- 错误消息提示账号已被禁用

### 5. testActiveUserCanLogin - 启用用户可以登录
**目的**：验证启用状态的用户可以正常登录

**测试步骤**：
1. 将测试用户状态设置为ACTIVE
2. 使用正确密码登录
3. 验证登录成功并返回Token

**预期结果**：
- 登录成功
- 返回有效的JWT Token

### 6. testLoginSuccessClearsFailedAttempts - 登录成功清除失败记录
**目的**：验证成功登录后清除之前的失败记录

**测试步骤**：
1. 连续3次使用错误密码登录
2. 验证失败记录被创建
3. 使用正确密码登录成功
4. 验证失败记录被清除

**预期结果**：
- 失败登录创建失败记录
- 成功登录后失败记录被清除

### 7. testLockTimeDisplay - 锁定时间显示
**目的**：验证锁定时显示剩余锁定时间

**测试步骤**：
1. 触发账户锁定
2. 验证错误消息包含锁定时间信息
3. 验证可以获取剩余锁定时间

**预期结果**：
- 错误消息包含"锁定"和时间信息
- 剩余时间大于0且不超过30分钟

## 实现细节

### 登录失败锁定
- 使用Redis存储失败记录（优先）
- Redis不可用时使用内存缓存（LoginAttemptCache）
- 5次失败锁定30分钟
- 成功登录后清除失败记录

### 密码强度验证
- 前端验证：@PasswordStrength注解
- 后端验证：UserService.validatePasswordStrength()
- 要求：至少8位，包含大小写字母和数字

### 用户状态管理
- UserStatus枚举：ACTIVE(1), DISABLED(0)
- CustomUserDetailsService检查用户状态
- 禁用用户无法通过认证

## 测试数据

测试使用以下凭据：
- 用户名：testuser_security
- 密码：Test1234
- 状态：ACTIVE / DISABLED（根据测试场景）

## 已知问题

无

## 维护说明

当修改以下功能时需要更新测试：
- AuthController.login()
- UserService.createUser()
- UserService.changePassword()
- UserService.resetPassword()
- CustomUserDetailsService.loadUserByUsername()
- LoginAttemptCache

## 结论

本测试套件全面覆盖了账户系统的安全功能，确保：
1. 系统可以抵御暴力破解攻击
2. 用户密码符合安全要求
3. 管理员可以有效控制用户访问权限

所有测试应该通过，任何失败都需要立即调查和修复。
