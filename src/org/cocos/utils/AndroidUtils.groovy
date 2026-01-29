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
            def list = m?.android?."${params.CHANNEL}"?."${params.ENV}"
            if (list) {
                lastName = list[0].versionName
                lastCode = list[0].versionCode as int
            }
        }

        def name = params.VERSION_NAME?.trim()
            ? params.VERSION_NAME
            : lastName

        def code = params.VERSION_CODE?.trim()
            ? params.VERSION_CODE.toInteger()
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
