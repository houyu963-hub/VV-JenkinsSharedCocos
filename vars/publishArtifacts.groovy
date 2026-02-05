import org.cocos.ci.*

// copy 产物到 artifacts 目录
def call(ctx) {
    if (ctx.env.PLATFORM == 'web') {
        bat "xcopy /E /I /Y build\\web-mobile \"${target}\""
    }

    // 清理旧目录 只保留 最近10 个
    // FileUtils.cleanupOldDirs(this, root, 10)
}