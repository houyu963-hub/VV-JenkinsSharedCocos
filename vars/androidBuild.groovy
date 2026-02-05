import org.cocos.ci.*

// 生成 Android apk
def call(ctx) {
    def params = ctx.params
    def (versionName, versionCode) =
        ApkUtils.resolveVersion(this, params, ctx.env.WORKSPACE)

    def new_versionName = params.versionName ?: versionName
    def new_versionCode = params.versionCode ?: versionCode

    ctx.env.ANDROID_VERSION_NAME = new_versionName
    ctx.env.ANDROID_VERSION_CODE = new_versionCode

    // apk名称
    def apkName = "Game_${params.channel}_${params.env}_${params.mode}_v${new_versionName}.apk"
    ctx.env.APK_NAME = apkName

    // apk输出目录
    def outputDir = "${ctx.env.WORKSPACE}\\..\\..\\artifacts\\${ctx.env.PLATFORM}\\${params.channel}\\${params.env}"
    ctx.env.APK_PATH = outputDir

    dir('build/android/proj') {
        bat """
        gradlew assemble${params.channel.capitalize()}${params.mode.capitalize()} \
        -PversionName=${new_versionName} \
        -PversionCode=${new_versionCode} \
        -PoutputDir=${outputDir} \
        -PapkName=${apkName}
        """
    }

    ctx.env.APK_SIZE = ApkUtils.apkSize(this, "${outputDir}\\${apkName}")
}
