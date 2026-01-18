const fs = require('fs');
const path = require('path');

const mainJsPath = path.join(process.cwd(), 'build', 'android', 'data', 'main.js');

if (!fs.existsSync(mainJsPath)) {
    console.error(`未发现 main.js 文件: ${mainJsPath}`);
    process.exit(1);
}

// 要插入的代码
const injectedCode = `(function () {
    if (typeof window.jsb === 'object') {
        var hotUpdateSearchPaths = localStorage.getItem('HotUpdateSearchPaths');
        if (hotUpdateSearchPaths) {
            var paths = JSON.parse(hotUpdateSearchPaths);
            jsb.fileUtils.setSearchPaths(paths);

            var fileList = [];
            var storagePath = paths[0] || '';
            var tempPath = storagePath + '_temp/';
            var baseOffset = tempPath.length;

            if (jsb.fileUtils.isDirectoryExist(tempPath) && !jsb.fileUtils.isFileExist(tempPath + 'project.manifest.temp')) {
                jsb.fileUtils.listFilesRecursively(tempPath, fileList);
                fileList.forEach(srcPath => {
                    var relativePath = srcPath.substr(baseOffset);
                    var dstPath = storagePath + relativePath;

                    if (srcPath[srcPath.length] == '/') {
                        jsb.fileUtils.createDirectory(dstPath)
                    }
                    else {
                        if (jsb.fileUtils.isFileExist(dstPath)) {
                            jsb.fileUtils.removeFile(dstPath)
                        }
                        jsb.fileUtils.renameFile(srcPath, dstPath);
                    }
                })
                jsb.fileUtils.removeDirectory(tempPath);
            }
        }
    }
})();`;

// 读取原文件内容
const originalContent = fs.readFileSync(mainJsPath, 'utf8');

// 写入新内容（注入代码 + 原始内容）
const newContent = injectedCode + originalContent;
fs.writeFileSync(mainJsPath, newContent, 'utf8');