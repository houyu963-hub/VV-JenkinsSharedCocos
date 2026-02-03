package org.cocos.utils

// Android 工具类
class AndroidUtils implements Serializable {

    // 获取最新 Android 版本信息 androidName/androidCode
    static List resolveVersion(script, params, workspace) {
        def manifestFile = "${workspace}/publish/JenkinsManifest.json"

        def lastName = "1.0.0.0"
        def lastCode = 1000

        if (script.fileExists(manifestFile)) {
            def m = script.readJSON(file: manifestFile)
            def list = m?.android?."${params.channel}"?."${params.env}"
            if (list) {
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
        def bytes = script.bat(
            script: "powershell -NoProfile -Command \"(Get-Item '${apkPath}').Length\"",
            returnStdout: true
        ).trim().toLong()

        return [
            bytes: bytes,
            mb: String.format("%.2f", bytes / 1024.0 / 1024.0)
        ]
    }
}