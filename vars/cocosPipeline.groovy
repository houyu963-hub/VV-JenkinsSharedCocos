def call(ctx) {
    return {
        agent { label 'cocos-windows-agent' }

        // 设置环境变量确保正确编码
        environment {
            // 编码相关环境变量
            LANG = 'zh_CN.UTF-8'
            LC_ALL = 'zh_CN.UTF-8'
            JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8'
            // Windows 中文编码
            CHCP_CMD = 'chcp 65001 >nul'  // UTF-8
            CHCP_GBK = 'chcp 936 >nul'    // GBK (Windows中文默认)
            // Cocos Creator 安装路径(按你机器实际改)
            CREATOR_PATH = 'D:\\software\\CocosEditors\\Creator\\3.8.1\\CocosCreator.exe'
            BUILD_SCRIPT = 'jenkins-shared-cocos/bat/build.bat'
        }

        stages {
            stage('拉代码') {
                steps {
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: params.git_ref]],
                        userRemoteConfigs: [[url: 'https://github.com/houyu963-hub/VV-CocosGameClient.git']],
                        extensions: [
                        // 启用子模块递归拉取
                        [$class: 'SubmoduleOption',
                            disableSubmodules: false,  // 启用子模块
                            recursiveSubmodules: true, // 递归拉取子模块
                            trackingSubmodules: false, // 不跟踪子模块的上游分支
                            reference: '',             // 不使用参考仓库
                            parentCredentials: true,   // 使用父仓库的凭据
                            depth: 1,                  // 只克隆最新提交
                            shallow: true              // 浅克隆
                        ],
                        // 清理工作区：先清理,再进行代码拉取
                        [$class: 'CleanBeforeCheckout'], // 在拉取代码之前清理工作区
                        //   [$class: 'CleanCheckout']        // 拉取代码时清理工作区
                        ]
                    ])
                }
            }

            stage('构建') {
                steps {
                    bat """
                    call ${env.BUILD_SCRIPT} ^
                        ${ctx.params.platform} ^
                        ${ctx.params.channel} ^
                        ${ctx.params.env} ^
                        ${ctx.params.mode} ^
                        ${env.CREATOR_PATH} ^
                        ${ctx.params.apk} ^
                        ${ctx.params.clean}
                    """
                }
            }
        }
    }
}
