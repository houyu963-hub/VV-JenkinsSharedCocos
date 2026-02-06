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
    def apkName = "game_${params.channel}_${params.env}_${params.mode}_v${new_versionName}.apk"
    ctx.env.APK_NAME = apkName

    def timeDir = new Date().format("yyyyMMdd_HHmmss")

    // apk输出目录
    def outputDir = "${ctx.env.WORKSPACE}\\..\\..\\artifacts\\${ctx.env.PLATFORM}\\${params.channel}\\${params.env}\\${timeDir}"
    ctx.env.APK_PATH = "${outputDir}\\${apkName}"

    def variantName = "${params.channel.capitalize()}${params.mode.capitalize()}"
    dir('build/android/proj') {
        bat """
        gradlew assemble${variantName} copy${variantName}Apk \
        -PversionName=${new_versionName} \
        -PversionCode=${new_versionCode} \
        -PoutputDir=${outputDir} \
        -PapkName=${apkName}
        """
    }

    ctx.env.APK_SIZE = FileUtils.getFileSize("${outputDir}\\${apkName}")
}
