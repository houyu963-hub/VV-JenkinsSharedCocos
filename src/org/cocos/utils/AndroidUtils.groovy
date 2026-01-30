package org.cocos.utils

// Android 工具类
class AndroidUtils implements Serializable {

    // 获取最新 Android 版本信息 androidName/androidCode
    static List resolveVersion(script, params, workspace) {
        def manifestFile = "${workspace}/publish/JenkinsManifest.json"

        def lastName = "1.0.0"
        def lastCode = 10000

        if (script.fileExists(manifestFile)) {
            def m = script.readJSON(file: manifestFile)
            def list = m?.android?."${params.channel}"?."${params.env}"
            if (list) {
                lastName = list[0].versionName
                lastCode = list[0].versionCode as int
            }
        }

        def name = params.version_name?.trim()
            ? params.version_name
            : lastName

        def code = params.version_code?.trim()
            ? params.version_code.toInteger()
            : lastCode + 1

        return [name, code]
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
