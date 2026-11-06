#!/usr/bin/env bash
# 判断本次 push 是否需要发布新版本（供 .github/workflows/release.yml 调用）。
#
# 版本号唯一来源是 app/build.gradle.kts：
#   versionCode = 4
#   versionName = "2.1.0"
#
# 发布规则：
#   * 推送到 main / 手动触发（workflow_dispatch）：
#       - versionName 严格高于当前最高 v* tag，且 versionCode 比该 tag 时更大 -> 发布
#       - versionName 等于最高 tag 但 Release 缺失（上次失败重试）-> 发布
#       - 否则跳过
#   * 推送 tag（refs/tags/vX.Y.Z）：
#       - tag 必须与 versionName 一致（防止发错版本）
#       - Release 不存在时发布
#
# 输出（写入 $GITHUB_OUTPUT）：should_release / version / version_code / reason
set -euo pipefail

GRADLE_FILE="${GRADLE_FILE:-app/build.gradle.kts}"
OUTPUT_FILE="${GITHUB_OUTPUT:-/dev/stdout}"

# $1 严格大于 $2 时返回 0（GNU/BSD sort -V 语义）
version_gt() {
  [ "$1" != "$2" ] || return 1
  [ "$(printf '%s\n%s\n' "$1" "$2" | sort -V | tail -n 1)" = "$1" ]
}

# $1 = tag 名；已存在同名 GitHub Release 时返回 0
release_exists() {
  if [ -z "${GH_TOKEN:-}" ] && [ -z "${GITHUB_TOKEN:-}" ]; then
    echo "::warning::GH_TOKEN 未设置，无法查询已有 Release，按不存在处理" >&2
    return 1
  fi
  gh release view "$1" >/dev/null 2>&1
}

read_version() { # $1 = build.gradle.kts 内容
  printf '%s\n' "$1" | awk -F'"' '/^[[:space:]]*versionName[[:space:]]*=/ { print $2; exit }'
}

read_version_code() { # $1 = build.gradle.kts 内容
  printf '%s\n' "$1" | awk '/^[[:space:]]*versionCode[[:space:]]*=/ { print $3; exit }'
}

GRADLE_CONTENT=$(cat "$GRADLE_FILE")
VERSION=$(read_version "$GRADLE_CONTENT")
VERSION_CODE=$(read_version_code "$GRADLE_CONTENT")

if [ -z "$VERSION" ] || ! [[ "$VERSION_CODE" =~ ^[0-9]+$ ]]; then
  echo "::error file=$GRADLE_FILE::无法读取版本号（versionName='${VERSION}', versionCode='${VERSION_CODE}'）"
  exit 1
fi

should_release=false
reason=""

if [[ "${GITHUB_REF:-}" == refs/tags/v* ]]; then
  TAG_VERSION="${GITHUB_REF#refs/tags/v}"
  if [ "$TAG_VERSION" != "$VERSION" ]; then
    echo "::error file=$GRADLE_FILE::tag v${TAG_VERSION} 与 versionName ${VERSION} 不一致，请先同步版本号"
    exit 1
  fi
  if release_exists "v$VERSION"; then
    reason="Release v$VERSION 已存在，跳过"
  else
    should_release=true
    reason="手动推送 tag v$VERSION"
  fi
else
  LATEST_TAG=$(git tag --list 'v[0-9]*' --sort=-v:refname | sed -n '1p')
  if [ -z "$LATEST_TAG" ]; then
    should_release=true
    reason="尚无 v* tag，首次发布"
  else
    LATEST_VERSION="${LATEST_TAG#v}"
    if [ "$VERSION" = "$LATEST_VERSION" ]; then
      if release_exists "v$VERSION"; then
        reason="v$VERSION 已是最新 tag 且 Release 已存在，跳过"
      else
        should_release=true
        reason="tag v$VERSION 已存在但 Release 缺失，重试发布"
      fi
    elif version_gt "$VERSION" "$LATEST_VERSION"; then
      PREV_GRADLE_CONTENT=$(git show "${LATEST_TAG}:${GRADLE_FILE}" 2>/dev/null || true)
      PREV_VERSION_CODE=$(read_version_code "$PREV_GRADLE_CONTENT")
      if ! [[ "$PREV_VERSION_CODE" =~ ^[0-9]+$ ]]; then
        echo "::error::无法从 ${LATEST_TAG}:${GRADLE_FILE} 读取 versionCode"
        exit 1
      fi
      if [ "$VERSION_CODE" -le "$PREV_VERSION_CODE" ]; then
        echo "::error file=$GRADLE_FILE::版本号升级时 versionCode 必须递增：当前 ${VERSION_CODE}，${LATEST_TAG} 为 ${PREV_VERSION_CODE}"
        exit 1
      fi
      should_release=true
      reason="versionName ${VERSION} 高于 ${LATEST_VERSION}，触发自动发布"
    else
      reason="versionName ${VERSION} 不高于最新 tag ${LATEST_VERSION}，跳过"
      echo "::warning::$reason"
    fi
  fi
fi

echo "should_release=$should_release"
echo "version=$VERSION"
echo "version_code=$VERSION_CODE"
echo "reason=$reason"

{
  echo "should_release=$should_release"
  echo "version=$VERSION"
  echo "version_code=$VERSION_CODE"
  echo "reason=$reason"
} >>"$OUTPUT_FILE"
