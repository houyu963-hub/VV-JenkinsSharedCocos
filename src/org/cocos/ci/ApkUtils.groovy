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

    // 获取最新 apk 文件信息
    static def findLatestApk(script, ctx) {
        def apkDir = "${ctx.env.WORKSPACE}\\..\\..\\artifacts\\${ctx.env.PLATFORM}\\${ctx.params.channel}\\${ctx.params.env}"
        
        // 使用简单的 bat 命令获取信息
        @NonCPS
        def name = script.bat(
            script: "dir \"${apkDir}\" /s /b *.apk | sort /r | head -n 1",
            returnStdout: true
        ).trim()
        
        if (name.empty) {
            return [name: "", path: "", size: "0MB"]
        }
        
        // 获取文件大小
        def sizeBytes = script.bat(
            script: "for %i in (\"${name}\") do @echo %~zi",
            returnStdout: true
        ).trim().toLong()
        
        def sizeMB = String.format("%.2f", sizeBytes / 1024.0 / 1024.0)
        
        return [
            name: new File(name).name,
            path: name,
            size: sizeMB + "MB"
        ]
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
}