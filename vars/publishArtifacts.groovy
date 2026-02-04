import org.cocos.ci.*

// copy 产物到 artifacts 目录
@NonCPS
def call(ctx) {
    if (ctx.env.PLATFORM == 'android' && !ctx.params.apk) { // is hotupdate
        return
    }
    def timeDir = new Date().format("yyyyMMdd_HHmmss")
    def root = "${ctx.env.WORKSPACE}\\..\\..\\artifacts\\${ctx.env.PLATFORM}\\${ctx.params.channel}\\${ctx.params.env}"
    def target = "${root}\\${timeDir}"

    bat "mkdir \"${target}\" 2>nul"

    if (ctx.env.PLATFORM == 'android') {
        def apk = "Game_${ctx.params.channel}_${ctx.params.mode}_v${ctx.env.android_version_name}.apk"
        def apk_full_name = "Game_${ctx.params.channel}_${ctx.params.env}_${ctx.params.mode}_v${ctx.env.android_version_name}.apk"
        bat """
            powershell -Command "
            \$apkFile = Get-ChildItem -Path '${ctx.env.WORKSPACE}\\build\\android\\proj\\build' -Filter '${apk}' -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
            if (\$apkFile) {
                Copy-Item \$apkFile.FullName -Destination '${target}\\${apk_full_name}' -Force
                Write-Host 'Successfully copied APK: ' \$apkFile.Name
            } else {
                Write-Warning 'APK file not found: ${apk}'
                exit 1
            }
            "
        """
}

    if (ctx.env.PLATFORM == 'web') {
        bat "xcopy /E /I /Y build\\web-mobile \"${target}\""
    }

    // 清理旧目录 只保留 最近10 个
    FileUtils.cleanupOldDirs(this, root, 10)
}