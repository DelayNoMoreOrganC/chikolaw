"""
AC精算 - Flask API 服务
桥接 ZGAI Java 后端与 AC 债权精算引擎
"""
import sys
import os
import json
from pathlib import Path

# 添加项目根目录到 Python 路径
project_root = Path(__file__).parent
sys.path.insert(0, str(project_root))

from flask import Flask, request, jsonify
from flask_cors import CORS
from datetime import date
from decimal import Decimal

from modules.calculator import create_calculator, DebtCalculator
from modules.repayment_plan import build_segment, calc_overdue_penalty_by_plan

app = Flask(__name__)
CORS(app)

class DecimalEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, Decimal):
            return float(obj)
        if isinstance(obj, date):
            return obj.isoformat()
        return super().default(obj)

app.json_encoder = DecimalEncoder

@app.route('/api/calc/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "service": "ac-calc"})

@app.route('/api/calc/compute', methods=['POST'])
def compute():
    """
    债权精算计算
    请求体:
    {
        "principal": 1000000.00,          # 本金
        "annual_rate": 0.05,              # 年利率
        "penalty_rate": 0.07,             # 罚息年利率
        "start_date": "2020-01-01",       # 起算日
        "end_date": "2024-06-30",         # 截止日
        "repayment_records": [            # 还款记录（可选）
            {"date": "2023-06-21", "amount": 50000.00, "type": "normal"}
        ],
        "rate_adjustments": []            # 利率调整记录（可选）
    }
    """
    try:
        data = request.get_json(force=True)
        
        # 创建计算器配置
        calc_config = {
            "calculation": {
                "day_count_convention": "actual/360",
                "date_calculation": "head_exclusive",
                "penalty_rate_multiplier": 1.5,
                "enable_compound_interest": True,
                "enable_penalty_interest": True,
                "decimal_precision": 2
            }
        }
        calculator = create_calculator(calc_config)
        
        # 准备贷款数据
        loan_data = {
            "loan_amount": data.get('principal'),
            "annual_interest_rate": data.get('annual_rate'),
            "start_date": data.get('start_date'),
            "end_date": data.get('end_date'),
            "remaining_principal": data.get('remaining_principal', 0),
            "accrued_interest": data.get('accrued_interest', 0)
        }
        
        # 准备还款记录
        repayments = []
        for record in data.get('repayment_records', []):
            repayments.append({
                "date": record['date'],
                "amount": record['amount'],
                "type": record.get('type', 'normal'),
                "description": record.get('description', '')
            })
        
        # 执行计算
        result = calculator.calculate(
            loan_data=loan_data,
            repayment_records=repayments,
            rate_adjustments=data.get('rate_adjustments', [])
        )
        
        return jsonify({
            "code": 200,
            "data": {
                "summary": {
                    "principal": float(loan_data["loan_amount"]),
                    "total_interest": float(result.get('total_interest', 0)),
                    "total_penalty": float(result.get('total_penalty', 0)),
                    "total_compound": float(result.get('total_compound_interest', 0)),
                    "total_amount": float(result.get('total_amount', 0)),
                    "period_count": len(result.get('periods', []))
                },
                "periods": result.get('periods', []),
                "detail": result.get('detail', {})
            }
        })
    except Exception as e:
        return jsonify({
            "code": 500,
            "message": f"计算失败: {str(e)}"
        }), 500

@app.route('/api/calc/batch', methods=['POST'])
def batch_compute():
    """批量计算"""
    data = request.get_json(force=True)
    results = []
    errors = []
    
    for i, item in enumerate(data.get('items', [])):
        try:
            # 复用 compute 逻辑
            principal = Decimal(str(item.get('principal', 0)))
            annual_rate = float(item.get('annual_rate', 0.05))
            penalty_rate = float(item.get('penalty_rate', annual_rate * 1.5))
            start_date = date.fromisoformat(item.get('start_date'))
            end_date = date.fromisoformat(item.get('end_date'))
            
            calculator = create_calculator(
                principal=principal,
                annual_rate=annual_rate,
                penalty_rate=penalty_rate,
                start_date=start_date,
                end_date=end_date
            )
            
            for record in item.get('repayment_records', []):
                calculator.add_repayment(
                    date=date.fromisoformat(record['date']),
                    amount=Decimal(str(record['amount'])),
                    repayment_type=record.get('type', 'normal')
                )
            
            result = calculator.calculate()
            results.append({
                "index": i,
                "summary": {
                    "total_interest": float(result.get('total_interest', 0)),
                    "total_penalty": float(result.get('total_penalty', 0)),
                    "total_amount": float(result.get('total_amount', 0))
                }
            })
        except Exception as e:
            errors.append({"index": i, "error": str(e)})
    
    return jsonify({
        "code": 200,
        "data": {"results": results, "errors": errors}
    })

if __name__ == '__main__':
    port = int(os.environ.get('AC_CALC_PORT', 5100))
    debug = os.environ.get('AC_CALC_DEBUG', 'false').lower() == 'true'
    print(f"AC精算服务启动于 http://0.0.0.0:{port}")
    app.run(host='0.0.0.0', port=port, debug=debug)
