@echo off
chcp 65001
setlocal enabledelayedexpansion

REM ===============================
REM 参数说明
REM build.bat android xiaomi dev debug CocosCreator.exe true true
REM build.bat web official test debug CocosCreator.exe true true
REM ===============================

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
  echo ❌ 错误: 未发现渠道配置:%channel_config%
  exit /b 1
)

REM ===============================
REM 注入 ChannelConfig.ts
REM ===============================
node %js_root%gen_channel_config.js %channel_config% %channel_ts%
if errorlevel 1 (
  echo ❌ 错误: 注入 ChannelConfig.ts 失败
  exit /b 1
)
echo ===========  注入 ChannelConfig.ts 完成: %channel_config% ===========

REM ===============================
REM 安装项目依赖
REM ===============================
if exist "package.json" (
  echo =========== Installing dependencies ===========
  call npm install --registry https://registry.npmmirror.com
  if errorlevel 1 (
    echo ❌ 错误: npm 安装失败 errorlevel: %ERRORLEVEL%
    exit /b 1
  )
) else (
  echo 未发现package.json, 跳过 npm install
)

REM ===============================
REM 选择构建参数
REM ===============================
if "%platform%"=="android" (
  set build_args=platform=android;configPath=build-config\android\buildConfig_android.json
)

if "%platform%"=="web" (
  set build_args=platform=web-mobile;configPath=build-config\web\buildConfig_web-mobile.json
)

if "%platform%"=="ios" (
  set build_args=platform=ios;configPath=build-config\ios\buildConfig_ios.json
)

REM 生成 apk 流程（必须双构建）
echo.
echo =========== 构建 ===========
echo   platform: %platform%
echo   channel : %channel%
echo   env     : %env%
echo   mode    : %mode%
echo   creator : %creator%
echo   apk     : %apk%
echo   clean   : %clean%
echo =========== 构建 ===========
echo.

REM 1. 第一次构建（生成最新资源）
%creator% --project %cd% --build "%build_args%;mode=%mode%"
if errorlevel 36 (
  if "%platform%"=="web" (
    @REM web 构建成功就结束 不需要后续流程
    echo 🎉 构建任务全部完成
    exit /b 0
  ) else ( 
    echo ✅ 第1次构建完成: code 36
  )
) else (
    echo ❌ 错误: 第1次构建失败
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
  echo 未发现version.manifest,默认热更新版本: 0.0.0.0
  set last_version=0.0.0.0
)

if "%last_version%"=="" (
  echo ❌ 错误: 读取线上最新热更版本号失败
  exit /b 1
)

REM 3. 读取热更新地址
set hotupdate_url=
for /f %%i in ('node %js_root%read_value.js %channel_config% hotupdateUrl') do (
  set hotupdate_url=%%i
)

if "%hotupdate_url%"=="" (
  echo ❌ 错误: 读取热更新地址失败
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
  echo ❌ 错误: 生成热更新 manifest 失败
  exit /b 1
)

REM 只是热更新的文件 就不需要第二次构建
if "%apk%"=="false" (
  echo ✅ 生成 %bundleName% 热更新文件完成
  exit /b 0
)

REM 5. 第二次构建（正式 APK）
%creator% --project %cd% --build "%build_args%;mode=%mode%"
if errorlevel 36 (
  echo ✅ 第2次构建完成: code 36
) else (
    echo ❌ 错误: 第2次构建失败
    exit /b 1
)

echo ========== 注入代码到 main.js ==========
node %js_root%gen_main.js
if errorlevel 1 (
    echo ❌ 错误: 注入 main.js 失败
    exit /b 1
)
echo ✅ main.js 代码注入完成

echo 🎉 构建任务全部完成
exit /b 0

:usage
echo.
echo 用法:
echo   build.bat ^<platform^> ^<channel^> ^<env^> ^<mode^> ^<creator^> ^<apk^> ^<clean^>
echo.
echo 示例:
echo   build.bat android xiaomi dev debug CocosCreator.exe true true
echo   build.bat android huawei prod debug CocosCreator.exe true true
echo   build.bat web official test debug CocosCreator.exe true true
exit /b 1