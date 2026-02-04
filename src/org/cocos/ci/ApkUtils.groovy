package org.cocos.ci

// apk 工具类
class ApkUtils implements Serializable {

    // 上个apk 版本信息
    static List resolveVersion(script, params, workspace) {
        def manifestFile = "${workspace}\\..\\..\\artifacts\\JenkinsManifest.json"

        def lastName = "1.0.0.0"
        def lastCode = 1000

        if (script.fileExists(manifestFile)) {
            def m = script.readJSON(file: manifestFile)
            def list = m?.android?."${params.channel}"?."${params.env}"
            if (list && list.size() > 0) {
                lastName = list[0].versionName
                lastCode = list[0].versionCode as int
            }
        }

        def code = params.version_code?.trim()
            ? params.version_code.toInteger()
            : lastCode + 1

        // 根据 versionCode 计算 versionName
        def name = params.version_name?.trim()
            ? params.version_name
            : calculateVersionName(code)

        return [name, code]
    }

    // 上个apk 物理信息
    @NonCPS
    static def findLatestApk(script, ctx) {
        def apkDir = "${ctx.env.WORKSPACE}\\..\\..\\artifacts\\${ctx.env.PLATFORM}\\${ctx.params.channel}\\${ctx.params.envName}"
        
        // 使用 bat 命令查找最新 APK
        def result = script.bat(
            script: """
                powershell -Command "
                \$latest = Get-ChildItem -Path '${apkDir}' -Filter '*.apk' -Recurse -ErrorAction SilentlyContinue | 
                        Sort-Object LastWriteTime -Descending | 
                        Select-Object -First 1;
                if (\$latest) {
                    Write-Output ('NAME:' + \$latest.Name);
                    Write-Output ('PATH:' + \$latest.FullName);
                    Write-Output ('SIZE:' + [Math]::Round(\$latest.Length / 1MB, 2) + 'MB');
                } else {
                    Write-Output 'NOT_FOUND'
                }
                "
            """,
            returnStdout: true
        ).trim()
        
        if (result == 'NOT_FOUND') {
            return [name: "", path: "", size: "0MB"]
        }
        
        // 解析输出
        def lines = result.readLines()
        def name = lines.find { it.startsWith('NAME:') }?.substring(5) ?: ""
        def path = lines.find { it.startsWith('PATH:') }?.substring(5) ?: ""
        def size = lines.find { it.startsWith('SIZE:') }?.substring(5) ?: "0MB"
        
        return [name: name, path: path, size: size]
    }

    // 根据 versionCode 计算四位版本号
    private static String calculateVersionName(int versionCode) {
        // 基础版本：1000 -> 1.0.0.0
        // 1001 -> 1.0.0.1
        // 1010 -> 1.0.1.0
        // 1100 -> 1.1.0.0
        // 2000 -> 2.0.0.0
        
        def base = versionCode - 1000  // 从1000开始计算偏移量
        
        def first = base / 1000
        def second = (base % 1000) / 100
        def third = (base % 100) / 10
        def fourth = base % 10
        
        return "${first.toInteger()}.${second.toInteger()}.${third.toInteger()}.${fourth.toInteger()}"
    }

    // 返回 apk 文件大小
    static Map apkSize(script, String apkPath) {
        if (!apkPath || !script.fileExists(apkPath)) {
            return [bytes: 0, mb: "0.00"]
        }
        
        try {
            def bytes = script.bat(
                script: "powershell -NoProfile -Command \"(Get-Item '${apkPath}').Length\"",
                returnStdout: true
            ).trim().toLong()

            return [
                bytes: bytes,
                mb: String.format("%.2f", bytes / 1024.0 / 1024.0)
            ]
        } catch (Exception e) {
            script.println("获取 APK 大小失败: ${e.message}")
            return [bytes: 0, mb: "0.00"]
        }
    }
}