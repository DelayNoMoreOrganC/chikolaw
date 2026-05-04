@echo off
chcp 65001 >nul
echo 债权精算工具启动中...
cd /d %~dp0
start http://localhost:8899
"C:\Users\Administrator\AppData\Local\Programs\Python\Python314\Scripts\streamlit.exe" run app_streamlit_simple.py --server.port 8899 --server.headless true
pause
