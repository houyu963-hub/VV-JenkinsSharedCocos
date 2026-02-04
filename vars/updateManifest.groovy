import org.cocos.ci.*

def call(Map ctx) {

    def platform = ctx.platform
    def channel  = ctx.channel
    def envName  = ctx.env

    def artifactsRoot = "${ctx.env.WORKSPACE}\\..\\..\\artifacts"
    def manifestFile = "${artifactsRoot}\\JenkinsManifest.json"

    // 创建目录（如果不存在）
    bat "mkdir \"${artifactsRoot}\" 2>nul"

    // 确保 manifest 文件存在
    if (!ctx.fileExists(manifestFile)) {
        ctx.writeFile file: manifestFile, text: '{}'
    }

    def commit   = GitUtils.shortCommit(this)
    def time     = new Date().format("yyyy-MM-dd HH:mm:ss")
    def author   = env.BUILD_USER ?: "jenkins"
    def duration = currentBuild.durationString.replace(" and counting", "")

    def manifest = FileUtils.readJson(manifestFile)

    def artifact = [:]

    if (platform == "android") {

        def (name, path, size) = ApkUtils.findLatestApk(this, ctx.params, ctx.env.WORKSPACE)

        artifact = [
            versionCode : ctx.env.ANDROID_VERSION_CODE as int,
            versionName : ctx.env.ANDROID_VERSION_NAME,
            name        : name,
            apk         : path,
            apkSize     : size,
            time        : time,
            author      : author,
            commit      : commit,
            duration    : duration
        ]
    }

    if (platform == "web") {

        artifact = [
            url      : "artifacts/latest/build/web-mobile/index.html",
            time     : time,
            author   : author,
            commit   : commit,
            duration : duration
        ]
    }

    manifest
        .get(platform, [:])
        .get(channel, [:])
        .get(envName, [])

    manifest[platform] = manifest[platform] ?: [:]
    manifest[platform][channel] = manifest[platform][channel] ?: [:]
    manifest[platform][channel][envName] =
        (manifest[platform][channel][envName] ?: [])

    manifest[platform][channel][envName].add(0, artifact)
    manifest[platform][channel][envName] =
        manifest[platform][channel][envName].take(10)

    FileUtils.writeJson(manifestFile, manifest)

    echo "✅ JenkinsManifest.json 已更新"
}
