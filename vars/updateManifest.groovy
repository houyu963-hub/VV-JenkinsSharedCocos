import org.cocos.ci.*

def call(Map cfg) {

    def platform = cfg.platform
    def channel  = cfg.channel
    def envName  = cfg.env

    def artifactsRoot = "${ctx.env.WORKSPACE}\\..\\..\\artifacts"
    def manifestFile = "${artifactsRoot}\\JenkinsManifest.json"

    def commit   = GitUtils.shortCommit()
    def time     = new Date().format("yyyy-MM-dd HH:mm:ss")
    def author   = env.BUILD_USER ?: "jenkins"
    def duration = currentBuild.durationString.replace(" and counting", "")

    def manifest = FileUtils.readJson(manifestFile)

    def artifact = [:]

    if (platform == "android") {

        def apk = ApkUtils.findLatestApk()

        artifact = [
            versionCode : env.android_version_code as int,,
            versionName : env.android_version_name,
            name        : apk.name,
            apk         : apk.path,
            apkSize     : apk.size,
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
