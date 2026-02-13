def call(ctx) {
    echo "🎮 cocos pipeline start"
    
    // 设置环境变量确保正确编码
    ctx.env.LANG = 'zh_CN.UTF-8'
    ctx.env.LC_ALL = 'zh_CN.UTF-8'
    ctx.env.JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'
    // Windows 中文编码
    ctx.env.CHCP_CMD = 'chcp 65001 >nul'  // UTF-8
    ctx.env.CHCP_GBK = 'chcp 936 >nul'    // GBK (Windows中文默认)
    
    // Cocos Creator 安装路径(按你机器实际改)
    ctx.env.CREATOR_PATH = 'D:/software/CocosEditors/Creator/3.8.1/CocosCreator.exe'
    ctx.env.BAT_ROOT = 'tools/bat'
    ctx.env.JS_ROOT = 'tools/js'
    ctx.env.ARTIFACTS_DIR = '../../artifacts'
    
    stage('GitSCM CocosClient') {
        checkout([
            $class: 'GitSCM',
            branches: [[name: ctx.params.git_ref]],
            userRemoteConfigs: [[url: 'https://github.com/houyu963-hub/VV-CocosGameClient.git']],
            extensions: [
                // 启用子模块递归拉取
                [$class: 'SubmoduleOption',
                    disableSubmodules: false,  // 禁用子模块
                    recursiveSubmodules: true, // 递归拉取子模块
                    trackingSubmodules: false, // 不跟踪子模块的上游分支
                    reference: '',             // 不使用参考仓库
                    parentCredentials: true,   // 使用父仓库的凭据
                    depth: 1,                  // 只克隆最新提交
                    shallow: true              // 浅克隆
                ],
                // 清理工作区
                [$class: 'CleanBeforeCheckout']// 在拉取代码之前清理工作区
                // [$class: 'CleanCheckout']   // 拉取代码时清理工作区
            ]
        ])
    }

    stage('1st Build') {
        bat """
        call ${ctx.env.BAT_ROOT}/cocos_build.bat ^
             ${ctx.env.PLATFORM} ^
             ${ctx.params.channel} ^
             ${ctx.params.env} ^
             ${ctx.params.mode} ^
             "${ctx.env.CREATOR_PATH}"
        """
    }
    
    stage('Hot Parameters') {
        script {
            def getResult = bat(
                script: """
                call ${ctx.env.BAT_ROOT}/gen_manifest_params.bat ^
                    ${ctx.env.PLATFORM} ^
                    ${ctx.params.channel} ^
                    ${ctx.params.env} ^
                    ${ctx.params.bundle} ^
                    ${ctx.params.apk.toString().toLowerCase()} ^
                    ${ctx.env.ARTIFACTS_DIR}
                """,
                returnStdout: true
            ).trim()

            // 仅匹配大写字母数字下划线开头的 KEY=VALUE 行
            getResult.eachLine { line ->
                if (line ==~ /^[A-Z0-9_]+=.*$/) {
                    def (key, value) = line.split('=', 2)
                    env[key.trim()] = value.trim()
                    echo "Set Jenkins env: ${key.trim()} = ${value.trim()}"
                }
            }

            echo "LAST_VERSION: ${env.LAST_VERSION}"
            echo "HOTUPDATE_URL: ${env.HOTUPDATE_URL}"
            echo "SAVE_MANIFEST_DIR: ${env.SAVE_MANIFEST_DIR}"
        }
    }
    
    stage('Hot Manifest') {
        bat """
        call ${ctx.env.BAT_ROOT}/gen_manifest.bat ^
             ${ctx.params.bundle} ^
             ${ctx.env.LAST_VERSION} ^
             ${ctx.env.HOTUPDATE_URL} ^
             ${ctx.env.SAVE_MANIFEST_DIR}
        """
    }
    
    stage('Hot Resources') {
        script {
            if (ctx.params.apk == false) {
                bat """
                call ${ctx.env.BAT_ROOT}/copy_hotupdate_resources.bat ^
                    ${ctx.params.bundle} ^
                    "${ctx.env.SAVE_MANIFEST_DIR}"
                """
            }
        }
    }
    
    stage('2nd Build') {
        when {
            expression { return ctx.params.apk == true }
        }
        steps {
            bat """
            call ${ctx.env.BAT_ROOT}/cocos_build.bat ^
                 ${ctx.env.PLATFORM} ^
                 ${ctx.params.channel} ^
                 ${ctx.params.env} ^
                 ${ctx.params.mode} ^
                 "${ctx.env.CREATOR_PATH}"
            """
        }
    }
}