package org.cocos.ci

// 文件 工具类
class FileUtils implements Serializable {

    // 清理旧目录 保留最新的 keep 个目录
    static void cleanupOldDirs(script, String baseDir, int keep) {
        if (!script.fileExists(baseDir)){
            script.echo "Directory ${baseDir} does not exist"
            return
        }
        script.bat(
            script:
                'powershell -NoProfile -Command "& {' +
                '  Get-ChildItem \'' + baseDir + '\' -Directory | ' +
                '  Sort-Object Name -Descending | ' +
                '  Select-Object -Skip ' + keep + ' | ' +
                '  ForEach-Object { Remove-Item $_.FullName -Recurse -Force }' +
                '}"'
        )
    }

    // 读取 JSON 文件
    static def readJson(path) {
        if (!new File(path).exists()) return [:]
        return new groovy.json.JsonSlurper().parse(new File(path))
    }

    // 写入 JSON 文件
    static void writeJson(path, obj) {
        def json = groovy.json.JsonOutput.prettyPrint(
            groovy.json.JsonOutput.toJson(obj)
        )
        new File(path).write(json, "UTF-8")
    }

    // 获取文件大小
    static String getFileSizeMB(path) {
        def bytes = new File(path).length()
        return String.format("%.2f MB", bytes / 1024.0 / 1024.0)
    }
}
