// 更新JenkinsManifest.json
def call(ctx) {
    def artifactsRoot = "${ctx.env.WORKSPACE}/../../artifacts"
    def manifestFile = "${artifactsRoot}/JenkinsManifest.json"

    if (!fileExists(manifestFile)) {
        writeFile file: manifestFile, text: "{}"
    }

    def manifest = readJSON(file: manifestFile)

    def commit = org.cocos.GitUtils.shortCommit(this)
    def time = new Date().format("yyyy-MM-dd HH:mm:ss")

    def artifact = [
        time: time,
        author: ctx.env.BUILD_USER ?: "jenkins",
        commit: commit,
        duration: currentBuild.durationString
    ]

    manifest[ctx.params.platform] =
        manifest.get(ctx.params.platform, [:])

    manifest[ctx.params.platform][ctx.params.channel] =
        manifest[ctx.params.platform][ctx.params.channel] ?: [:]

    manifest[ctx.params.platform][ctx.params.channel][ctx.params.env] =
        (manifest[ctx.params.platform][ctx.params.channel][ctx.params.env] ?: [])

    manifest[ctx.params.platform][ctx.params.channel][ctx.params.env].add(0, artifact)
    manifest[ctx.params.platform][ctx.params.channel][ctx.params.env] =
        manifest[ctx.params.platform][ctx.params.channel][ctx.params.env].take(10)

    writeJSON file: manifestFile, json: manifest, pretty: 2
}
