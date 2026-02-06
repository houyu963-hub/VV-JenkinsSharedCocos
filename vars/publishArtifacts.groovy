import org.cocos.ci.*

// copy 产物到 artifacts 目录
def call(ctx) {
    def timeDir = new Date().format("yyyyMMdd_HHmmss")
    def root = "${ctx.env.WORKSPACE}\\..\\..\\artifacts\\${ctx.env.PLATFORM}\\${ctx.params.channel}\\${ctx.params.env}"
    def target = "${root}\\${timeDir}"


    if (ctx.env.PLATFORM == 'web') {
        bat "xcopy /E /I /Y build\\web-mobile \"${target}\""
    }

    // 清理旧目录 只保留 最近10 个
    FileUtils.cleanupOldDirs(this, root, 10)
}