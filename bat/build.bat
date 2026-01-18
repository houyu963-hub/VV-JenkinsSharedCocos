@echo off
setlocal enabledelayedexpansion

REM ===============================
REM 参数说明
REM build.bat android xiaomi dev debug CocosCreator.exe true true
REM build.bat web official test debug CocosCreator.exe true true
REM ===============================

REM 当前目录
echo -------------------------------------------------------------------
set current_dir=%cd%
echo %current_dir%

if "%1"=="" goto usage
if "%2"=="" goto usage
if "%3"=="" goto usage
if "%4"=="" goto usage
if "%5"=="" goto usage

set platform=%1
set channel=%2
set env=%3
set mode=%4
set creator=%5
set apk=%6
set clean=%7

REM ===============================
REM 环境名归一化
REM ===============================
if "%env%"=="prod" set env=prod
if "%env%"=="test" set env=test
if "%env%"=="dev" set env=dev
if "%mode%"=="debug" set mode=debug
if "%mode%"=="release" set mode=release
if "%clean%"=="true" set clean=true
if "%clean%"=="false" set clean=false
if "%apk%"=="true" set apk=true
if "%apk%"=="false" set apk=false

REM ===============================
REM 渠道配置文件
REM ===============================
if "%env%"=="dev"  set CONFIG_NAME=dev.json
if "%env%"=="test" set CONFIG_NAME=test.json
if "%env%"=="prod" set CONFIG_NAME=prod.json

set channel_config=build-config\%platform%\%channel%\%CONFIG_NAME%
set channel_ts=assets\frame\config\ChannelConfig.ts

set js_root=jenkins-shared-cocos\src\org\cocos\js\

if not exist "%channel_config%" (
  echo ❌ Error: Channel config not found: %channel_config%
  exit /b 1
)

REM ===============================
REM 注入 ChannelConfig.ts
REM ===============================
node %js_root%gen_channel_config.js %channel_config% %channel_ts%
if errorlevel 1 (
  echo ❌ Error: Injection of ChannelConfig.ts failed
  exit /b 1
)
echo ===========  Injection of ChannelConfig.ts completed: %channel_config% ===========

REM ===============================
REM 安装项目依赖
REM ===============================
if exist "package.json" (
  echo =========== Installing dependencies ===========
  call npm install --registry https://registry.npmmirror.com
  if errorlevel 1 (
    echo ❌ Error: npm installation failed, errorlevel: %ERRORLEVEL%
    exit /b 1
  )
) else (
  echo package.json not found, skipping npm install
)

REM 生成 apk 流程（必须双构建）
echo.
echo =========== Build ===========
echo   platform: %platform%
echo   channel : %channel%
echo   env     : %env%
echo   mode    : %mode%
echo   creator : %creator%
echo   apk     : %apk%
echo   clean   : %clean%
echo =========== Build ===========
echo.

REM 1. 第一次构建（生成最新资源）
%creator% --project %cd% --build "%build_args%;mode=%mode%"
if errorlevel 36 (
  if "%platform%"=="web" (
    @REM web 构建成功就结束 不需要后续流程
    echo 🎉 All build tasks completed
    exit /b 0
  ) else ( 
    echo ✅ 1st build completed: code 36
  )
) else (
    echo ❌ Error: 1st build failed
    exit /b 1
)

REM 2. 读取线上最新的热更版本号
set last_version_path=..\hotupdate\hall\version.manifest
set last_version=
if exist %last_version_path% (
  for /f %%i in ('node %js_root%read_value.js ..\hotupdate\hall\version.manifest version') do (
    set last_version=%%i
  )
) else (
  echo version.manifest not found, default hot update version: 0.0.0.0
  set last_version=0.0.0.0
)

if "%last_version%"=="" (
  echo ❌ Error: Failed to read online hot update version number
  exit /b 1
)

REM 3. 读取热更新地址
set hotupdate_url=
for /f %%i in ('node %js_root%read_value.js %channel_config% hotupdateUrl') do (
  set hotupdate_url=%%i
)

if "%hotupdate_url%"=="" (
  echo ❌ Error: Failed to read hot update address
  exit /b 1
)

REM 4. 生成热更新 manifest
set savea_artifacts_dir = 
if "%apk%"=="false" (
  set publish_root = \..\..\publish
  set savea_artifacts_dir=publish_root\hotupdate\%platform%\%channel%\%env%\%bundleName%\
) else if "%apk%"=="true" (
  set savea_artifacts_dir=.\assets\resources\manifest\hall\
)
call jenkins-shared-cocos\bat\gen_hotupdate.bat hall %last_version% %hotupdate_url% %apk% %savea_artifacts_dir%

if errorlevel 1 (
  echo ❌ Error: Failed to generate hot update manifest
  exit /b 1
)

REM 只是热更新的文件 就不需要第二次构建
if "%apk%"=="false" (
  echo ✅ Generation of %bundleName% hot update files completed
  exit /b 0
)

REM 5. 第二次构建（正式 APK）
%creator% --project %cd% --build "%build_args%;mode=%mode%"
if errorlevel 36 (
  echo ✅ 2nd build completed: code 36
) else (
    echo ❌ Error: 2nd build failed
    exit /b 1
)

echo ========== Inject code to main.js ==========
node %js_root%gen_main.js
if errorlevel 1 (
    echo ❌ Error: Injection of main.js failed
    exit /b 1
)
echo ✅ main.js code injection completed

echo 🎉 All build tasks completed
exit /b 0

:usage
echo.
echo Usage:
echo   build.bat ^<platform^> ^<channel^> ^<env^> ^<mode^> ^<creator^> ^<apk^> ^<clean^>
echo.
echo Example:
echo   build.bat android xiaomi dev debug CocosCreator.exe true true
echo   build.bat android huawei prod debug CocosCreator.exe true true
echo   build.bat web official test debug CocosCreator.exe true true
exit /b 1