# -*- mode: python ; coding: utf-8 -*-
"""
PyInstaller spec for 债权精算工具 v0.3
打包为文件夹（推荐），用户双击债权精算工具.exe即可使用。
"""
import os
from PyInstaller.utils.hooks import collect_data_files, collect_submodules, copy_metadata

# SPEC is a PyInstaller-builtin variable pointing to the spec file path
project_root = os.path.dirname(os.path.abspath(SPEC))

# 收集Streamlit的数据文件
datas = collect_data_files("streamlit")
datas += copy_metadata("streamlit")
datas += [
    # 程序文件
    (os.path.join(project_root, "app_streamlit_simple.py"), "."),
    (os.path.join(project_root, "config.yaml"), "."),
    # 模块
    (os.path.join(project_root, "modules"), "modules"),
    (os.path.join(project_root, "utils"), "utils"),
    # 提示词
    (os.path.join(project_root, "prompts"), "prompts"),
    # 数据目录
    (os.path.join(project_root, "data"), "data"),
]

# 隐式导入
hiddenimports = collect_submodules("streamlit")
hiddenimports += [
    # 核心依赖
    "yaml", "pandas", "openpyxl",
    "fitz", "PIL", "PIL.Image", "PIL.ImageDraw",
    "dateutil", "dateutil.relativedelta",
    "numpy",
    # 自定义模块
    "modules",
    "modules.calculator",
    "modules.excel_exporter",
    "modules.repayment_plan",
    "modules.bank_loan_flow",
    "utils",
    "utils.date_utils",
    "utils.constants",
    "utils.validators",
    "utils.runtime_paths",
]

# 分析
a = Analysis(
    ["desktop_launcher.py"],
    pathex=[project_root],
    binaries=[],
    datas=datas,
    hiddenimports=hiddenimports,
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[
        "tkinter", "matplotlib",
        "scipy", "sympy", "notebook",
        "ipython", "test", "unittest",
    ],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    noarchive=False,
)

pyz = PYZ(a.pure, a.zipped_data)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    name="债权精算工具",
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=False,
    console=True,  # 显示控制台窗口，提示用户启动进度
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon=None,
)

coll = COLLECT(
    exe,
    a.binaries,
    a.zipfiles,
    a.datas,
    strip=False,
    upx=False,
    upx_exclude=[],
    name="债权精算工具",
)
