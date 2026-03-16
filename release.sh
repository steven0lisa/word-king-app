#!/bin/bash

# WordKing 自动发布脚本
# 版本号+1、更新文件、提交代码、创建并推送 tag

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
RESET='\033[0m'

# 文件路径
PACKAGE_JSON="package.json"
BUILD_GRADLE="app/build.gradle"

# 打印带颜色的消息
log() {
    local color=$2
    echo -e "${!color}$1${RESET}"
}

# 打印标题
print_title() {
    echo ""
    log "═══════════════════════════════════════════════════════════════ " "CYAN"
    log "  $1" "BOLD"
    log "═══════════════════════════════════════════════════════════════ " "CYAN"
    echo ""
}

# 读取版本号
read_version() {
    grep '"version"' "$PACKAGE_JSON" | head -1 | awk -F: '{print $2}' | sed 's/[ ",]//g'
}

# 解析版本号
parse_version() {
    local version=$1
    version=${version#v}  # 移除 v 前缀
    IFS='.' read -r major minor patch <<< "$version"
    echo "$major $minor $patch"
}

# 格式化版本号
format_version() {
    echo "v$1.$2.$3"
}

# 增加版本号
bump_version() {
    local version=$1
    local type=$2

    local major minor patch
    read -r major minor patch <<< $(parse_version "$version")

    case $type in
        major)
            major=$((major + 1))
            minor=0
            patch=0
            ;;
        minor)
            minor=$((minor + 1))
            patch=0
            ;;
        patch|*)
            patch=$((patch + 1))
            ;;
    esac

    format_version "$major" "$minor" "$patch"
}

# 主函数
main() {
    print_title "WordKing 自动发布工具"

    # 读取当前版本
    local current_version=$(read_version)
    log "当前版本: $current_version" "BLUE"

    # 选择版本更新类型
    echo ""
    log "请选择版本更新类型:" "YELLOW"
    log "  1. patch (补丁版) - 例如: v1.0.0 -> v1.0.1" "RESET"
    log "  2. minor (次版本) - 例如: v1.0.0 -> v1.1.0" "RESET"
    log "  3. major (主版本) - 例如: v1.0.0 -> v2.0.0" "RESET"
    log "  4. 自定义版本" "RESET"

    echo -n "请输入选项 (1-4, 默认: 1): "
    read -r choice

    local type="patch"
    local custom_version=""

    case $choice in
        2) type="minor" ;;
        3) type="major" ;;
        4)
            echo -n "请输入新版本号 (例如: v1.2.3): "
            read -r custom_version
            if [[ ! "$custom_version" =~ ^v?[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
                log "错误: 版本号格式不正确！" "RED"
                exit 1
            fi
            ;;
        *) type="patch" ;;
    esac

    # 计算新版本号
    local new_version
    if [ -n "$custom_version" ]; then
        new_version="$custom_version"
    else
        new_version=$(bump_version "$current_version" "$type")
    fi

    log "" "RESET"
    log "新版本号: $new_version" "GREEN"

    # 确认
    echo -n "确认发布此版本? (y/N): "
    read -r confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        log "已取消发布" "YELLOW"
        exit 0
    fi

    # 获取当前分支
    local current_branch=$(git rev-parse --abbrev-ref HEAD)
    log "" "RESET"
    log "当前分支: $current_branch" "BLUE"

    # 更新 package.json
    local version_without_v=${new_version#v}
    sed -i '' "s/\"version\": \".*\"/\"version\": \"$version_without_v\"/" "$PACKAGE_JSON"
    log "✓ 已更新 package.json" "GREEN"

    # 更新 build.gradle
    local old_version_code=$(grep "versionCode" "$BUILD_GRADLE" | awk '{print $2}')
    local new_version_code=$((old_version_code + 1))
    sed -i '' "s/versionName \".*\"/versionName \"$new_version\"/" "$BUILD_GRADLE"
    sed -i '' "s/versionCode .*/versionCode $new_version_code/" "$BUILD_GRADLE"
    log "✓ 已更新 build.gradle" "GREEN"

    # 提交更改
    print_title "提交更改"
    log "正在提交更改..." "BLUE"
    log "可能需要输入 Git 凭据..." "YELLOW"

    git add "$PACKAGE_JSON" "$BUILD_GRADLE"
    git commit -m "chore: release $new_version"
    log "✓ 已提交更改" "GREEN"

    # 创建 tag
    print_title "创建标签"
    log "正在创建标签 $new_version..." "BLUE"

    git tag -f "$new_version" 2>/dev/null || true
    log "✓ 已创建本地标签" "GREEN"

    # 推送到远程
    print_title "推送到远程"
    log "正在推送代码和标签..." "BLUE"
    log "可能需要输入凭据..." "YELLOW"

    git push origin "$current_branch"
    git push origin "$new_version" --force

    log "✓ 已推送代码" "GREEN"
    log "✓ 已推送标签" "GREEN"

    # 完成
    print_title "发布完成！"
    log "版本 $new_version 已成功发布" "GREEN"
    echo ""
    log "GitHub Actions 将自动构建并发布 APK" "BLUE"
    log "查看构建状态: https://github.com/steven0lisa/word-king-app/actions" "RESET"
    echo ""
}

# 运行主函数
main
