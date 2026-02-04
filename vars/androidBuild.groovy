import org.cocos.ci.*

// 生成 Android apk
def call(ctx) {
    def (versionName, versionCode) =
        ApkUtils.resolveVersion(this, ctx.params, ctx.env.WORKSPACE)

    def new_versionName = ctx.params.versionName ?: versionName
    def new_versionCode = ctx.params.versionCode ?: versionCode

    ctx.env.ANDROID_VERSION_NAME = new_versionName
    ctx.env.ANDROID_VERSION_CODE = new_versionCode

    dir('build/android/proj') {
        bat """
        gradlew assemble${ctx.params.channel.capitalize()}${ctx.params.mode.capitalize()} \
        -PversionName=${new_versionName} \
        -PversionCode=${new_versionCode}
        """
    }
}
