import org.cocos.ci.*

// 更新JenkinsManifest.json
def call(ctx) {

    def platform = ctx.env.PLATFORM
    def channel  = ctx.params.channel
    def env  = ctx.params.env

    def artifactsRoot = "${ctx.env.WORKSPACE}\\..\\..\\artifacts"
    def manifestFile = "${artifactsRoot}\\JenkinsManifest.json"

    // 确保 manifest 文件存在
    if (!ctx.fileExists(manifestFile)) {
        ctx.writeFile file: manifestFile, text: '{}'
    }
    def commit   = GitUtils.shortCommit(this)
    def time     = new Date().format("yyyy-MM-dd HH:mm:ss")
    def author   = ctx.env.BUILD_USER ?: "jenkins"
    def duration = currentBuild.durationString.replace(" and counting", "")

    def manifest = FileUtils.readJson(manifestFile)

    def artifact = [:]

    if (platform == "android") {
        def versionCode = ctx.env.ANDROID_VERSION_CODE as int
        def versionName = ctx.env.ANDROID_VERSION_NAME
        def name = ctx.env.APK_NAME
        def path = ctx.env.APK_PATH
        def size = ctx.env.APK_SIZE
        def hot = ctx.env.HOT_VERSION

        artifact = [
            author      : author,
            versionCode : versionCode,
            versionName : versionName,
            versioHot   : hot,
            name        : name,
            path        : path,
            size        : size,
            time        : time,
            commit      : commit,
            duration    : duration
        ]
    }

    if (platform == "web") {
        def path = ctx.env.INDEX_URL
        
        artifact = [
            author   : author,
            time     : time,
            path      : path,
            commit   : commit,
            duration : duration
        ]
    }

    manifest
        .get(platform, [:])
        .get(channel, [:])
        .get(env, [])

    manifest[platform] = manifest[platform] ?: [:]
    manifest[platform][channel] = manifest[platform][channel] ?: [:]
    manifest[platform][channel][env] =
        (manifest[platform][channel][env] ?: [])

    manifest[platform][channel][env].add(0, artifact)
    manifest[platform][channel][env] =
        manifest[platform][channel][env].take(10)
    FileUtils.writeJson(manifestFile, manifest)

    echo "✅ JenkinsManifest.json 已更新"
}
