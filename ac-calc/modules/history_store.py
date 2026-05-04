"""
历史记录存储模块
负责将表单输入、计算结果和导出的 Excel 快照持久化到本地
"""

from __future__ import annotations

import json
import sqlite3
from contextlib import closing
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List, Optional
from uuid import uuid4

import sys

project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from utils.runtime_paths import get_data_root


class HistoryStore:
    """本地历史记录存储"""

    def __init__(self, base_dir: Optional[Path] = None):
        self.base_dir = Path(base_dir or get_data_root() / "history")
        self.snapshot_dir = self.base_dir / "snapshots"
        self.excel_dir = self.base_dir / "excels"
        self.db_path = self.base_dir / "history.db"

        self.base_dir.mkdir(parents=True, exist_ok=True)
        self.snapshot_dir.mkdir(parents=True, exist_ok=True)
        self.excel_dir.mkdir(parents=True, exist_ok=True)

        self._init_db()

    def _connect(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn

    def _init_db(self) -> None:
        with closing(self._connect()) as conn:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS history_records (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    calculation_date TEXT,
                    loan_amount REAL,
                    annual_interest_rate TEXT,
                    start_date TEXT,
                    end_date TEXT,
                    remaining_principal REAL,
                    total_amount REAL,
                    snapshot_path TEXT NOT NULL,
                    excel_path TEXT
                )
                """
            )
            conn.commit()

    def save_record(
        self,
        loan_data: Dict[str, Any],
        repayment_records: List[Dict[str, Any]],
        rate_adjustments: List[Dict[str, Any]],
        calculation_result: Dict[str, Any],
        excel_bytes: Optional[bytes] = None
    ) -> Dict[str, Any]:
        record_id = uuid4().hex
        created_at = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        calculation_date = (
            calculation_result.get("summary", {}).get("calculation_date")
            or loan_data.get("calculation_date", "")
        )
        title = self._build_title(loan_data, calculation_result, created_at)

        snapshot = {
            "id": record_id,
            "title": title,
            "created_at": created_at,
            "loan_data": loan_data,
            "repayment_records": repayment_records,
            "rate_adjustments": rate_adjustments,
            "calculation_result": calculation_result
        }

        snapshot_path = self.snapshot_dir / f"{record_id}.json"
        snapshot_path.write_text(
            json.dumps(snapshot, ensure_ascii=False, indent=2),
            encoding="utf-8"
        )

        excel_path = None
        if excel_bytes:
            excel_path = self.excel_dir / f"{record_id}.xlsx"
            excel_path.write_bytes(excel_bytes)

        with closing(self._connect()) as conn:
            conn.execute(
                """
                INSERT INTO history_records (
                    id, title, created_at, calculation_date, loan_amount,
                    annual_interest_rate, start_date, end_date,
                    remaining_principal, total_amount, snapshot_path, excel_path
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    record_id,
                    title,
                    created_at,
                    calculation_date,
                    self._safe_float(loan_data.get("loan_amount")),
                    str(loan_data.get("annual_interest_rate", "")),
                    loan_data.get("start_date", ""),
                    loan_data.get("end_date", ""),
                    self._safe_float(loan_data.get("remaining_principal")),
                    self._safe_float(calculation_result.get("summary", {}).get("total_amount")),
                    str(snapshot_path),
                    str(excel_path) if excel_path else None
                )
            )
            conn.commit()

        return self.get_record(record_id)

    def list_records(self, limit: int = 100) -> List[Dict[str, Any]]:
        with closing(self._connect()) as conn:
            rows = conn.execute(
                """
                SELECT id, title, created_at, calculation_date, loan_amount,
                       annual_interest_rate, start_date, end_date,
                       remaining_principal, total_amount, snapshot_path, excel_path
                FROM history_records
                ORDER BY datetime(created_at) DESC, id DESC
                LIMIT ?
                """,
                (limit,)
            ).fetchall()

        return [dict(row) for row in rows]

    def get_record(self, record_id: str) -> Optional[Dict[str, Any]]:
        with closing(self._connect()) as conn:
            row = conn.execute(
                """
                SELECT id, title, created_at, calculation_date, loan_amount,
                       annual_interest_rate, start_date, end_date,
                       remaining_principal, total_amount, snapshot_path, excel_path
                FROM history_records
                WHERE id = ?
                """,
                (record_id,)
            ).fetchone()

        if not row:
            return None

        record = dict(row)
        snapshot_path = Path(record["snapshot_path"])
        if snapshot_path.exists():
            record["snapshot"] = json.loads(snapshot_path.read_text(encoding="utf-8"))
        else:
            record["snapshot"] = None

        return record

    def get_excel_bytes(self, record_id: str) -> Optional[bytes]:
        record = self.get_record(record_id)
        if not record:
            return None

        excel_path = record.get("excel_path")
        if not excel_path:
            return None

        path = Path(excel_path)
        if not path.exists():
            return None

        return path.read_bytes()

    def _build_title(
        self,
        loan_data: Dict[str, Any],
        calculation_result: Dict[str, Any],
        created_at: str
    ) -> str:
        calculation_date = (
            calculation_result.get("summary", {}).get("calculation_date")
            or loan_data.get("calculation_date")
            or ""
        )
        remaining_principal = loan_data.get("remaining_principal", "")
        if remaining_principal not in ("", None):
            try:
                remaining_principal = f"{float(remaining_principal):,.2f}元"
            except (TypeError, ValueError):
                remaining_principal = str(remaining_principal)

        start_date = loan_data.get("start_date", "")
        end_date = loan_data.get("end_date", "")
        return f"{created_at} | 暂计日 {calculation_date} | 本金 {remaining_principal} | {start_date}~{end_date}"

    def _safe_float(self, value: Any) -> Optional[float]:
        if value in ("", None):
            return None
        try:
            return float(value)
        except (TypeError, ValueError):
            return None


def create_history_store(base_dir: Optional[Path] = None) -> HistoryStore:
    """工厂函数：创建历史记录存储实例"""
    return HistoryStore(base_dir=base_dir)
