package org.cocos

// Git 工具类
class GitUtils implements Serializable {

    // 返回 Git 短提交 ID
    static String shortCommit(script) {
        return script.bat(
            script: 'git rev-parse --short HEAD',
            returnStdout: true
        ).trim().split('\r\n')[-1]
    }
}
