// 生成 Android apk
def call(ctx) {
    def (versionName, versionCode) =
        org.cocos.utils.AndroidUtils.resolveVersion(this, ctx.params, ctx.env.WORKSPACE)

    def new_versionName = ctx.params.versionName ?: versionName
    def new_versionCode = ctx.params.versionCode ?: versionCode

    ctx.env.android_version_name =  new_versionName
    ctx.env.android_version_code = new_versionCode

    dir('build/android/proj') {
        bat """
        gradlew assemble${ctx.params.channel.capitalize()}${ctx.params.mode.capitalize()} \
        -PversionName=${new_versionName} \
        -PversionCode=${new_versionCode}
        """
    }
}
