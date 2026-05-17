# Stonecutter Migration: Add Minecraft 26.1 Support

**ステータス**: 実装完了 (branch `feat/stonecutter-mc26`)。Phase A〜H は shipped (commits `441b209` / `f5f27c4` / `23c0ae8`)。本ファイルは実装後の実行ログを兼ねている (DoD のうち実行可能なものは ✅、CI green / PR マージ待ちは未チェックのまま)。

**前提となる完了済み作業**: PR #23 (`Introduce Stonecutter` series, 1d443dbd) で 1.21.11 単一バージョンの Stonecutter 化は shipped。本計画はその上に **26.1 を 2 つ目のバージョンとして追加**する。

---

## 0. ゴール / 非ゴール

### ゴール
- Minecraft 26.1 を Stonecutter 管理対象に追加する。`./gradlew :26.1:build` と `./gradlew chiseledBuild` の両方で 1.21.11 / 26.1 双方の jar が生成できる。
- 1.21.11 ビルドは PR #23 と完全に**同等**の挙動 (jar 内容、CI、release 経路) を維持する。
- 26.1 ビルドは公式 26.1 の **non-obfuscated 開発フロー** (`net.fabricmc.fabric-loom` plugin id, Java 25, Loom 1.16+, modImplementation→implementation, remapJar→jar) に準拠する。
- 1.21.11 と 26.1 の **buildscript を完全分離** する: `build.fabric.gradle.kts` (1.21.11, 旧 fabric-loom plugin id) / `build.fabric.unobfuscated.gradle.kts` (26.1+, 新 net.fabricmc.fabric-loom plugin id)。
- CI matrix で両バージョンを並行ビルド・テストする。
- リリースタグ `v<modver>+26.1` で 26.1 jar が GitHub Release に publish される。

### 非ゴール (本 PR スコープ外)
- 1.21.10 以前の旧バージョン (legacy グループ) の Stonecutter 化。別計画書で扱う (未作成)。
- 既存の MannequinEntity システムの 26.1 バニラ Mannequin への置き換え。26.1 でも引き続き mod 独自の MannequinEntity を使用する (本計画は「ビルドが通って既存挙動が維持される」までを目指す)。
- 1.21.11 の Java/Loom/Loader 等のバージョンアップ。
- 26.2 以降への追加対応 (本計画は 26.1 のみ)。
- 既存ブランチ群 (1.21.10, 1.21.7 等) の整理。

---

## 1. 前提と制約

### 1.1 アーキテクチャ上の制約

**26.1 と 1.21.11 では Loom plugin id が異なる**。
- 1.21.11: `id("fabric-loom") version "1.14-SNAPSHOT"` (旧 plugin id, obfuscated mappings ベース、Mojmap mapping を `loom.officialMojangMappings()` 経由で取得)
- 26.1: `id("net.fabricmc.fabric-loom") version "1.16-SNAPSHOT"` (新 plugin id, non-obfuscated, mapping 不要)

> **検証要 (実装時)**: Loom 1.16-SNAPSHOT の最新バージョン (1.17.0-alpha.x の可能性あり) と plugin id (`net.fabricmc.fabric-loom` が正式) を実装着手時に Maven で再確認する。本計画では便宜上「Loom 1.16-SNAPSHOT (新 plugin id)」と表記するが、実装時に最新安定/snapshot に追従する。

これらは単一の `plugins {}` ブロック内で `?if` プリプロセッサ切り替えできない (Gradle の plugins ブロックは静的解析される)。**Stonecutter の per-version `buildscript()` 機能で物理的に build script を分離する**のが推奨パターン (librarian で公式 docs および fabric-permissions-api 等の OSS 実例を確認済)。

### 1.2 Stonecutter buildscript 分離の構文

settings.gradle.kts (例示。**現行実装では `centralScript` 行は省略**しており、`versions(...).buildscript(...)` のみで per-version 切替している):
```kotlin
stonecutter {
    kotlinController = true
    // centralScript は per-version buildscript 指定があるバージョンには適用されない
    centralScript = "build.gradle.kts"  // fallback (使われない想定)
    create(rootProject) {
        versions("1.21.11").buildscript("build.fabric.gradle.kts")
        versions("26.1").buildscript("build.fabric.unobfuscated.gradle.kts")
        vcsVersion = "1.21.11"
    }
}
```

> **検証要 (実装時)**: 上記構文 (`versions("X").buildscript("Y.kts")`) は librarian 調査で HIGH confidence と回答されたが、実際の挙動 (ファイルが見つからないとどう失敗するか、各 version subproject から見える working directory はどこか) は実装時に最小再現で確認する。万一構文が異なる場合 (例: `vers(...)` / 引数順序違い) は公式 docs に従って修正する。

### 1.3 26.1 の主要な breaking change (本 mod に影響するもののみ)

librarian + explore 調査結果より、本 mod の 26.1 移行で対応が必要な項目:

1. ~~**`UseBlockCallback` → `BlockUseCallback`** にリネーム~~ → **実装結果**: 26.1 でも `UseBlockCallback` がそのまま生存しておりコンパイル成功。切替不要。
2. **`fabric-key-binding-api-v1` → `fabric-key-mapping-api-v1`** モジュール名変更
3. **`modImplementation` → `implementation`**, **`remapJar` → `jar`** (新 Loom)
4. ~~**fabric.mod.json `loom:injected_interfaces` の intermediary class 名 → Mojmap 名**~~ → **実装結果**: injected_interfaces 自体を**削除**し、利用箇所 (8 mixin Java ファイル) で explicit cast 方式 (`((PosingFlag) instance).fabSit$xxx()`) に統一。intermediary が無い 26.1 と obfuscated な 1.21.11 の両方で同一ソースから動くため、ブロック自体を消す方が単純。
5. **Java 21 → Java 25** (toolchain)
6. **Mixin ターゲット**: librarian 検証で全 31 クラス EXISTS 確認済 (Avatar/Mannequin は 25w36a で追加。AvatarRenderState 等も存続)。**コード変更不要**の見込み。
7. **AccessWidener (`ClientboundPlayerInfoUpdatePacket entries`)**: フィールド存続。形式変更不要 (元から Mojmap)。

本 mod が 26.1 で **使っていない** (= 影響なし) 削除/リネーム API:
- `HudRenderCallback`, `ColorProviderRegistry`, `BlockRenderLayerMap`, `FluidRenderHandler`, `ItemStack` の生成タイミング, `fabric-convention-tags-v1`, `fabric-loot-api-v2` 系統 (explore で 0 ヒット確認済)

### 1.4 mod ロジックの変更方針

**最小変更**で動かす。
- `MannequinEntity` (mod 独自) は 26.1 でもそのまま使う (26.1 バニラの Mannequin に置き換える検討は別 PR)。
- 既存の sit/lay/spin/swim 挙動は 1.21.11 と 26.1 で同等であることをサーバーテストで検証する。
- API リネームは 1 対 1 で対応 (`UseBlockCallback` → `BlockUseCallback`)。プリプロセッサ切替が必要な箇所のみ Stonecutter `?if` を入れる。

---

## 2. 最終ディレクトリ構造

```
FabPose/
├── settings.gradle.kts                         # 26.1 を versions に追加 + per-version buildscript 指定
├── build.fabric.gradle.kts                     # 1.21.11 用 (現 build.gradle.kts をリネーム)
├── build.fabric.unobfuscated.gradle.kts        # 26.1 用 (新規)
├── stonecutter.gradle.kts                      # Stonecutter controller (既存、active 切替のみ調整)
├── gradle.properties                           # 共通プロパティ (maven_group, archives_base_name)
├── versions/
│   ├── 1.21.11/
│   │   └── gradle.properties                   # (既存) MC 1.21.11 系プロパティ
│   └── 26.1/
│       └── gradle.properties                   # (新規) MC 26.1 系プロパティ
├── src/                                        # 共有ソース (rootProject.file 参照は変更不要)
│   ├── main/
│   │   ├── java/                               # FabSit.java の UseBlockCallback だけ Stonecutter `?if` 切替
│   │   ├── kotlin/
│   │   └── resources/
│   │       ├── fabric.mod.json                 # depends + injected_interfaces を `?if` で切替
│   │       └── fabpose.accesswidener           # 変更不要 (元から Mojmap、フィールド存続)
│   ├── clienttest/
│   └── servertest/
└── plans/
    ├── stonecutter-migration-modern.md         # PR #23 完了済
    └── stonecutter-migration-mc26.md           # 本計画
```

**削除されるもの**: ルート直下の `build.gradle.kts` (centralScript 指定は残しておくが、per-version buildscript が指定された全バージョンで実際には参照されない。fallback 用に空ファイルかリネーム前のまま残すかは Phase B で決定)。

---

## 3. 詳細手順

### Phase A — 下調べ・ベースライン

#### A-1. PR #23 マージ後の main 状態確認
- **使用ツール**: `git log --oneline main -5`, `git rev-parse main`
- **手順**: main HEAD が `1d443dbd` (PR #23 merge commit) であることを確認。
- **期待結果**: HEAD が PR #23 merge 後を指す。

#### A-2. 26.1 Maven artifacts 最新バージョンの再確認
- **使用ツール**: `webfetch` で Fabric Maven メタデータを実装着手時に再取得
  - https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml (Loom 最新 1.16/1.17 系)
  - https://meta.fabricmc.net/v2/versions/loader (loader 最新)
  - https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml (fabric-api 26.1 系)
  - https://maven.fabricmc.net/net/fabricmc/fabric-language-kotlin/maven-metadata.xml (FLK 26.1 対応)
  - https://repo1.maven.org/maven2/me/lucko/fabric-permissions-api/maven-metadata.xml (permissions-api 0.7.0 系)
- **期待結果**: 各 artifact の最新版が確定し、versions/26.1/gradle.properties に書き込む値が決まる。
- **参考値 (librarian 2026-05-12 時点)**: loom 1.17.0-alpha.8 / loader 0.19.2 / fabric-api 0.144.0+26.1〜0.148.x / FLK 1.13.11+kotlin.2.3.21 / permissions-api 0.7.0。**実装時に必ず再確認**。

#### A-3. fabric-example-mod 26.1 ブランチの buildscript 構造確認
- **使用ツール**: `webfetch` https://raw.githubusercontent.com/FabricMC/fabric-example-mod/26.1/build.gradle (および .kts 版があれば)
- **期待結果**: 新 plugin id 利用例 / dependency 表記 / processResources 方式 / Java 25 toolchain 設定の参考実装が手元に揃う。

#### A-4. Stonecutter per-version buildscript の最小再現
- **使用ツール**: 一時ディレクトリで Stonecutter 0.9.3 + 2 buildscript の最小プロジェクトを作って `./gradlew projects -q` で構文を検証
- **手順**: `tmp-stonecutter-poc/` を作成 → settings.gradle.kts に `versions("a").buildscript("a.kts"); versions("b").buildscript("b.kts")` → 各 .kts に `tasks.register("hello") { doLast { println("$name") } }` だけ書いて `:a:hello :b:hello` 実行
- **期待結果**: 両方 `BUILD SUCCESSFUL`。構文が違った場合はここで判明する。完了後 `tmp-stonecutter-poc/` 削除。
- **失敗時**: Stonecutter 公式 docs (https://stonecutter.kikugie.dev/wiki/start/builds) を再読し、正しい構文を確定する。

#### A-5. ベースライン jar 保存
- **使用ツール**: `./gradlew :1.21.11:build` + `cp` で `temp/baseline-1.21.11.jar` に保存
- **手順**: 現状 (mc26 ブランチ作成直後 = main 状態) で 1.21.11 ビルドを実行し、jar を保存。実装後の DoD 検証で「1.21.11 jar が同等」確認に使用。
- **期待結果**: `temp/baseline-1.21.11.jar` 存在。`temp/` は .gitignore 済 (PR #23 で追加済)。

### Phase B — settings.gradle.kts と buildscript 分離

#### B-1. 既存 build.gradle.kts を build.fabric.gradle.kts にリネーム
- **使用ツール**: `git mv build.gradle.kts build.fabric.gradle.kts`
- **手順**: ファイル名変更のみ。中身は無変更。
- **QA**: `git status` で rename 認識確認、`./gradlew :1.21.11:projects -q` ではこの段階ではまだ動かない (settings 未調整)。

#### B-2. settings.gradle.kts に 26.1 を追加 + per-version buildscript 指定
- **使用ツール**: `edit`
- **変更**: `versions("1.21.11")` を `versions("1.21.11").buildscript("build.fabric.gradle.kts"); versions("26.1").buildscript("build.fabric.unobfuscated.gradle.kts")` に展開。`vcsVersion = "1.21.11"` は維持。
- **QA**: `./gradlew projects -q | grep ':26.1'` でサブプロジェクト認識確認。`./gradlew tasks -q` でエラー無く起動することを確認 (この時点で build.fabric.unobfuscated.gradle.kts が存在しないと失敗する想定 → B-3 で先に空ファイルを置く)。

#### B-3. build.fabric.unobfuscated.gradle.kts の最小スケルトン作成
- **使用ツール**: `write`
- **内容**: `plugins { id("net.fabricmc.fabric-loom") version "<verified>" }` + 空の `loom {}` + 空の `dependencies {}` + 空の `tasks.processResources {}` のみ。compile はまだ通らなくてよいが Gradle の構文解析は通ること。
- **QA**: `./gradlew :26.1:tasks -q | head` で `:26.1` の task 一覧が出ること。`UnknownPluginException` が出たら plugin id か repository 設定 (settings.gradle.kts pluginManagement) を確認。

#### B-4. settings.gradle.kts の pluginManagement に 26.1 用 repository 追加 (必要なら)
- **使用ツール**: `read` で現在の pluginManagement 確認 → 必要なら `edit`
- **想定**: 既に `https://maven.fabricmc.net/` は登録済のため追加不要の見込み。Stonecutter maven も登録済。
- **QA**: B-3 の `:26.1:tasks` が成功すれば OK。

#### B-5. versions/26.1/gradle.properties 作成
- **使用ツール**: `write`
- **内容** (実装時 A-2 で確定した値で書く):
  ```properties
  minecraft_version=26.1
  loader_version=<verified>
  fabric_version=<verified>+26.1
  flk_version=<verified>+kotlin.<verified>
  loom_version=<verified>
  java_version=25
  ```
- **QA**: `./gradlew :26.1:properties | grep -E 'minecraft_version|loader_version'` で値が読めること。

### Phase C — build.fabric.unobfuscated.gradle.kts 本実装

#### C-1. 1.21.11 buildscript からの差分洗い出し
- **使用ツール**: `read build.fabric.gradle.kts` 全文を確認
- **手順**: 26.1 用に変更すべきポイントをリストアップ:
  1. `plugins { id("fabric-loom") version "1.14-SNAPSHOT" }` → `id("net.fabricmc.fabric-loom") version "<verified>"`
  2. `java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }` → `25`
  3. `kotlin { jvmToolchain(21) }` → `25`
  4. `mappings(loom.officialMojangMappings())` → **削除** (un-obfuscated は mapping 不要)
  5. `modImplementation("net.fabricmc:fabric-loader:$loaderVersion")` → `implementation(...)`
  6. `modImplementation(fabricApi.module(it, fabricVersion))` → `implementation(...)`
  7. `modLocalRuntime("net.fabricmc.fabric-api:fabric-api:$fabricVersion")` → `runtimeOnly(...)`
  8. `modImplementation("net.fabricmc:fabric-language-kotlin:$flkVersion")` → `implementation(...)`
  9. `modImplementation(include("me.lucko:fabric-permissions-api:0.7.0")!!)` → `implementation(include(...))!!` (新 plugin id でも `include()` は維持される想定 → A-3 で確認)
  10. fabric-api `setOf` の `fabric-key-binding-api-v1` を `fabric-key-mapping-api-v1` に変更
  11. `tasks.remapJar` 関連の処理 → `tasks.jar` に置き換え (公式 26.1 ガイドに従う)
  12. `accessWidenerPath.set(rootProject.file("src/main/resources/fabpose.accesswidener"))` は維持
  13. sourceSets srcDirs (rootProject.file 参照) は維持
  14. `cleanupXvfb` task / runClienttest 周りは維持 (新 Loom でも `loom { runs { create(...) } }` API は同等の見込み → A-3 で確認)
  15. `tasks.withType<AbstractCopyTask>().configureEach { duplicatesStrategy = DuplicatesStrategy.INCLUDE }` は維持
  16. processResources の `expand()` は維持

#### C-2. C-1 の変更を build.fabric.unobfuscated.gradle.kts に反映
- **使用ツール**: `write` (B-3 のスケルトンを書き換え)
- **手順**: 1.21.11 版を雛形にして上記 16 点を適用。kotlin block と sourceSets block は 1.21.11 版から丸ごとコピー。
- **QA**: `./gradlew :26.1:build --dry-run` で task graph が出ること。実 build はまだ失敗してよい。

#### C-3. fabric.mod.json の `loom:injected_interfaces` を削除し cast 方式へ移行
- **実装結果**: 当初は `?if` 切替を予定していたが、より単純な解として `custom.loom:injected_interfaces` ブロック自体を削除し、利用箇所 (8 mixin Java ファイル) で explicit cast (`((PosingFlag) instance).fabSit$xxx()`) に書き換えた。これにより 1.21.11 / 26.1 双方で同一の fabric.mod.json が使える。
- **QA**: 1.21.11 / 26.1 双方の jar の fabric.mod.json に `custom` キーが存在しないこと、両 build が成功すること。

#### C-4. fabric.mod.json の depends 切替 (必要なら)
- **使用ツール**: `read` で現在の depends 確認
- **手順**: `fabric-key-binding-api-v1` 系の明示 depends があれば 26.1 で `fabric-key-mapping-api-v1` に切替。現行は `fabric-api` 全体への wildcard depends のみで個別 depends はなさそう (再確認)。
- **QA**: 両ビルド後に `unzip -p ... fabric.mod.json | jq .depends` で値確認。

#### C-5. fabric.mod.json の minecraft / java バージョン切替
- **使用ツール**: `edit`
- **手順**: `depends.java` を 1.21.11 では `>=21`, 26.1 では `>=25` に切替 (Stonecutter `?if`)。`depends.minecraft` は `>=${minecraft_version}` のままで両方対応 (placeholder が processResources で展開される)。
- **QA**: jq 確認。

### Phase D — Java/Kotlin ソースの 26.1 対応

#### D-1. UseBlockCallback の動作確認
- **実装結果**: `UseBlockCallback` は 26.1 でも生存していたためコード変更不要。`src/main/java/net/fill1890/fabsit/FabSit.java` は両 MC で同一コードのままコンパイル・ロード成功。当初想定していた `BlockUseCallback` への切替は不要。

#### D-2. その他のコンパイルエラー対応 (random fix)
- **使用ツール**: `./gradlew :26.1:compileJava :26.1:compileKotlin` を実行 → エラー出たら 1 件ずつ対応
- **方針**: librarian + explore 結果より重大な API 変更は無い見込みだが、26.1 でしか壊れていない箇所は Stonecutter `?if` で 1.21.11 版を保護しつつ修正する。
- **QA**: 両 sourceSet の compile が green。

### Phase E — Mixin / AW / リソース検証

#### E-1. 全 Mixin が 26.1 でも remap 通ることの確認
- **使用ツール**: `./gradlew :26.1:remapJar` (新 Loom では `tasks.jar` だが、Mixin の検証は build に内包される)
- **手順**: `./gradlew :26.1:build` を実行。`Cannot remap XXX because it does not exist in any of the targets` 警告/エラーが出ないことを確認。
- **期待結果**: librarian 検証で全 Mixin ターゲット EXISTS のため警告 0 件。出た場合は該当クラスの 26.1 での新名 (NeoForge javadocs / fabric-example-mod 26.1 で確認) に直す。
- **QA**: build log を grep `'Cannot remap'` で 0 ヒット。

#### E-2. AccessWidener が 26.1 で適用されることの確認
- **使用ツール**: `./gradlew :26.1:build` の build/processed access widener (新 Loom の出力場所は要確認)
- **手順**: AW フィールド `ClientboundPlayerInfoUpdatePacket entries` が 26.1 で存続する (librarian MEDIUM confidence) ため、build が通れば適用成功。
- **QA**: build SUCCESSFUL。jar 内に `fabpose.accesswidener` が含まれること (`unzip -l`)。

### Phase F — テスト実行

#### F-1. :1.21.11:runServertest (non-regression)
- **使用ツール**: `./gradlew :1.21.11:runServertest`
- **手順**: 1.21.11 側のサーバーゲームテストを実行し、現行と同じ 12/12 pass を維持していること。
- **期待結果**: `versions/1.21.11/build/runServertest/junit.xml` で `<failure>` 0 件、`testsuite[@tests]` が 12 (PR #23 で確認した数)。
- **QA**: `grep -c '<failure' versions/1.21.11/build/runServertest/junit.xml || echo 0` で 0、`grep -oE 'tests="[0-9]+"' versions/1.21.11/build/runServertest/junit.xml | head -1` で 12。

#### F-2. :26.1:runServertest (gametest)
- **使用ツール**: `./gradlew :26.1:runServertest`
- **手順**: 26.1 側で同一テストスイートを実行。失敗したらバニラ挙動変更 vs mod 側 bug を切り分け。
- **期待結果**: 12/12 pass。
- **QA**: `versions/26.1/build/runServertest/junit.xml` で `<failure>` 0 件、`tests="12"`。

#### F-3. :1.21.11:lintKotlin / :26.1:lintKotlin
- **使用ツール**: `./gradlew :1.21.11:lintKotlin :26.1:lintKotlin`
- **手順**: 両 subproject で kotlinter (Kotlinter) を実行。
- **期待結果**: どちらも `BUILD SUCCESSFUL`、`*** lintKotlin*Source ***` セクションでエラー報告なし。
- **QA**: 両 task の終了コード 0、`versions/<mc>/build/reports/ktlint/` 配下のレポートに `<error` を含む行が 0 件。

#### F-4. :26.1:runClienttest (オプション、ローカル PulseAudio 問題で SKIP 可)
- **使用ツール**: `./gradlew :26.1:runClienttest` (CI 上は build.yml の `runClienttest` ジョブで実施)
- **手順**: ローカル PulseAudio 問題で fail し得る (PR #23 で 1.21.11 同様)。CI で確認する方針。
- **期待結果**: ローカルは exit 134 許容 (xvfb / cleanupXvfb のみ正常終了確認)、CI 上で `actions/upload-artifact` の test report に `<failure>` 0 件。
- **QA**: ローカルで実行する場合は `./gradlew :26.1:runClienttest` の前後で `pgrep Xvfb` を確認 (cleanupXvfb で終了されているはず)。CI は build.yml の matrix.task=runClienttest ジョブが green であること (PR push 後に GitHub Actions UI で確認)。

#### F-5. :1.21.11 ビルド jar non-regression diff
- **使用ツール**: `./gradlew :1.21.11:build` + `unzip` + `jq` + `diff`
- **手順**: ベースライン jar (`temp/baseline-1.21.11.jar`、Phase A-5 で保存) と新 jar (`versions/1.21.11/build/libs/fabpose-...+1.21.11.jar`) を `/tmp/jar-compare/old` `/tmp/jar-compare/new` に解凍 → 以下の diff:
  - `(cd /tmp/jar-compare/old && find . -type f | sort) > /tmp/old.list; (cd /tmp/jar-compare/new && find . -type f | sort) > /tmp/new.list; diff /tmp/old.list /tmp/new.list` でファイル一覧一致 (Stonecutter メタファイル追加は許容)
  - `diff <(jq -S 'del(.version)' /tmp/jar-compare/old/fabric.mod.json) <(jq -S 'del(.version)' /tmp/jar-compare/new/fabric.mod.json)` で fabric.mod.json 一致
  - `diff /tmp/jar-compare/old/fabpose.mixins.json /tmp/jar-compare/new/fabpose.mixins.json` で mixins 設定一致
  - `diff /tmp/jar-compare/old/fabpose.accesswidener /tmp/jar-compare/new/fabpose.accesswidener` で AW 一致
- **期待結果**: 全 diff が 0 行 (Stonecutter 追加メタファイルは許容、許容範囲を Phase A-5 で具体名で確認)。

#### F-6. chiseledBuild 全体検証
- **使用ツール**: `./gradlew chiseledBuild`
- **手順**: 1.21.11 と 26.1 両方の jar が生成されること。
- **期待結果**: `find versions -path '*/build/libs/*.jar' -name 'fabpose-*' | sort` で 2+ ヒット (`versions/1.21.11/build/libs/fabpose-*+1.21.11.jar` と `versions/26.1/build/libs/fabpose-*+26.1.jar`)。
- **QA**: `find versions -path '*/build/libs/fabpose-*+1.21.11.jar' | wc -l` ≥ 1、同様に `+26.1.jar` で ≥ 1。

### Phase G — CI / リリース更新

#### G-1. .github/workflows/build.yml の matrix.version に 26.1 を追加 + Java 切替
- **使用ツール**: `edit`
- **変更**:
  1. `version: ['1.21.11']` → `version: ['1.21.11', '26.1']`
  2. matrix に `include:` ブロックを追加して MC バージョンと JDK バージョンを mapping:
     ```yaml
     matrix:
       version: ['1.21.11', '26.1']
       task: ['build', 'runServertest', 'runClienttest']
       include:
         - version: '1.21.11'
           java: '21'
         - version: '26.1'
           java: '25'
     ```
  3. `actions/setup-java` の `java-version` を `${{ matrix.java }}` に変更。
- **検討点**: Loom 1.14 (1.21.11) が JDK 25 host で動くか不明なため、安全側で matrix で切替。ただし Gradle toolchain auto-provision で 1.21.11 buildscript 内の `JavaLanguageVersion.of(21)` が working する可能性もあり (host JDK と build target JDK は別)。**判断**: setup-java を MC バージョンに合わせる方針を採用 (host = build target で揃える)。
- **QA**: `nix run nixpkgs#actionlint -- .github/workflows/build.yml` で lint クリーン。`yq '.jobs.build.strategy.matrix.include' .github/workflows/build.yml` で 2 entries 確認。GitHub Actions UI で PR push 後、6 ジョブ (2 version × 3 task) 起動を視認。

#### G-2. .github/workflows/build.yml の runClienttest step の env (ALSOFT/SDL) は 26.1 でも維持
- 変更不要 (matrix 全体に既に適用済み)。

#### G-3. .github/workflows/publish.yml の validate と build を 26.1 対応
- **使用ツール**: `edit` (publish.yml の複数 step を編集)

##### G-3-a. validate regex の検証
- **手順**: 現行 regex `^v[0-9A-Za-z._-]+\+[0-9]+\.[0-9]+(\.[0-9]+)?$` は `(\.[0-9]+)?` がオプションのため `v0.0.0+26.1` も `v0.0.0+1.21.11` もマッチする想定。
- **QA**: 手元で `printf 'v0.0.0+26.1\n' | grep -E '^v[0-9A-Za-z._-]+\+[0-9]+\.[0-9]+(\.[0-9]+)?$'` がマッチ、`printf 'v0.0.0+invalid\n' | grep -E '...'` がマッチしないこと。**期待結果**: 修正不要 (validate 自体は両 MC で成立)。

##### G-3-b. setup-java の Java 25 切替 (MC バージョンに応じて)
- **問題**: 現行 publish.yml L36-40 は `java-version: '21'` 固定。26.1 ビルドは Java 25 toolchain を要求するため、このままでは `:26.1:build` が `Could not find any version matching '25'` 系で失敗する可能性が高い (Gradle toolchain auto-provision が CI で無効の場合)。
- **変更**: validate step の前または直後に MC バージョン → JDK バージョンの mapping step を追加。例:
  ```yaml
  - name: Determine Java version for MC
    id: jdk
    env:
      MC_VERSION: ${{ steps.tag.outputs._1 }}
    run: |
      case "${MC_VERSION}" in
        26.*|27.*) echo "java=25" >> "$GITHUB_OUTPUT" ;;
        *)         echo "java=21" >> "$GITHUB_OUTPUT" ;;
      esac
  - name: Setup java
    uses: actions/setup-java@... # 既存 pin
    with:
      java-version: ${{ steps.jdk.outputs.java }}
      distribution: 'temurin'
  ```
  - 既存の `Setup java` step (L36-40) を上記の `Setup java` に置き換え、Setup java の前に `Determine Java version for MC` step を挿入。
- **QA**: yamllint クリーン。`yq '.jobs.check.steps[] | select(.name == "Setup java") | .with."java-version"' .github/workflows/publish.yml` で `${{ steps.jdk.outputs.java }}` が出ること。

##### G-3-c. mc-publish の `java:` メタデータも MC バージョンに応じて切替
- **問題**: 現行 L101 は `java: 21` 固定。CurseForge / Modrinth に公開される jar のメタデータが Java 25 ターゲット artifact に対して 21 と表示される。
- **変更**: L101 を `java: ${{ steps.jdk.outputs.java }}` に置き換え (G-3-b の出力を流用)。
- **QA**: `yq '.jobs.check.steps[] | select(.uses | test("Kir-Antipov/mc-publish")) | .with.java' .github/workflows/publish.yml` で `${{ steps.jdk.outputs.java }}` が出ること。

##### G-3-d. build step / artifact path
- **手順**: `./gradlew :${{ steps.tag.outputs._1 }}:build` (L66) と `files: versions/${{ steps.tag.outputs._1 }}/build/libs/fabpose-...+${{ steps.tag.outputs._1 }}.jar` (L104-106) は MC バージョンを動的に取るため変更不要。
- **QA**: `grep 'steps.tag.outputs._1' .github/workflows/publish.yml | wc -l` で 6+ ヒット (validate / build / files / mc-publish.game-versions 等) を確認。

##### G-3-e. drymrun 検証 (タグ push 不要)
- **使用ツール**: `act` (nix で利用可能なら) または GitHub Actions の `workflow_dispatch` 一時トリガー
- **代替手順**: `gh workflow view publish.yml` で yaml 構文 OK 確認 + `nix run nixpkgs#actionlint -- .github/workflows/publish.yml` で lint 通ること。
- **QA**: actionlint エラー 0 件。

### Phase H — ドキュメント更新

#### H-1. CLAUDE.md (および AGENTS.md symlink) の更新
- **使用ツール**: `edit`
- **変更点**:
  - `Build and Run` セクションに `:26.1:build` 例を追加
  - `Adding a new Minecraft version` 手順に「**26.1+ の場合は build.fabric.unobfuscated.gradle.kts 用の per-version `buildscript()` 指定が必要**」と注記
  - non-obfuscated と obfuscated buildscript 2 種類が並行する旨を 1〜2 文で説明
- **QA**: `read CLAUDE.md` で該当箇所反映確認。

#### H-2. plans/stonecutter-migration-mc26.md (本書) のステータス更新
- 実装完了時に冒頭ステータスバナーを「実装完了 (branch ...)」に変更し、各 Phase チェックボックスを `[x]` に。

#### H-3. README.md
- 現状 README に Gradle コマンド記述なし → 触らない。

---

## 4. リスク表

| ID | リスク | 影響 | 対策 |
|---|---|---|---|
| R1 | Stonecutter `versions("X").buildscript("Y.kts")` 構文が librarian 報告と違う | 着手時 Phase A-4 で破綻 | A-4 の最小再現で先に検証。違ったら公式 docs に従う |
| R2 | 新 fabric-loom plugin id (`net.fabricmc.fabric-loom`) が想定と違う / 別の Maven coordinates が必要 | Phase B-3 で `UnknownPluginException` | A-2/A-3 で fabric-example-mod 26.1 の plugins 宣言を直接確認 |
| R3 | 26.1 で実際に削除されている API が explore の grep で漏れて検出されている | Phase D-2 のコンパイルエラー多発 | Phase D-2 で 1 件ずつ対応 / Stonecutter `?if` で 1.21.11 を保護 |
| R4 | Mixin 31 個のうち librarian 検証が間違っているクラスがある | Phase E-1 で remap 失敗 | NeoForge javadocs 26.1 / fabric-example-mod 26.1 ソースで個別確認 |
| R5 | fabric.mod.json の JSON5 コメントが Loom 26.1 で正しく解釈されない | fabric.mod.json invalid エラー | Stonecutter docs の JSON5 用シンタックス (`//?` ベース) を厳守、Phase C-3 後に jq で構文検証 |
| R6 | CI matrix で Java 25 が actions/setup-java で取得不可 | CI fail | A-2 の時点で setup-java サポート JDK list (Temurin 25 等) を確認 |
| R7 | 1.21.11 ビルドが per-version buildscript 化で壊れる (非 regression 要件違反) | リリース系統断絶 | F-3 でベースライン diff、必ず PR マージ前に CI で検証 |
| R8 | 26.1 で `loom { runs { create(...) } }` API が変更されている (runClienttest が動かない) | clienttest 側のみ影響 | A-3 fabric-example-mod 26.1 で API 形を確認、必要なら Stonecutter `?if` で buildscript 内に分岐入れる (が、本案では物理分離なので不要) |
| R9 | fabric-permissions-api 0.7.0 で `include()` shadow パターンが変更された | Phase C-2 で実行時 ClassNotFound | A-3 で permissions-api リリースノート確認 |

---

## 5. ロールバック

`feat/stonecutter-mc26` ブランチを破棄するだけで main は無傷。途中段階のコミットは `git revert` 可能 (Phase ごとに小さくコミットする方針)。

---

## 6. Definition of Done

- [x] Phase A〜H の全タスクが完了 (チェックボックスは shipped 状態)
- [x] `./gradlew :1.21.11:build` SUCCESSFUL
- [x] `./gradlew :26.1:build` SUCCESSFUL
- [x] `./gradlew chiseledBuild` SUCCESSFUL (両 jar 生成)
- [x] `./gradlew :1.21.11:runServertest` 12/12 pass
- [x] `./gradlew :26.1:runServertest` 12/12 pass
- [x] `./gradlew :1.21.11:lintKotlin` クリーン
- [x] `./gradlew :26.1:lintKotlin` クリーン
- [x] 1.21.11 jar 内容がベースライン (`temp/baseline-1.21.11.jar`) と一致 (`injected_interfaces` 削除分は意図差分)
  - `find . -type f | sort` で一致
  - AW (`fabpose.accesswidener`), mixins (`fabpose.mixins.json`) は完全一致
  - `fabric.mod.json` は `custom.loom:injected_interfaces` ブロックのみ削除 (cast 方式へ移行のため)
- [x] CI build job が `matrix.include=[1.21.11+JDK21, 26.1+JDK25]` で両方 green (PR #24 run 25991062993)
- [ ] CI publish job が `v0.0.0-test+26.1` の dry-run タグで validate 通過 (regex 確認、本 PR では未実施)
- [x] CLAUDE.md / AGENTS.md が新コマンド体系を反映
- [ ] PR がレビュー承認され main にマージ

---

## 7. 関連項目 / 引き継ぎ

- 本計画は `feat/stonecutter-mc26` ブランチで作業。base は main = `1d443dbd` (PR #23 merge)。
- 既存ブランチ群 (1.21.10, 1.21.7, 1.21.4, 1.21.1, 1.21, 1.20.6, 1.20.1-bp, 1.20.1-unsafe) は触らない。legacy グループ計画は別途作成予定 (未作成)。
- 本 PR マージ後に検討する別 PR 候補:
  - 既存 mod 独自 MannequinEntity を 26.1 バニラ Mannequin に置き換え
  - Java 25 への toolchain 統一 (1.21.11 も 25 で動くなら matrix.java を統一できる)
  - 26.2 / それ以降への対応

---

## 8. 参照

- PR #23 (Stonecutter modern intro): https://github.com/YukkuriLaboratory/FabPose/pull/23
- Fabric 26.1 announcement: https://fabricmc.net/2026/03/14/261.html
- Fabric 26.1 porting guide: https://docs.fabricmc.net/26.1/develop/porting/fabric-api
- Stonecutter buildscript docs: https://stonecutter.kikugie.dev/wiki/start/builds
- 関連コミット (本計画作成時点): TBD (実装時にコミットハッシュを追記)
