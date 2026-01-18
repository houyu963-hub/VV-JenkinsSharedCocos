@echo off

REM ===============================
REM 参数说明
REM ===============================

if "%1"=="" goto usage
if "%2"=="" goto usage
if "%3"=="" goto usage
if "%4"=="" goto usage
if "%5"=="" goto usage
if "%6"=="" goto usage

REM bundle name
set bundleName=%1
REM 版本号
set version=%2
REM 热更新地址
set hotupdateUrl=%3
REM 构建类型
set apk=%4
REM 产物保存目录
set saveAartifactsDir=%5

REM ===============================
REM 热更新参数
REM ===============================
echo.
echo =========== Hotupdate Building ===========
echo   bundleName  :%bundleName%
echo   version     :%version%
echo   hotupdateUrl:%hotupdateUrl%
echo   apk         :%apk%
echo   saveAartifactsDir:%saveAartifactsDir%
echo =========== Hotupdate Building ===========
echo.

REM 项目路径
set projectPath=%cd%

REM 资源根目录
set assetsRootPath=%projectPath%\build\android\data\assets\

REM 哪些bundle需要放进manifest中
if "%bundleName%"=="hall" (
    set resourceFolder="src","jsb-adapter","assets\internal","assets\resources","assets\main"
    REM 如果需要放入其他bundle 可以自行修改
    REM set resourceFolder="src","jsb-adapter","assets\internal","assets\resources","assets\main","assets\common","assets\loading","assets\hall","assets\mahjong"
) else (
    set resourceFolder=["%bundleName%"]
)

set UPDATE_URL=%hotupdateUrl%\%bundleName%\
set ASSETSROOT_PATH=%assetsRootPath%
set RESOURCE_FOLDER=%resourceFolder%

REM 将路径中的反斜杠替换为正斜杠
set "UPDATE_URL=%UPDATE_URL:\=/%"
set "ASSETSROOT_PATH=%ASSETSROOT_PATH:\=/%"
set "RESOURCE_FOLDER=%RESOURCE_FOLDER:\=/%"

node jenkins-shared-cocos\src\org\cocos\js\gen_manifest.js ^
  -v "%version%" ^
  -u "%UPDATE_URL%" ^
  -s "%ASSETSROOT_PATH%" ^
  -d "%ASSETSROOT_PATH%" ^
  -i "%RESOURCE_FOLDER%"

if errorlevel 1 (
    exit /b 1
)
echo ✅ Generated %bundleName% manifest completed

REM 删除旧文件
if exist "%saveAartifactsDir%" (
    REM 先尝试删除其中的文件 防止被占用的情况
    REM set "item=!item:"=!"
    attrib -R "%saveAartifactsDir%"*.* /S
    rmdir /s /q "%saveAartifactsDir%"
) 
mkdir "%saveAartifactsDir%"

REM 移动生成的.manifest文件到保存目录
move "%assetsRootPath%*.manifest" "%saveAartifactsDir%"
if errorlevel 1 (
    echo ❌ Error: Failed to move artifact manifest files to save directory
    exit /b 1
)
if "%apk%"=="true" (
    echo ✅ Move manifest files to %saveAartifactsDir% completed
    exit /b 0
)

REM 复制资源到热更新目录
set dataPath=%assetsRootPath%..
setlocal enabledelayedexpansion
for %%i in (%resourceFolder%) do (
    set "item=%%i"
    set "item=!item:"=!"
    if exist "%dataPath%\!item!" (
        echo Copying !item! to "%saveAartifactsDir%"
        copy /Y "%dataPath%\!item!" "%saveAartifactsDir%" >nul
    )
)
endlocal
if errorlevel 1 (
    echo ❌ Error: Failed to move artifact resource files to save directory 1
    exit /b 1
)

exit /b 0

:usage
echo.
echo Usage:
echo   gen_hotupdate.bat ^<bundleName^> ^<version^> ^<hotupdateUrl^> ^<apk^> ^<saveAartifactsDir^>
echo.
echo Example:
echo   build.bat hall 0.0.1 dev https://test.cdn.xxx.com/xiaomi true 0 D:\project\game\hotupdate\
exit /b 1