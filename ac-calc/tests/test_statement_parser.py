"""
对账单模板解析测试
"""

import unittest
import sys
from pathlib import Path

project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from modules.statement_parser import extract_statement_template_data, parse_amount


SAMPLE_STATEMENT_TEXT = """
中国建设银行个人贷款对账单
贷款金额: 400, 000. 00
合同起始日， 2023年10月17日     合同到期日期: 2024年10月17日
查询开始日期， 2023年07月23日    查询结束日期，2024年07月22日
3 _|2023年10月21日|自动扣款入账|188.88 |0.00 |188.88 |0. 00 |400, 000.00|1082014201697743330099510
4 _|2023年11月21日|自动扣款入账|1 463.89 |0.00 |1, 463. 89 |0.00 |400, 000. 00|1082017381700416848381076
5 |2023年12月21日|自动扣款入账|1, 416.67 |0.00 |1, 416. 67 |0.00 |400, 000. 00|1082019471703013137754411
6 |2024年01月21日|自动扣款入账|1, 463.89 |0. 00 |1, 463.89 |0. 00 |400, 000.00|1082011931705689552370808
7 |2024年02月21日|自动扣款入账|L, 463.89 |0.00 | 463. 89 |0. 00 |400 000.00|1082014841708368040322163
8 |2024年03月21日|自动扣款入账|1.369. 44 |0.00 |1.369. 44 |0. 00 |400, 000. 00|1082015991710879300007234
9 |2024年04月21日|自动扣款入账|1, 463.89 |0.00 |1, 463.89 |0 00 |400, 000. 00|1082018091713558173784033
"""


class TestStatementParser(unittest.TestCase):
    def test_extract_statement_template_data(self):
        result = extract_statement_template_data(SAMPLE_STATEMENT_TEXT)

        self.assertEqual(result["data"]["loan_amount"], 400000.0)
        self.assertEqual(result["data"]["start_date"], "2023-10-17")
        self.assertEqual(result["data"]["end_date"], "2024-10-17")
        self.assertEqual(result["data"]["calculation_date"], "2024-07-22")
        self.assertEqual(result["data"]["remaining_principal"], 400000.0)

        repayment_records = result["repayment_records"]
        self.assertEqual(len(repayment_records), 7)
        self.assertEqual(repayment_records[0]["date"], "2023-10-21")
        self.assertEqual(repayment_records[0]["type"], "interest")
        self.assertEqual(repayment_records[0]["amount"], 188.88)
        self.assertEqual(repayment_records[-1]["date"], "2024-04-21")
        self.assertEqual(repayment_records[-1]["amount"], 1463.89)
        self.assertEqual(result["pending_repayment_records"], [])

    def test_parse_amount_handles_common_ocr_noise(self):
        self.assertEqual(parse_amount("L, 463.89"), 1463.89)
        self.assertEqual(parse_amount("1.369. 44"), 1369.44)
        self.assertEqual(parse_amount("1, 463.89 ."), 1463.89)


if __name__ == "__main__":
    unittest.main()
