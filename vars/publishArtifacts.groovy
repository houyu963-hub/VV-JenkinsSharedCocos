// copy 产物到 artifacts 目录
def call(ctx) {
    def timeDir = new Date().format("yyyyMMdd_HHmmss")
    def root = "${ctx.env.WORKSPACE}\\..\\..\\artifacts\\${ctx.env.PLATFORM}\\${ctx.params.channel}\\${ctx.params.env}"
    def target = "${root}\\${timeDir}"

    bat "mkdir \"${target}\" 2>nul"

    if (ctx.env.PLATFORM == 'android') {
        def apk = "Game_${ctx.params.channel}_${ctx.params.env}_v${ctx.env.android_version_code}.apk"
        // 使用 xcopy 替代 copy，并指定 /S 参数递归复制
        bat "xcopy /Y /S \"${ctx.env.WORKSPACE}\\build\\android\\*.apk\" \"${target}\\${apk}\""
    }

    if (ctx.env.PLATFORM == 'web') {
        bat "xcopy /E /I /Y build\\web-mobile \"${target}\""
    }

    // 清理旧目录 只保留 最近10 个
    org.cocos.utils.FileUtils.cleanupOldDirs(this, root, 10)
}