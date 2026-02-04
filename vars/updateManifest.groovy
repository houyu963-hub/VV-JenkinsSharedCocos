import org.cocos.ci.*

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

    echo "JenkinsManifest.json 更新中."
    def manifest = FileUtils.readJson(manifestFile)

    def artifact = [:]

    if (platform == "android") {
        echo "JenkinsManifest.json 更新中.."
        // def apkInfo = ApkUtils.findLatestApk(this, ctx)

        def apkInfo = bat """
            powershell -Command "
            \$latest = Get-ChildItem -Path '${apkDir}' -Filter '*.apk' -Recurse -ErrorAction SilentlyContinue | 
                    Sort-Object LastWriteTime -Descending | 
                    Select-Object -First 1;
            if (\$latest) {
                Write-Output ('NAME:' + \$latest.Name);
                Write-Output ('PATH:' + \$latest.FullName);
                Write-Output ('SIZE:' + [Math]::Round(\$latest.Length / 1MB, 2) + 'MB');
            } else {
                Write-Output ('NAME:' + '');
                Write-Output ('PATH:' + '');
                Write-Output ('SIZE:' + '0MB');
            }
            "
        """
        // 解析输出
        def lines = apkInfo.readLines()
        def name = lines.find { it.startsWith('NAME:') }?.substring(5) ?: ""
        def path = lines.find { it.startsWith('PATH:') }?.substring(5) ?: ""
        def size = lines.find { it.startsWith('SIZE:') }?.substring(5) ?: "0MB"
        
        echo "APK name: ${name}"
        echo "APK path: ${path}"
        echo "APK size: ${size}"

        echo "JenkinsManifest.json 更新中2.."

        artifact = [
            versionCode : ctx.env.ANDROID_VERSION_CODE as int,
            versionName : ctx.env.ANDROID_VERSION_NAME,
            name        : apkInfo.name,
            apk         : apkInfo.path,
            apkSize     : apkInfo.size,
            time        : time,
            author      : author,
            commit      : commit,
            duration    : duration
        ]
    }

    if (platform == "web") {
        // 获取最新的时间戳目录
        def webRoot = "${artifactsRoot}\\${platform}\\${channel}\\${env}"
        def latestDir = bat(
            script: """
                powershell -Command "
                Get-ChildItem -Path '${webRoot}' -Directory | 
                Sort-Object LastWriteTime -Descending | 
                Select-Object -First 1 -ExpandProperty Name
                "
            """,
            returnStdout: true
        ).trim()
        
        artifact = [
            url      : "${webRoot}\\${latestDir}\\index.html",
            time     : time,
            author   : author,
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
    echo "JenkinsManifest.json 更新中..."
    FileUtils.writeJson(manifestFile, manifest)

    echo "✅ JenkinsManifest.json 已更新"
}
