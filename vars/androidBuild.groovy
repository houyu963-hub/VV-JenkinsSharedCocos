// 生成 Android apk
def call(ctx) {
    def (versionName, versionCode) =
        org.cocos.utils.AndroidUtils.resolveVersion(this, ctx.params, ctx.env.WORKSPACE)

    def android_version_name = ctx.params.versionName ?: versionName
    def android_version_code = ctx.params.versionCode ?: versionCode

    ctx.env.android_version_name = android_version_name
    ctx.env.android_version_code = android_version_code

    dir('build/android') {
        bat """
        gradlew assemble${ctx.params.channel.capitalize()}${ctx.params.mode.capitalize()} \
        -PversionName=${versionName} \
        -PversionCode=${versionCode}
        """
    }
}
