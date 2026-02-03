// 生成 Android apk
def call(ctx) {
    def (versionName, versionCode) =
        org.cocos.utils.AndroidUtils.resolveVersion(this, ctx.params, ctx.env.WORKSPACE)

    versionName =  ctx.params.versionName ?: versionName
    versionCode = ctx.params.versionCode ?: versionCode

    dir('build/android/proj') {
        bat """
        gradlew assemble${ctx.params.channel.capitalize()}${ctx.params.mode.capitalize()} \
        -PversionName=${versionName} \
        -PversionCode=${versionCode}
        """
    }
}
