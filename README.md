# WordKing - 词王

多语言单词学习 Android 应用，基于艾宾浩斯遗忘曲线实现智能复习。

## 功能特性

- **艾宾浩斯复习** - 12个记忆阶段，科学复习间隔
- **多语言支持** - 英语、印尼语、泰语、韩语、日语、西班牙语、葡萄牙语、法语
- **AI 生成题库** - 自动生成学习单词（支持 Moonshot/Kimi API）
- **Git 云同步** - 数据跨设备同步（支持 Gitee/GitHub HTTPS + Token）
- **学习统计** - 可视化学习进度与贡献热力图
- **离线可用** - 内置词库，无需网络即可学习

## 技术栈

- **语言**: Kotlin 1.9.22
- **架构**: MVVM + Clean Architecture
- **数据库**: Room 2.6.1
- **异步**: Coroutines + Flow
- **UI**: Material Design 3
- **图表**: MPAndroidChart
- **Git操作**: JGit

## 下载安装

从 [Releases](https://github.com/steven0lisa/word-king-app/releases) 下载最新 APK。

## 构建说明

```bash
# 克隆仓库
git clone https://github.com/steven0lisa/word-king-app.git
cd word-king-app

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（需要配置签名）
./gradlew assembleRelease
```

## 配置说明

### AI API 配置
在「设置 → AI配置」中填入：
- API 地址：`https://api.moonshot.cn/v1/chat/completions`
- API Key：从 [Moonshot AI](https://platform.moonshot.cn/) 获取
- 模型：`kimi-k2-0905-preview` 或其他支持模型

### Git 同步配置
1. 生成访问令牌：
   - Gitee：设置 → 私人令牌
   - GitHub：Settings → Developer settings → Personal access tokens
2. 在「设置 → Git同步」中填入：
   - 仓库地址（HTTPS格式）
   - 访问令牌
   - 分支名称（默认 `master`）

## 版本发布

使用自动发布脚本：

```bash
# Node.js 版本（推荐）
node release.js

# Bash 版本
./release.sh
```

脚本会自动：
1. 版本号 +1（支持 major/minor/patch）
2. 更新 `package.json` 和 `app/build.gradle`
3. 提交代码并创建 tag
4. 推送到 GitHub，触发 Actions 构建

## 许可证

[License](LICENSE)
