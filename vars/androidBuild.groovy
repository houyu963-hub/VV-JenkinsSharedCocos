// 生成 Android apk
def call(ctx) {
    def (versionName, versionCode) =
        org.cocos.AndroidUtils.resolveVersion(this, ctx.params, ctx.env.WORKSPACE)

    def versionName = ctx.params.versionName ?: versionName
    def versionCode = ctx.params.versionCode ?: versionCode

    ctx.env.android_version_name = versionName
    ctx.env.android_version_code = versionCode

    dir('build/android') {
        bat """
        gradlew assemble${ctx.params.channel.capitalize()}${ctx.params.mode.capitalize()} \
        -PversionName=${versionName} \
        -PversionCode=${versionCode}
        """
    }
}
