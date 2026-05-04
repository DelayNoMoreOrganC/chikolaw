"""
运行时路径工具
兼容源码运行与 PyInstaller 打包后的目录结构
"""

from __future__ import annotations

import os
import sys
from pathlib import Path


APP_ENV_ROOT = "AC_APP_ROOT"
APP_NAME = "ACDebtCalculator"


def get_resource_root() -> Path:
    """获取资源根目录"""
    if getattr(sys, "frozen", False):
        meipass = getattr(sys, "_MEIPASS", "")
        if meipass:
            return Path(meipass).resolve()
        return Path(sys.executable).resolve().parent

    return Path(__file__).resolve().parent.parent


def get_app_root() -> Path:
    """获取应用运行目录"""
    env_root = os.getenv(APP_ENV_ROOT)
    if env_root:
        return Path(env_root).resolve()

    if getattr(sys, "frozen", False):
        return Path(sys.executable).resolve().parent

    return Path(__file__).resolve().parent.parent


def get_resource_path(*parts: str) -> Path:
    """获取资源文件路径"""
    return get_resource_root().joinpath(*parts)


def get_data_root() -> Path:
    """获取可写数据目录"""
    app_root = get_app_root()
    candidate = app_root / "data"

    try:
        candidate.mkdir(parents=True, exist_ok=True)
        test_file = candidate / ".write_test"
        test_file.write_text("ok", encoding="utf-8")
        test_file.unlink(missing_ok=True)
        return candidate
    except OSError:
        local_appdata = os.getenv("LOCALAPPDATA")
        if local_appdata:
            fallback = Path(local_appdata) / APP_NAME / "data"
        else:
            fallback = app_root / "data"
        fallback.mkdir(parents=True, exist_ok=True)
        return fallback
