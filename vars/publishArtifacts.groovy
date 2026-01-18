// copy 产物到发布目录
def call(ctx) {
    def timeDir = new Date().format("yyyyMMdd_HHmmss")
    def root = "${ctx.env.WORKSPACE}\\..\\..\\artifacts\\${ctx.params.platform}\\${ctx.params.channel}\\${ctx.params.env}"
    def target = "${root}/${timeDir}"

    bat "mkdir \"${target}\" 2>nul"

    if (ctx.params.platform == 'android') {
        def apk = "Game_${ctx.params.channel}_${ctx.params.env}_v${ctx.env.android_version_code}.apk"
        bat "copy ${ctx.env.WORKSPACE}\\build\\android\\**\\*.apk \"${target}\\${apk}\""
    }

    if (ctx.params.platform == 'web') {
        bat "xcopy /E /I /Y build\\web-mobile \"${target}\""
    }

    // 清理旧目录 只保留 最近10 个
    org.cocos.FileUtils.cleanupOldDirs(this, root, 10)
}
