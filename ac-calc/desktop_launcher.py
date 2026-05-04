"""
债权精算工具 v0.3 - 桌面启动器（EXE入口）
兼容 PyInstaller 打包环境和源码直接运行。
"""
import sys, os, webbrowser, threading, time
from pathlib import Path

# 判断是否在PyInstaller打包环境中
if getattr(sys, 'frozen', False):
    # 打包EXE模式：程序文件在 sys._MEIPASS (_internal 目录)
    ROOT = Path(sys._MEIPASS)
else:
    # 源码模式：程序文件在当前目录
    ROOT = Path(__file__).parent

os.chdir(str(ROOT))

# 打印启动信息（在控制台窗口中可见）
print("=" * 50)
print("   债权精算工具 v0.3")
print("=" * 50)
print()
print("正在启动...")
print()

# 验证关键文件存在
app_script = ROOT / "app_streamlit_simple.py"
if not app_script.exists():
    print(f"错误: 找不到 {app_script}")
    print(f"当前目录: {os.getcwd()}")
    print(f"_MEIPASS: {getattr(sys, '_MEIPASS', 'N/A')}")
    print()
    print("按回车键退出...")
    sys.stdin.readline()
    sys.exit(1)

print(f"已加载程序文件: {app_script.name}")
print()
print("正在启动服务器 (http://localhost:8899)...")
print("浏览器将自动打开，请稍候...")
print()

# 在新线程中打开浏览器
def open_browser():
    time.sleep(3)
    try:
        webbrowser.open("http://localhost:8899")
    except Exception:
        pass

threading.Thread(target=open_browser, daemon=True).start()

# 使用 Streamlit CLI 启动
from streamlit.web import cli as stcli

sys.argv = [
    "streamlit",
    "run",
    str(app_script),
    "--global.developmentMode", "false",
    "--server.port", "8899",
    "--server.headless", "true",
    "--browser.gatherUsageStats", "false",
    "--server.enableXsrfProtection", "false",
]

try:
    sys.exit(stcli.main())
except KeyboardInterrupt:
    print()
    print("服务已停止")
    sys.exit(0)
except Exception as e:
    print()
    print(f"启动失败: {e}")
    print()
    print("按回车键退出...")
    try:
        sys.stdin.readline()
    except Exception:
        pass
    sys.exit(1)
