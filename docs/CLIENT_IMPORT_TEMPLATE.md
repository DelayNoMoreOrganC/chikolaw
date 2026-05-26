# 客户 Excel 导入模板说明

接口：`POST /api/clients/import`（multipart `file`，可选 `skipConflictRows=true`）

## 列定义（第 1 行为表头）

| 列 | 字段 | 必填 |
|----|------|------|
| A | 客户名称 | 是 |
| B | 客户类型 | 否（个人/企业等，默认个人） |
| C | 电话 | 否 |
| D | 身份证号 | 否 |
| E | 统一社会信用代码 | 否 |
| F | 邮箱 | 否 |
| G | 地址 | 否 |
| H | 备注 | 否 |

## 利冲规则

- 每行导入前调用 `ConflictCheckService.checkClientNameConflict`
- `skipConflictRows=true`（默认）：利冲行记入结果 `conflictRows`，不阻断其他行
- `skipConflictRows=false`：遇利冲即失败该行

## 权限

- 新建客户负责人默认为当前登录用户
- 客户名称变更、案源人变更规则见 `ClientService.updateClient` 与行政管理要求
