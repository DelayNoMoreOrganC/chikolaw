"""
南海农商银行PDF数据提取模块
从PDF对账单中提取贷款信息
"""

import pdfplumber
from decimal import Decimal
from datetime import datetime, date
from typing import List, Dict, Any, Optional
from pathlib import Path


class NHRCBPDFExtractor:
    """南海农商银行PDF数据提取器"""

    def __init__(self):
        self.flow_data: List[Dict[str, Any]] = []

    def read_pdf(self, file_path: str) -> bool:
        """
        读取PDF文件

        Args:
            file_path: PDF文件路径

        Returns:
            bool: 是否成功读取
        """
        try:
            with pdfplumber.open(file_path) as pdf:
                # 遍历所有页面
                for page in pdf.pages:
                    # 提取表格
                    tables = page.extract_tables()
                    if tables:
                        for table in tables:
                            # 处理表格数据
                            self._process_table(table)

            if not self.flow_data:
                print("PDF中未找到表格数据")
                return False

            return True

        except Exception as e:
            print(f"读取PDF文件失败: {e}")
            import traceback
            traceback.print_exc()
            return False

    def _process_table(self, table: List[List[Any]]):
        """处理表格数据"""
        if not table or len(table) < 2:
            return

        # 跳过表头
        for row in table[1:]:
            if not row or len(row) < 8:
                continue

            # 提取序号
            seq_no = row[0] if row[0] else None
            if seq_no is None:
                continue

            # 尝试转换为数字
            try:
                int(str(seq_no).strip())
            except:
                continue

            # 构建数据行
            data_row = {
                '序号': str(seq_no).strip() if seq_no else '',
                '流水号': str(row[1]).strip() if len(row) > 1 and row[1] else '',
                '借贷标志': str(row[2]).strip() if len(row) > 2 and row[2] else '',
                '交易类型': str(row[3]).strip() if len(row) > 3 and row[3] else '',
                '实际发生日': row[4] if len(row) > 4 and row[4] else None,
                '交易金额': row[5] if len(row) > 5 and row[5] else None,
                '起息日': row[6] if len(row) > 6 and row[6] else None,
                '到期日期': row[7] if len(row) > 7 and row[7] else None,
                '还款方式': row[8] if len(row) > 8 and row[8] else None,
                '摘要信息': row[9] if len(row) > 9 and row[9] else None
            }

            self.flow_data.append(data_row)

        # 按序号排序
        self.flow_data.sort(key=lambda r: int(r['序号']) if str(r['序号']).isdigit() else 0)

    def extract_loan_info(self) -> Dict[str, Any]:
        """提取贷款基本信息"""
        if not self.flow_data:
            raise ValueError("没有数据可提取")

        # 从最后一行提取贷款信息（通常是放款记录）
        loan_row = self.flow_data[-1]

        # 解析贷款金额
        amount_str = str(loan_row['交易金额']) if loan_row['交易金额'] else '0'
        amount_str = amount_str.replace(',', '').replace(' ', '')
        loan_amount = float(amount_str)

        return {
            'loan_amount': loan_amount,
            'start_date': self._parse_date(loan_row['起息日']),
            'end_date': self._parse_date(loan_row['到期日期']),
            'annual_interest_rate': 0.048,
            'calculation_date': datetime.now().strftime('%Y-%m-%d')
        }

    def extract_repayments(self) -> List[Dict[str, Any]]:
        """提取还款记录"""
        repayments = []

        for row in self.flow_data[:-1]:
            trans_type = str(row['交易类型']) if row['交易类型'] else ''
            amount_str = str(row['交易金额']) if row['交易金额'] else '0'
            date_val = row['实际发生日']

            # 检查是否是借方（负数）
            is_negative = '-' in amount_str
            amount_str = amount_str.replace('-', '').replace(',', '').replace(' ', '')

            try:
                amount = float(amount_str)
            except:
                continue

            # 借方本金还款
            if is_negative and ('本金' in trans_type or 'P' in trans_type):
                if date_val:
                    repayments.append({
                        'date': self._parse_date(date_val),
                        'amount': abs(amount),
                        'type': 'principal',
                        'description': trans_type
                    })

        # 按日期排序
        repayments.sort(key=lambda r: r['date'])

        return repayments

    def extract_rate_adjustments(self) -> List[Dict[str, Any]]:
        """提取利率调整记录"""
        return []

    def extract_all(self) -> Dict[str, Any]:
        """提取所有数据"""
        loan_info = self.extract_loan_info()
        repayments = self.extract_repayments()
        rate_adjustments = self.extract_rate_adjustments()

        return {
            'loan_info': loan_info,
            'repayments': repayments,
            'rate_adjustments': rate_adjustments,
            'flow_count': len(self.flow_data)
        }

    def _parse_date(self, date_val) -> Optional[str]:
        """解析日期值"""
        if date_val is None:
            return None

        if isinstance(date_val, datetime):
            return date_val.strftime('%Y-%m-%d')

        if isinstance(date_val, date):
            return datetime.combine(date_val, datetime.min.time()).strftime('%Y-%m-%d')

        if isinstance(date_val, str):
            for fmt in ['%Y-%m-%d', '%Y-%m-%d %H:%M:%S', '%Y/%m/%d']:
                try:
                    return datetime.strptime(date_val, fmt).strftime('%Y-%m-%d')
                except:
                    continue

        return str(date_val)

    def get_summary(self) -> Dict[str, Any]:
        """获取数据提取摘要"""
        if not self.flow_data:
            return {'status': 'error', 'message': '没有数据'}

        loan_info = self.extract_loan_info()
        repayments = self.extract_repayments()

        total_repaid = sum(r['amount'] for r in repayments)

        return {
            'status': 'success',
            'loan_amount': loan_info['loan_amount'],
            'start_date': loan_info['start_date'],
            'end_date': loan_info['end_date'],
            'total_repayments': len(repayments),
            'total_repaid': total_repaid,
            'remaining_principal': loan_info['loan_amount'] - total_repaid,
            'flow_count': len(self.flow_data)
        }


def extract_from_pdf(uploaded_file) -> Optional[Dict[str, Any]]:
    """
    从Streamlit上传的PDF文件中提取数据

    Args:
        uploaded_file: Streamlit上传的PDF文件对象

    Returns:
        Optional[Dict]: 提取的数据，失败返回None
    """
    import tempfile
    import os

    try:
        # 保存到临时文件
        with tempfile.NamedTemporaryFile(delete=False, suffix='.pdf') as tmp_file:
            try:
                tmp_file.write(uploaded_file.getbuffer())
            except AttributeError:
                tmp_file.write(uploaded_file.read())
            tmp_path = tmp_file.name

        # 提取数据
        extractor = NHRCBPDFExtractor()
        if not extractor.read_pdf(tmp_path):
            try:
                os.unlink(tmp_path)
            except:
                pass
            return None

        result = extractor.extract_all()

        # 清理临时文件
        try:
            os.unlink(tmp_path)
        except:
            pass

        return result

    except Exception as e:
        import traceback
        print(f"PDF提取失败: {e}")
        traceback.print_exc()
        return None
