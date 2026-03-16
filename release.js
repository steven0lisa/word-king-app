#!/usr/bin/env node

/**
 * WordKing 自动发布脚本
 * 功能：版本号+1、更新文件、提交代码、创建并推送 tag
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');
const readline = require('readline');

// ANSI 颜色代码
const colors = {
    reset: '\x1b[0m',
    red: '\x1b[31m',
    green: '\x1b[32m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    cyan: '\x1b[36m',
    bold: '\x1b[1m'
};

// 文件路径
const PACKAGE_JSON_PATH = path.join(__dirname, 'package.json');
const BUILD_GRADLE_PATH = path.join(__dirname, 'app', 'build.gradle');

/**
 * 打印带颜色的消息
 */
function log(message, color = 'reset') {
    console.log(`${colors[color]}${message}${colors.reset}`);
}

/**
 * 打印标题
 */
function printTitle(title) {
    console.log('');
    log('═'.repeat(50), 'cyan');
    log(`  ${title}`, 'bold');
    log('═'.repeat(50), 'cyan');
    console.log('');
}

/**
 * 从用户获取输入
 */
function question(prompt) {
    const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });

    return new Promise(resolve => {
        rl.question(prompt, answer => {
            rl.close();
            resolve(answer);
        });
    });
}

/**
 * 读取 package.json
 */
function readPackageJson() {
    const content = fs.readFileSync(PACKAGE_JSON_PATH, 'utf8');
    return JSON.parse(content);
}

/**
 * 写入 package.json
 */
function writePackageJson(data) {
    fs.writeFileSync(PACKAGE_JSON_PATH, JSON.stringify(data, null, 2) + '\n');
}

/**
 * 读取 build.gradle
 */
function readBuildGradle() {
    return fs.readFileSync(BUILD_GRADLE_PATH, 'utf8');
}

/**
 * 写入 build.gradle
 */
function writeBuildGradle(content) {
    fs.writeFileSync(BUILD_GRADLE_PATH, content);
}

/**
 * 解析版本号
 */
function parseVersion(version) {
    // 移除可能的 'v' 前缀
    version = version.replace(/^v/, '');
    const parts = version.split('.').map(Number);
    return {
        major: parts[0] || 0,
        minor: parts[1] || 0,
        patch: parts[2] || 0
    };
}

/**
 * 格式化版本号
 */
function formatVersion(major, minor, patch) {
    return `v${major}.${minor}.${patch}`;
}

/**
 * 增加版本号
 */
function bumpVersion(version, type) {
    const parsed = parseVersion(version);
    let { major, minor, patch } = parsed;

    switch (type) {
        case 'major':
            major++;
            minor = 0;
            patch = 0;
            break;
        case 'minor':
            minor++;
            patch = 0;
            break;
        case 'patch':
        default:
            patch++;
            break;
    }

    return formatVersion(major, minor, patch);
}

/**
 * 检查是否有未提交的更改
 */
function checkGitStatus() {
    try {
        const result = execSync('git status --porcelain', { encoding: 'utf8' });
        return result.trim().length > 0;
    } catch (error) {
        return false;
    }
}

/**
 * 获取当前 git 分支
 */
function getCurrentBranch() {
    try {
        return execSync('git rev-parse --abbrev-ref HEAD', { encoding: 'utf8' }).trim();
    } catch (error) {
        return 'unknown';
    }
}

/**
 * 执行 git 命令
 */
function gitExec(command, silent = false) {
    try {
        if (silent) {
            execSync(command, { stdio: 'ignore' });
        } else {
            execSync(command, { stdio: 'inherit' });
        }
        return true;
    } catch (error) {
        return false;
    }
}

/**
 * 主函数
 */
async function main() {
    printTitle('WordKing 自动发布工具');

    // 1. 读取当前版本
    const pkg = readPackageJson();
    const currentVersion = pkg.version;
    log(`当前版本: ${currentVersion}`, 'blue');

    // 2. 选择版本更新类型
    console.log('');
    log('请选择版本更新类型:', 'yellow');
    log('  1. patch (补丁版) - 例如: v1.0.0 -> v1.0.1', 'reset');
    log('  2. minor (次版本) - 例如: v1.0.0 -> v1.1.0', 'reset');
    log('  3. major (主版本) - 例如: v1.0.0 -> v2.0.0', 'reset');
    log('  4. 自定义版本', 'reset');

    const choice = await question('\n请输入选项 (1-4, 默认: 1): ');
    let type = 'patch';
    let customVersion = null;

    switch (choice.trim()) {
        case '2':
            type = 'minor';
            break;
        case '3':
            type = 'major';
            break;
        case '4':
            customVersion = await question('请输入新版本号 (例如: v1.2.3): ');
            if (!customVersion.match(/^v?\d+\.\d+\.\d+$/)) {
                log('错误: 版本号格式不正确！', 'red');
                process.exit(1);
            }
            break;
        default:
            type = 'patch';
            break;
    }

    // 3. 计算新版本号
    const newVersion = customVersion || bumpVersion(currentVersion, type);
    log(`\n新版本号: ${newVersion}`, 'green');

    // 4. 确认
    const confirm = await question('\n确认发布此版本? (y/N): ');
    if (confirm.toLowerCase() !== 'y') {
        log('已取消发布', 'yellow');
        process.exit(0);
    }

    // 5. 检查 git 状态
    if (checkGitStatus()) {
        log('\n警告: 存在未提交的更改！', 'yellow');
        const proceed = await question('是否继续发布? (y/N): ');
        if (proceed.toLowerCase() !== 'y') {
            log('已取消发布', 'yellow');
            process.exit(0);
        }
    }

    const currentBranch = getCurrentBranch();
    log(`\n当前分支: ${currentBranch}`, 'blue');

    // 6. 更新 package.json
    pkg.version = newVersion.replace(/^v/, '');
    writePackageJson(pkg);
    log('✓ 已更新 package.json', 'green');

    // 7. 更新 build.gradle
    let buildGradle = readBuildGradle();
    const oldVersionLine = buildGradle.match(/versionName\s+["'](.+?)["']/);
    if (oldVersionLine) {
        const oldGradleVersion = oldVersionLine[1];
        buildGradle = buildGradle.replace(
            /versionName\s+["'].+?["']/,
            `versionName "${newVersion}"`
        );
        // 同时更新 versionCode (简单 +1)
        buildGradle = buildGradle.replace(
            /versionCode\s+(\d+)/,
            (match, code) => `versionCode ${parseInt(code) + 1}`
        );
        writeBuildGradle(buildGradle);
        log('✓ 已更新 build.gradle', 'green');
    }

    // 8. 提交更改
    printTitle('提交更改');

    const commitMessage = `chore: release ${newVersion}`;
    const password = await question('请输入 Git 凭据密码 (如果需要): ');

    console.log('');
    log('正在提交更改...', 'blue');

    try {
        // 添加更改
        execSync('git add package.json app/build.gradle', { stdio: 'ignore' });

        // 提交 (如果有密码，配置 git credential helper)
        if (password) {
            // 注意：这里需要根据实际的 git 配置来处理密码
            // 简单情况下，用户可能已经配置了 SSH 密钥或凭据缓存
        }

        execSync(`git commit -m "${commitMessage}"`, { stdio: 'inherit' });
        log('✓ 已提交更改', 'green');
    } catch (error) {
        log('✗ 提交失败，请检查 git 配置', 'red');
        log('提示: 如果使用 HTTPS，可能需要配置凭据助手', 'yellow');
        process.exit(1);
    }

    // 9. 创建 tag
    printTitle('创建标签');
    log(`正在创建标签 ${newVersion}...`, 'blue');

    try {
        // 删除已存在的同名标签（如果有）
        execSync(`git tag -d ${newVersion} 2>/dev/null || true`, { stdio: 'ignore' });
        execSync(`git tag ${newVersion}`, { stdio: 'inherit' });
        log('✓ 已创建本地标签', 'green');
    } catch (error) {
        log('✗ 创建标签失败', 'red');
        process.exit(1);
    }

    // 10. 推送到远程
    printTitle('推送到远程');
    log('正在推送代码和标签...', 'blue');
    log('这可能需要输入凭据...', 'yellow');

    try {
        execSync(`git push origin ${currentBranch}`, { stdio: 'inherit' });
        log('✓ 已推送代码', 'green');

        execSync(`git push origin ${newVersion}`, { stdio: 'inherit' });
        log('✓ 已推送标签', 'green');
    } catch (error) {
        log('✗ 推送失败', 'red');
        log('提示: 如果使用 HTTPS，可以手动推送:', 'yellow');
        log(`  git push origin ${currentBranch}`, 'reset');
        log(`  git push origin ${newVersion}`, 'reset');
        process.exit(1);
    }

    // 11. 完成
    printTitle('发布完成！');
    log(`版本 ${newVersion} 已成功发布`, 'green');
    log('', 'reset');
    log('GitHub Actions 将自动构建并发布 APK', 'blue');
    log(`查看构建状态: https://github.com/steven0lisa/word-king-app/actions`, 'reset');
    log('', 'reset');
}

// 运行主函数
main().catch(error => {
    log(`\n错误: ${error.message}`, 'red');
    process.exit(1);
});
