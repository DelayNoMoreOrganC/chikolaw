"""
本地大模型信息提取模块
负责调用Ollama本地模型进行结构化信息抽取
"""

import json
import os
import re
import time
from typing import Dict, List, Optional, Any
from urllib.parse import urlparse, urlunparse

import sys
from pathlib import Path

# 添加项目根目录到Python路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

try:
    import ollama
except ImportError:
    ollama = None
    print("警告: ollama库未安装，请运行: pip install ollama")

from modules.statement_parser import extract_statement_template_data


class LLMExtractor:
    """本地大模型信息提取器"""

    def __init__(
        self,
        base_url: str = "http://127.0.0.1:11434",
        model: str = "qwen2.5:7b",
        preferred_models: Optional[List[str]] = None,
        timeout: int = 120,
        temperature: float = 0.1
    ):
        """
        初始化LLM提取器

        Args:
            base_url: Ollama服务地址
            model: 使用的模型名称
            timeout: 请求超时时间（秒）
            temperature: 温度参数（0-1，越低越确定性）
        """
        if ollama is None:
            raise ImportError("ollama库未安装，请运行: pip install ollama")

        self.base_url = self._prepare_base_url(base_url)
        self.requested_model = model
        self.model = model
        self.preferred_models = preferred_models or []
        self.timeout = timeout
        self.temperature = temperature
        self.available_models: List[str] = []

        # 测试连接
        self._test_connection()

    def _prepare_base_url(self, base_url: str) -> str:
        """
        规范化 Ollama 地址，并确保本机地址不走系统代理。
        """
        parsed = urlparse(base_url or "http://127.0.0.1:11434")
        hostname = parsed.hostname or "127.0.0.1"
        if hostname.lower() == "localhost":
            hostname = "127.0.0.1"

        netloc = hostname
        if parsed.port:
            netloc = f"{hostname}:{parsed.port}"

        normalized = urlunparse((
            parsed.scheme or "http",
            netloc,
            parsed.path or "",
            parsed.params or "",
            parsed.query or "",
            parsed.fragment or "",
        ))

        self._ensure_local_no_proxy()
        return normalized

    def _ensure_local_no_proxy(self) -> None:
        """
        确保 localhost / 127.0.0.1 不经过系统代理。
        """
        bypass_hosts = ["127.0.0.1", "localhost"]
        for env_name in ("NO_PROXY", "no_proxy"):
            existing = os.environ.get(env_name, "")
            values = [item.strip() for item in existing.split(",") if item.strip()]
            for host in bypass_hosts:
                if host not in values:
                    values.append(host)
            os.environ[env_name] = ",".join(values)

    def _build_client(self):
        self._ensure_local_no_proxy()
        return ollama.Client(host=self.base_url)

    def _test_connection(self) -> bool:
        """
        测试Ollama服务连接

        Returns:
            bool: 连接是否成功
        """
        last_error = None
        for _ in range(3):
            try:
                client = self._build_client()
                result = client.list()

                available_models = []
                if hasattr(result, 'models'):
                    available_models = [m.model for m in result.models]

                self.available_models = available_models
                self.model = self._resolve_model_name(available_models)
                return True
            except Exception as e:
                last_error = e
                time.sleep(0.5)

        raise ConnectionError(
            f"无法连接到Ollama服务 ({self.base_url}): {str(last_error)}\n"
            f"请确认Ollama已启动且地址正确"
        )

    def _resolve_model_name(self, available_models: List[str]) -> str:
        """
        从配置模型、偏好模型和本机已安装模型中选择一个可用模型。
        """
        if not available_models:
            raise ValueError("Ollama 服务可访问，但未发现任何已安装模型")

        requested_model = (self.requested_model or "").strip()
        if requested_model and requested_model.lower() != "auto" and requested_model in available_models:
            return requested_model

        for model_name in self.preferred_models:
            if model_name in available_models:
                return model_name

        if requested_model and requested_model.lower() != "auto":
            print(f"警告: 模型 '{requested_model}' 不在可用列表中，已自动回退到本机现有模型")
            print(f"可用模型: {', '.join(available_models)}")

        return available_models[0]

    def extract_loan_info(
        self,
        text: str,
        retry_times: int = 3
    ) -> Dict[str, Any]:
        """
        从文本中提取贷款信息

        Args:
            text: 输入文本（OCR提取的PDF内容）
            retry_times: 失败重试次数

        Returns:
            Dict: 提取的结构化数据

        Raises:
            Exception: 提取失败或JSON解析失败
        """
        prompt = self._build_extraction_prompt(text)

        for attempt in range(retry_times):
            try:
                response = self._call_ollama(prompt)

                # 解析JSON响应
                data = self._parse_json_response(response)
                data = self._apply_rule_based_fallbacks(text, data)

                # 验证必需字段
                validation_result = self._validate_extracted_data(data)
                if not validation_result["is_valid"]:
                    if attempt < retry_times - 1:
                        print(f"第{attempt+1}次尝试数据验证失败，重试...")
                        continue
                    else:
                        print("警告: 提取的数据可能不完整，请人工复核")

                return data

            except json.JSONDecodeError as e:
                if attempt < retry_times - 1:
                    print(f"第{attempt+1}次尝试JSON解析失败，重试...")
                    continue
                else:
                    raise Exception(f"JSON解析失败: {str(e)}\n原始响应: {response}")

            except Exception as e:
                if attempt < retry_times - 1:
                    print(f"第{attempt+1}次尝试失败: {str(e)}，重试...")
                    continue
                else:
                    raise Exception(f"LLM提取失败: {str(e)}")

    def extract_statement_preview(
        self,
        text: str,
        retry_times: int = 2
    ) -> Dict[str, Any]:
        """
        对“对账单 + 系统截图”场景返回适合直接写入表单的预览结果。

        Returns:
            {
                "data": {...},
                "repayment_records": [...],
                "pending_repayment_records": [...],
                "field_sources": {...},
                "warnings": [...],
                "raw_llm_data": {...}
            }
        """
        template_result = extract_statement_template_data(text)
        warnings = list(template_result.get("warnings", []))
        field_sources = dict(template_result.get("field_sources", {}))

        llm_data: Dict[str, Any] = {}
        try:
            llm_data = self.extract_loan_info(text, retry_times=retry_times)
        except Exception as exc:
            warnings.append(f"AI结构化提取失败，已保留规则识别结果：{exc}")

        merged_data = self._merge_statement_data(template_result.get("data", {}), llm_data)
        repayment_records = template_result.get("repayment_records") or self._normalize_repayment_records(
            llm_data.get("repayment_records", [])
        )

        if repayment_records:
            field_sources.setdefault("repayment_records", "对账单流水识别")

        return {
            "data": merged_data,
            "repayment_records": repayment_records,
            "pending_repayment_records": template_result.get("pending_repayment_records", []),
            "field_sources": field_sources,
            "warnings": warnings,
            "raw_llm_data": llm_data,
        }

    def _build_extraction_prompt(self, text: str) -> str:
        """
        构建信息提取的提示词（增强版）

        Args:
            text: 待提取的文本

        Returns:
            str: 完整的提示词
        """
        # 截取文本，避免太长
        max_length = 12000  # 增加到12000字符
        truncated_text = text[:max_length] if len(text) > max_length else text

        prompt = f"""你是一个专业的金融债权文档信息提取专家。请仔细阅读以下文档内容，准确提取贷款相关的所有关键信息。

## 📋 需要提取的字段

### 必填字段（必须在文档中找到）：
1. **loan_amount** - 贷款金额或本金
   - 识别关键词：贷款金额、本金、借款金额、授信额度等
   - 示例：1000000 或 100万 或 1,000,000

2. **annual_interest_rate** - 年利率
   - 识别关键词：年利率、利率、执行利率、约定利率等
   - 示例：4.35、4.35%、0.0435
   - 注意：如果是月利率，需要乘以12

3. **start_date** - 贷款起始日或合同生效日
   - 识别关键词：起息日、生效日、起始日、放款日等
   - 格式：YYYY-MM-DD
   - 示例：2024-01-01

4. **end_date** - 贷款到期日或合同到期日
   - 识别关键词：到期日、还款期限届满日等
   - 格式：YYYY-MM-DD
   - 示例：2025-01-01

5. **remaining_principal** - 剩余本金或未还本金
   - 识别关键词：剩余本金、未还本金、当前余额、尚欠本金等
   - 示例：800000

### 可选字段（如果文档中有则提取）：
6. **accrued_interest** - 已计利息或应付利息
   - 识别关键词：已计利息、应付利息、利息余额等

7. **penalty_interest** - 罚息或违约金
   - 识别关键词：罚息、违约金、逾期利息等

8. **compound_interest** - 复利
   - 识别关键词：复利、利息的利息等

9. **rate_adjustments** - 利率调整记录（数组）
   - 识别关键词：利率调整、调整后利率等
   - 格式：[{{"date": "2024-06-01", "rate": 3.95}}]

10. **repayment_records** - 还款记录（数组）
    - 识别关键词：还款记录、还款明细、偿还情况等
    - 格式：[{{"date": "2024-06-30", "type": "interest", "amount": 10000}}]
    - type可选值：principal(本金)、interest(利息)、penalty(罚息)、compound(复利)、fee(费用)

## 📝 输出格式要求

请严格按照以下JSON格式输出，不要添加任何其他文字：

```json
{{
  "loan_amount": 数字或null,
  "annual_interest_rate": 数字或null,
  "start_date": "YYYY-MM-DD格式或null",
  "end_date": "YYYY-MM-DD格式或null",
  "remaining_principal": 数字或null,
  "accrued_interest": 数字或0,
  "penalty_interest": 数字或0,
  "compound_interest": 数字或0,
  "rate_adjustments": [],
  "repayment_records": []
}}
```

## 🔍 提取技巧

1. **数字识别**：
   - 中文数字转换：一千万 → 10000000，壹佰万 → 1000000
   - 混合格式：1,234.56 → 1234.56

2. **日期识别**：
   - 中文日期：二〇二四年一月一日 → 2024-01-01
   - 多种格式：2024.1.1、2024/1/1 → 2024-01-01

3. **利率识别**：
   - 百分比：4.35% → 4.35
   - 千分比：15‰ → 1.5
   - 月转年：月息0.3% → 3.6
   - 只有在文档中明确出现利率字段时才提取；如果文档中没有明确利率，不要根据还款金额、利息金额或其他数据推算

4. **金额识别**：
   - 大写数字：壹万元 → 10000元
   - 单位换算：万元 → 乘以10000

## ⚠️ 重要规则

1. **只输出JSON**：不要有Markdown代码块标记（不要用```json包裹）
2. **不要编造**：如果文档中明确没有某个字段，设为null
3. **保留精度**：金额保留2位小数，利率保留4-6位小数
4. **数组为空**：如果没有还款记录或利率调整，设为空数组[]
5. **确保格式**：所有字符串用双引号，数字不用引号

## 📄 待提取的文档内容：

{truncated_text}

请输出提取结果（纯JSON格式，不要有任何其他文字）：
"""
        return prompt

    def _apply_rule_based_fallbacks(
        self,
        text: str,
        data: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        对LLM结果进行规则兜底，提升OCR噪声文档提取稳定性

        Args:
            text: 原始OCR文本
            data: LLM提取结果

        Returns:
            Dict[str, Any]: 处理后的结构化结果
        """
        normalized_text = self._normalize_ocr_text(text)
        merged = dict(data or {})

        merged.setdefault("rate_adjustments", [])
        merged.setdefault("repayment_records", [])
        merged.setdefault("accrued_interest", 0)
        merged.setdefault("penalty_interest", 0)
        merged.setdefault("compound_interest", 0)

        for field, value in {
            "loan_amount": self._extract_amount_by_patterns(
                normalized_text,
                [r"贷款金额[:：]?\s*([0-9][0-9,\.\s]*)", r"借款金额[:：]?\s*([0-9][0-9,\.\s]*)"]
            ),
            "remaining_principal": self._extract_amount_by_patterns(
                normalized_text,
                [r"剩余本金[:：]?\s*([0-9][0-9,\.\s]*)", r"未还本金[:：]?\s*([0-9][0-9,\.\s]*)"]
            ),
            "start_date": self._extract_date_by_patterns(
                normalized_text,
                [r"合同起始日[期]?[，,:：]?\s*([0-9]{4}[年./-][0-9]{1,2}[月./-][0-9]{1,2}日?)"]
            ),
            "end_date": self._extract_date_by_patterns(
                normalized_text,
                [r"合同到期日(?:期)?[，,:：]?\s*([0-9]{4}[年./-][0-9]{1,2}[月./-][0-9]{1,2}日?)"]
            ),
            "annual_interest_rate": self._extract_rate_by_patterns(normalized_text),
        }.items():
            if merged.get(field) in (None, "", []):
                merged[field] = value

        merged["repayment_records"] = self._normalize_repayment_records(merged.get("repayment_records", []))
        merged["rate_adjustments"] = self._normalize_rate_adjustments(merged.get("rate_adjustments", []))

        return merged

    def _merge_statement_data(
        self,
        rule_data: Dict[str, Any],
        llm_data: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        合并规则提取与 LLM 提取，规则结果优先。
        """
        merged = {
            "loan_amount": None,
            "annual_interest_rate": None,
            "start_date": None,
            "end_date": None,
            "calculation_date": None,
            "remaining_principal": None,
            "accrued_interest": 0,
            "penalty_interest": 0,
            "compound_interest": 0,
            "rate_adjustments": [],
        }

        llm_data = dict(llm_data or {})
        rule_data = dict(rule_data or {})

        for field in merged:
            llm_value = llm_data.get(field)
            if llm_value not in (None, "", []):
                merged[field] = llm_value

        for field in merged:
            rule_value = rule_data.get(field)
            if rule_value not in (None, "", []):
                merged[field] = rule_value

        return merged

    def _normalize_ocr_text(self, text: str) -> str:
        """
        统一OCR文本中的常见噪声字符
        """
        normalized = text or ""
        replacements = {
            "，": ",",
            "。": ".",
            "：": ":",
            "；": ";",
            "（": "(",
            "）": ")",
            "｜": "|",
            "【": "[",
            "】": "]",
            "“": "\"",
            "”": "\"",
            "‘": "'",
            "’": "'",
        }
        for source, target in replacements.items():
            normalized = normalized.replace(source, target)

        normalized = re.sub(r"[ \t]+", " ", normalized)
        return normalized

    def _extract_amount_by_patterns(self, text: str, patterns: List[str]) -> Optional[float]:
        """
        按模式提取金额
        """
        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                amount = self._parse_number(match.group(1))
                if amount is not None:
                    return amount
        return None

    def _extract_date_by_patterns(self, text: str, patterns: List[str]) -> Optional[str]:
        """
        按模式提取日期
        """
        for pattern in patterns:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                return self._normalize_date_string(match.group(1))
        return None

    def _extract_rate_by_patterns(self, text: str) -> Optional[float]:
        """
        按模式提取年利率，返回百分数形式
        """
        patterns = [
            r"年利率[:：]?\s*([0-9]+(?:\.[0-9]+)?)\s*%",
            r"执行利率[:：]?\s*([0-9]+(?:\.[0-9]+)?)\s*%",
            r"利率[:：]?\s*([0-9]+(?:\.[0-9]+)?)\s*%",
            r"月利率[:：]?\s*([0-9]+(?:\.[0-9]+)?)\s*%",
        ]

        for index, pattern in enumerate(patterns):
            match = re.search(pattern, text, re.IGNORECASE)
            if not match:
                continue

            rate = self._parse_number(match.group(1))
            if rate is None:
                continue

            if index == 3:
                rate *= 12

            return round(rate, 4)

        return None

    def _normalize_repayment_records(self, records: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        统一还款记录格式
        """
        normalized = []
        for record in records:
            normalized_record = dict(record)
            if "date" in normalized_record and normalized_record["date"]:
                normalized_record["date"] = self._normalize_date_string(normalized_record["date"])
            if "amount" in normalized_record:
                amount = self._parse_number(normalized_record["amount"])
                if amount is not None:
                    normalized_record["amount"] = amount
            normalized.append(normalized_record)
        return normalized

    def _normalize_rate_adjustments(self, adjustments: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        统一利率调整记录格式，利率保留百分数形式
        """
        normalized = []
        for adjustment in adjustments:
            normalized_adjustment = dict(adjustment)
            if "date" in normalized_adjustment and normalized_adjustment["date"]:
                normalized_adjustment["date"] = self._normalize_date_string(normalized_adjustment["date"])
            if "rate" in normalized_adjustment:
                rate = self._parse_number(normalized_adjustment["rate"])
                if rate is not None:
                    normalized_adjustment["rate"] = round(rate, 4)
            normalized.append(normalized_adjustment)
        return normalized

    def _parse_number(self, value: Any) -> Optional[float]:
        """
        将金额或利率文本转成数字
        """
        if value is None:
            return None

        if isinstance(value, (int, float)):
            return float(value)

        text = str(value).strip()
        text = text.replace(",", "").replace(" ", "")
        text = text.replace("。", ".").replace("O", "0").replace("o", "0")
        text = re.sub(r"[^0-9.\-]", "", text)

        if not text or text in {"-", ".", "-."}:
            return None

        try:
            return float(text)
        except ValueError:
            return None

    def _normalize_date_string(self, value: Any) -> Optional[str]:
        """
        将常见OCR日期格式标准化为 YYYY-MM-DD
        """
        if value is None:
            return None

        text = str(value).strip()
        text = text.replace("年", "-").replace("月", "-").replace("日", "")
        text = text.replace(".", "-").replace("/", "-").replace(",", "-")
        text = re.sub(r"[^0-9-]", "", text)
        text = re.sub(r"-{2,}", "-", text).strip("-")

        parts = text.split("-")
        if len(parts) >= 3:
            year, month, day = parts[0], parts[1], parts[2]
            return f"{int(year):04d}-{int(month):02d}-{int(day):02d}"

        return None

    def _call_ollama(self, prompt: str) -> str:
        """
        调用Ollama API

        Args:
            prompt: 提示词

        Returns:
            str: 模型响应
        """
        client = self._build_client()

        try:
            response = client.chat(
                model=self.model,
                messages=[{"role": "user", "content": prompt}],
                options={
                    "temperature": self.temperature,
                    "num_predict": 2000,  # 限制输出长度
                }
            )

            return response["message"]["content"].strip()

        except Exception as e:
            raise Exception(f"Ollama API调用失败: {str(e)}")

    def _parse_json_response(self, response: str) -> Dict[str, Any]:
        """
        解析LLM响应中的JSON

        Args:
            response: 原始响应文本

        Returns:
            Dict: 解析后的JSON对象
        """
        # 尝试直接解析
        try:
            return json.loads(response)
        except json.JSONDecodeError:
            pass

        # 尝试提取JSON代码块
        json_match = re.search(r'```json\s*(.*?)\s*```', response, re.DOTALL)
        if json_match:
            try:
                return json.loads(json_match.group(1))
            except json.JSONDecodeError:
                pass

        # 尝试提取花括号内容
        brace_match = re.search(r'\{.*\}', response, re.DOTALL)
        if brace_match:
            try:
                return json.loads(brace_match.group(0))
            except json.JSONDecodeError:
                pass

        # 都失败了，抛出异常
        raise json.JSONDecodeError("无法从响应中提取有效JSON", response, 0)

    def _validate_extracted_data(self, data: Dict[str, Any]) -> Dict[str, Any]:
        """
        验证提取的数据

        Args:
            data: 提取的数据

        Returns:
            Dict: 验证结果 {"is_valid": bool, "errors": List[str]}
        """
        result = {
            "is_valid": True,
            "errors": [],
            "warnings": []
        }

        # 检查必填字段
        required_fields = [
            "loan_amount",
            "start_date",
            "end_date"
        ]

        for field in required_fields:
            if field not in data or data[field] is None:
                result["is_valid"] = False
                result["errors"].append(f"缺少必填字段: {field}")

        # 检查数据类型
        type_checks = {
            "loan_amount": (int, float),
            "annual_interest_rate": (int, float),
            "remaining_principal": (int, float),
            "start_date": str,
            "end_date": str
        }

        for field, expected_type in type_checks.items():
            if field in data and data[field] is not None:
                if not isinstance(data[field], expected_type):
                    result["warnings"].append(
                        f"字段 {field} 类型应为 {expected_type}，实际为 {type(data[field])}"
                    )

        return result

    def extract_with_retry(
        self,
        text: str,
        max_retries: int = 3
    ) -> Dict[str, Any]:
        """
        带重试的信息提取

        Args:
            text: 输入文本
            max_retries: 最大重试次数

        Returns:
            Dict: 提取的数据
        """
        return self.extract_loan_info(text, retry_times=max_retries)

    def chat_completion(
        self,
        messages: List[Dict[str, str]],
        system_prompt: Optional[str] = None
    ) -> str:
        """
        通用对话接口

        Args:
            messages: 消息列表 [{"role": "user", "content": "..."}]
            system_prompt: 系统提示词（可选）

        Returns:
            str: 模型响应
        """
        client = self._build_client()

        # 构建完整消息列表
        full_messages = []
        if system_prompt:
            full_messages.append({"role": "system", "content": system_prompt})
        full_messages.extend(messages)

        try:
            response = client.chat(
                model=self.model,
                messages=full_messages,
                options={"temperature": self.temperature}
            )

            return response["message"]["content"]

        except Exception as e:
            raise Exception(f"对话调用失败: {str(e)}")


def create_llm_extractor(config: Dict) -> LLMExtractor:
    """
    工厂函数：根据配置创建LLM提取器

    Args:
        config: 配置字典

    Returns:
        LLMExtractor: LLM提取器实例
    """
    ollama_config = config.get("ollama", {})

    return LLMExtractor(
        base_url=ollama_config.get("base_url", "http://localhost:11434"),
        model=ollama_config.get("model", "auto"),
        preferred_models=ollama_config.get("preferred_models", []),
        timeout=ollama_config.get("timeout", 120),
        temperature=ollama_config.get("temperature", 0.1)
    )
