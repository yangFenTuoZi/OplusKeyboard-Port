#!/usr/bin/env bash
set -euo pipefail

if [[ $# != 2 ]]; then
    printf '用法：bash %s <原版 APK> <--check 或未签名 APK 输出路径>\n' "$0" >&2
    exit 2
fi

tools_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
project_dir=$(dirname "$tools_dir")
source_dir="$project_dir/src"
temp="$project_dir/build"
input=$(realpath -- "$1")
output=$2
sdk=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/AndroidSdk}}
build_tools="$sdk/build-tools/${ANDROID_BUILD_TOOLS_VERSION:-36.1.0}"
android_jar="$sdk/platforms/android-33/android.jar"
sdk_libraries="$sdk/cmdline-tools/latest/lib/external"
dexlib="$sdk_libraries/com/android/tools/smali/smali-dexlib2/3.0.9/smali-dexlib2-3.0.9.jar"
guava="$sdk_libraries/com/google/guava/guava/33.4.0-jre/guava-33.4.0-jre.jar"
hidden_api_aar=${OPLUS_IME_HIDDEN_API_AAR:-$project_dir/input/hiddenapibypass-6.2.aar}

for dependency in "$android_jar" "$dexlib" "$guava" "$hidden_api_aar"; do
    if [[ ! -f "$dependency" ]]; then
        printf '缺少依赖 %s\n' "$dependency" >&2
        exit 1
    fi
done

mkdir -p "$temp"
output_stage=
trap 'rm -rf -- "$temp"; if [[ -n "$output_stage" ]]; then rm -f -- "$output_stage"; fi' EXIT
classpath="$dexlib:$guava"
unzip -p "$hidden_api_aar" classes.jar > "$temp/hiddenapibypass.jar"
javac -encoding UTF-8 -cp "$classpath" -d "$temp/host" "$tools_dir/PatchDex.java"
javac -encoding UTF-8 -Xlint:deprecation --release 11 -cp "$android_jar:$temp/hiddenapibypass.jar" -d "$temp/compat" $(find "$source_dir" -name "*.java")
patch_dex=(java -cp "$temp/host:$classpath" PatchDex "$input")

if [[ "$output" == --check ]]; then
    "${patch_dex[@]}" --check
    exit 0
fi

output=$(realpath -m -- "$output")
if [[ -e "$output" || -L "$output" || ! -d "$(dirname -- "$output")" ]]; then
    printf '输出路径必须是现有目录中的新文件 %s\n' "$output" >&2
    exit 1
fi
mkdir -- "$temp/patch" "$temp/d8"
"${patch_dex[@]}" "$temp/patch"
mkdir -- "$temp/patch/assets"
unzip -p "$input" META-INF/ANDROIDP.RSA | openssl pkcs7 -inform DER -print_certs \
    | openssl x509 -outform DER > "$temp/patch/assets/oplus-ime-original-cert.der"
cert_sha256=$(sha256sum "$temp/patch/assets/oplus-ime-original-cert.der" | cut -d ' ' -f 1)
if [[ "$cert_sha256" != fc98dae63ad39626c8c67fbe83f2f06f74932a9cd146b92cecfc6a047a904386 ]]; then
    printf '原版 APK 的公开证书不符合预期\n' >&2
    exit 1
fi
"$build_tools/d8" --min-api 33 --lib "$android_jar" --output "$temp/d8" \
    "$temp/hiddenapibypass.jar" $(find "$temp/compat" -name "*.class")
mv -- "$temp/d8/classes.dex" "$temp/patch/classes5.dex"
cp -- "$input" "$temp/unsigned.apk"
zip -q -d "$temp/unsigned.apk" META-INF/ANDROIDP.SF META-INF/ANDROIDP.RSA \
    META-INF/MANIFEST.MF assets/dexopt/baseline.prof assets/dexopt/baseline.profm
(
    cd -- "$temp/patch"
    zip -q "$temp/unsigned.apk" classes*.dex assets/oplus-ime-original-cert.der
)
"$build_tools/zipalign" -P 16 -f 4 "$temp/unsigned.apk" "$temp/aligned.apk"
"$build_tools/zipalign" -c -P 16 4 "$temp/aligned.apk"
unzip -tq "$temp/aligned.apk"
cmp <(unzip -p "$input" AndroidManifest.xml) <(unzip -p "$temp/aligned.apk" AndroidManifest.xml)
output_stage=$(mktemp -- "$(dirname -- "$output")/.oplus-ime.XXXXXX")
cp -- "$temp/aligned.apk" "$output_stage"
ln -T -- "$output_stage" "$output"
printf '已生成未签名 APK %s\n' "$output"
