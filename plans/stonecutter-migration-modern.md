# Stonecutter 導入計画 (modern グループ: 1.21.11+)

> 本計画は **main ブランチ (1.21.11 / Mojang Mappings / MannequinEntity ベース)** を
> Stonecutter 化し、今後 1.21.12 / 1.22 等の新バージョンを **同一コードベース** で
> 同時管理できる体制を整えることを目的とする。
>
> **対象外** (別計画書 `stonecutter-migration-legacy.md` で扱う予定):
> - 1.20.1 〜 1.21.10 (Yarn + 旧 PosingEntity システム)
> - 既存のバージョン別ブランチ (`1.21.10`, `1.21.7`, ...) は **本計画では一切触らない**

---

## 0. ゴール / 非ゴール

### ゴール (この計画書のスコープ)
1. main ブランチ上で Stonecutter 0.9.x を導入し、`./gradlew chiseledBuild` で
   1.21.11 のビルドが通る状態にする。
2. **将来 1.21.12 / 1.22 が出た際に `versions/1.21.12/gradle.properties` を 1 つ
   追加するだけで対応着手できる** ディレクトリ構造 / build スクリプト構造を整える。
3. `runServer`, `runClient`, `runServertest`, `runClienttest` の各 Loom run 構成を
   Stonecutter 配下でも動作させる。
4. 既存の Xvfb 自動起動ロジックを失わない。

### 非ゴール
- 1.21.12 以降への実バージョン追加 (構造だけ用意し、追加自体は別タスク)。
- 1.21.10 以下の取り込み (legacy グループの計画書で扱う)。
- リファクタ・命名整理・追加機能。**プレ/ポストの差分は最小** に保つ。
- 既存ブランチの整理 / 削除。

---

## 1. 前提と制約

### 1.1 検証済み事実 (調査結果)
- Stonecutter Plugin: `dev.kikugie.stonecutter` v0.9.3 (Maven: `https://maven.kikugie.dev/releases`)
- Gradle 9.0+ 推奨。本リポジトリは Gradle Wrapper を使用 (要バージョン確認)。
- Loom 連携で AccessWidener はプリプロセッサを通すため
  `loom.accessWidenerPath = stonecutter.current.process(awFile, "build/processed.aw")`
  パターンが必要。
- 推奨レイアウト: ルートに共有 `src/`, `build.gradle.kts`, `versions/<mc>/gradle.properties`,
  自動生成される `stonecutter.gradle.kts` (controller)。

### 1.2 リポジトリ固有の制約
- **マッピング**: `loom.officialMojangMappings()` を使用 (1.21.11 で導入済)。
  本計画では **Mojmap を継続**。
- **AccessWidener**: `src/main/resources/fabpose.accesswidener` (1 ファイルのみ)。
- **Mixin**: `src/main/resources/fabpose.mixins.json` で集中登録。
- **fabric.mod.json**: `processResources` で `${version}` 等を `expand()` 注入している。
  Stonecutter の JSON5 プリプロセッサと共存させる必要がある。
- **`custom.loom:injected_interfaces`**: `class_1657` 等の **intermediary 名** を直接指定。
  これは Loom の仕様で Mojmap 利用時もそのまま動作するため変更不要。
- **ソースセット**: `main`, `clienttest`, `servertest` の 3 つ。
- **依存**: Fabric API のサブモジュール 9 個を個別 include + `fabric-api` 全体を runtime。

### 1.3 1 バージョンしか持たない初期状態の Stonecutter 化
今回の起点は **1.21.11 単体**。Stonecutter は 1 バージョンでも完全に動作する
(`versions("1.21.11")` だけで OK)。プリプロセッサ条件分岐 (`//? if >=1.22 {`) は
**将来の追加に備えてコード内には入れない** (現時点で枝が無い)。

---

## 2. 最終ディレクトリ構造 (target state)

```
FabPose/
├── settings.gradle.kts          # ★ 大幅書き換え (Stonecutter plugin + create node)
├── build.gradle.kts             # ★ 中規模書き換え (centralScript として全 version 共有)
├── stonecutter.gradle.kts       # ★ 新規 (controller, chiseledBuild 登録)
├── gradle.properties            # ★ 縮小 (グローバル設定のみ。MC固有は versions/ へ)
├── versions/
│   └── 1.21.11/
│       └── gradle.properties    # ★ 新規 (minecraft_version / loader_version / fabric_version / flk_version)
├── src/                         # ← 既存のまま (中身は触らない)
│   ├── main/
│   ├── clienttest/
│   └── servertest/
├── plans/
│   └── stonecutter-migration-modern.md   # 本ファイル
└── ... (既存ファイル)
```

### 2.1 重要: `src/` 配下は本計画では一切編集しない
1.21.11 単独構成では条件分岐が不要。Kotlin/Java/Mixin/AccessWidener 全てそのまま。
将来 1.22 を追加する際に初めて `//? if >=1.22 {` を入れる。

---

## 3. 詳細手順 (チェックリスト + QA シナリオ)

実装フェーズではこのリストを TodoWrite に転記して順に消化する。
各タスクに **QA** ブロック (使用ツール / 実行コマンド / 期待結果) を必ず付ける。

### Phase A: 下調べ・準備 (READ ONLY)

- [ ] **A-1** `gradle/wrapper/gradle-wrapper.properties` を確認し、Gradle 9.0+ か検証。
  9.0 未満なら `./gradlew wrapper --gradle-version 9.0.2` で更新する手順を Phase B-0
  として追加する。
  - **QA 使用ツール**: `read` (gradle-wrapper.properties)
  - **手順**: `read /home/turtton/Documents/YukuLab/mods/FabPose/gradle/wrapper/gradle-wrapper.properties`
  - **期待結果**: `distributionUrl=...gradle-9.x.y-...zip` であること。
    9.0 未満なら本ファイルに「Phase B-0: wrapper 更新」サブタスクを追記してから先へ進む。

- [ ] **A-2** Loom 1.14-SNAPSHOT が Stonecutter 0.9.3 と互換か実例で確認。
  - **QA 使用ツール**: `webfetch` または `librarian`
  - **手順**:
    1. `webfetch https://github.com/isXander/CullLessLeaves/blob/trunk/build.gradle.kts` で
       Loom と Stonecutter の併用バージョンを確認。
    2. Stonecutter v0.9.x リリースノート (`https://stonecutter.kikugie.dev/wiki/changelog`)
       を確認。
  - **期待結果**: 1.14-SNAPSHOT もしくは近接マイナーが動作実績ありと判明。
    互換性に懸念がある場合は本計画の §4 リスク表 R1 を更新し、Loom を 1.13 系へ
    フォールバックする差分タスクを追加してから次フェーズへ。

- [ ] **A-3** main 上で **Stonecutter 化用の作業ブランチ** `feat/stonecutter-modern`
  を切る (push しない)。
  - **QA 使用ツール**: `bash` (git)
  - **手順**: `git checkout -b feat/stonecutter-modern && git status`
  - **期待結果**: `On branch feat/stonecutter-modern` / `nothing to commit, working tree clean`。

### Phase B: settings / controller / version property の整備

- [ ] **B-1** `settings.gradle.kts` を以下に書き換え:
  ```kts
  pluginManagement {
      repositories {
          maven("https://maven.fabricmc.net/")
          maven("https://maven.kikugie.dev/releases")
          mavenCentral()
          gradlePluginPortal()
      }
  }

  plugins {
      id("dev.kikugie.stonecutter") version "0.9.3"
  }

  stonecutter {
      kotlinController = true
      centralScript = "build.gradle.kts"

      create(rootProject) {
          versions("1.21.11")
          vcsVersion = "1.21.11"
      }
  }

  rootProject.name = "FabPose"
  ```
  - **QA 使用ツール**: `bash` (gradle), `read`
  - **手順**:
    1. `read settings.gradle.kts` で内容が上記スニペットと一致することを確認。
    2. `./gradlew help -q` でビルドスクリプト評価エラーが出ないことを確認。
  - **期待結果**: `read` 結果が完全一致。`./gradlew help` の終了コードが 0 で
    `BUILD SUCCESSFUL` を含む。

- [ ] **B-2** `versions/1.21.11/gradle.properties` を新規作成:
  ```properties
  minecraft_version=1.21.11
  loader_version=0.18.3
  fabric_version=0.140.2+1.21.11
  flk_version=1.13.8+kotlin.2.3.0
  ```
  - **QA 使用ツール**: `read`, `bash`
  - **手順**:
    1. `read versions/1.21.11/gradle.properties` で 4 行のプロパティを確認。
    2. `./gradlew :1.21.11:properties -q | grep -E '^(minecraft_version|loader_version|fabric_version|flk_version):'`
  - **期待結果**: 4 つのプロパティが `versions/1.21.11/gradle.properties` の値と一致して
    出力される。

- [ ] **B-3** ルート `gradle.properties` から MC 固有プロパティ
  (`minecraft_version`, `loader_version`, `fabric_version`, `flk_version`) を削除し、
  共通設定 (`org.gradle.jvmargs`, `maven_group`, `archives_base_name`) のみ残す。
  - **QA 使用ツール**: `bash` (grep)
  - **手順**: `grep -E '^(minecraft_version|loader_version|fabric_version|flk_version)=' gradle.properties; echo "exit=$?"`
  - **期待結果**: 標準出力に何も出ず `exit=1` (grep ヒット 0)。
    `grep -E '^(maven_group|archives_base_name|org.gradle.jvmargs)' gradle.properties`
    は 3 行ヒットすること。

- [ ] **B-4** Stonecutter 同期を実行し、`stonecutter.gradle.kts` (controller) が
  自動生成されることを確認。
  - **QA 使用ツール**: `bash` (gradle, ls)
  - **手順**:
    1. `./gradlew tasks --all -q | grep -i stonecutter` で Stonecutter タスクが登録
       されていることを確認。
    2. controller が未生成なら IDE Gradle Sync 相当として `./gradlew help -q` を再実行
       (Stonecutter は settings.gradle.kts 評価時に controller を生成する)。
    3. `ls stonecutter.gradle.kts` で controller ファイルが存在することを確認。
    4. `./gradlew projects -q | grep ':1.21.11'` で `:1.21.11` サブプロジェクトが
       Gradle に認識されていることを確認。
  - **期待結果**:
    - 手順 1: `chiseled*` 系または `stonecutter*` 系タスクが 1 件以上ヒット。
    - 手順 3: `stonecutter.gradle.kts` の `ls` 出力 (エラーなし)。
    - 手順 4: `Project ':1.21.11'` を含む行が出力。
  - **注**: Stonecutter は shared buildscript 方式 (centralScript) のため
    `versions/1.21.11/build.gradle.kts` は **生成されない**。検証は controller 存在 +
    Gradle のサブプロジェクト認識で行う。

- [ ] **B-5** controller `stonecutter.gradle.kts` に chiseled タスクを登録:
  ```kts
  plugins { id("dev.kikugie.stonecutter") }

  stonecutter active "1.21.11"

  tasks.register("chiseledBuild") {
      group = "project"
      dependsOn(stonecutter.tasks.named("build"))
  }
  ```
  > **注**: Stonecutter 0.9.x で chiseled タスク登録 API は `stonecutter registerChiseled ... ofTask(...)` から
  > `tasks.register { dependsOn(stonecutter.tasks.named("...")) }` 方式に変更されている。
  - **QA 使用ツール**: `bash` (gradle)
  - **手順**: `./gradlew tasks --group project -q | grep chiseledBuild`
  - **期待結果**: `chiseledBuild` の行が 1 件出力される。

### Phase C: build.gradle.kts の Stonecutter 対応
> **方針**: 既存ロジックを **削らず** に、プロパティ参照を `property("...")` 経由に
> 寄せ、AccessWidener パスだけ Stonecutter 経由にする最小改修。

- [ ] **C-1** `build.gradle.kts` 冒頭の plugin ブロックは **そのまま** (Stonecutter
  plugin はサブプロジェクトに自動付与されるため `build.gradle.kts` 側に追記不要)。
  - **QA 使用ツール**: `read`
  - **手順**: `read build.gradle.kts` の 1〜10 行目で plugin ブロックを確認。
  - **期待結果**: `id("dev.kikugie.stonecutter")` 行が **追加されていない** こと。

- [ ] **C-2** プロパティ取得 (`val minecraftVersion = project.property("minecraft_version").toString()`)
  はそのまま機能する (versions/ から自動継承)。
  - **QA 使用ツール**: `bash` (gradle)
  - **手順**: `./gradlew :1.21.11:dependencies --configuration minecraftLibraries -q | head -5`
  - **期待結果**: `com.mojang:minecraft:1.21.11` を含む依存関係ツリーが出力される。

- [ ] **C-3** `loom { accessWidenerPath.set(...) }` を以下に変更:
  ```kts
  loom {
      accessWidenerPath.set(rootProject.file("src/main/resources/fabpose.accesswidener"))
      runtimeOnlyLog4j.set(true)
      // runs { ... } はそのまま
  }
  ```
  > **注**: 1.21.11 単独構成では AW に条件分岐コメントを入れる必要がないため、
  > `stonecutter.current.process()` を経由せず `rootProject.file()` を直接渡す最小構成とする。
  > 将来複数バージョンを Stonecutter 化したタイミングで AW 内に条件分岐が必要になった場合は
  > `stonecutter.current.process(awSrc, "build/processed.accesswidener")` 経由に切替える。
  - **QA 使用ツール**: `bash` (gradle, diff)
  - **手順**:
    1. `./gradlew :1.21.11:processResources -q`
    2. `diff src/main/resources/fabpose.accesswidener versions/1.21.11/build/resources/main/fabpose.accesswidener`
  - **期待結果**:
    - 手順 1: BUILD SUCCESSFUL。
    - 手順 2: AW がパススルーで一致 (diff 出力 0 行)。

- [ ] **C-4** `tasks.processResources { filesMatching("fabric.mod.json") { expand(...) } }`
  はそのまま使用。
  - **QA 使用ツール**: `bash` (gradle, jq), `read`
  - **手順**:
    1. `./gradlew :1.21.11:processResources -q`
    2. `, jq '.depends.minecraft' versions/1.21.11/build/resources/main/fabric.mod.json`
  - **期待結果**: 出力が `">=1.21.11"` (versions プロパティ展開済)。

- [ ] **C-5** Xvfb 自動起動ロジックの動作検証 (サブプロジェクトに登録された
  `runClienttest` task で `finalizedBy(cleanupXvfb)` が実際に配線されているか)。
  - **前提変更**: 現行 `build.gradle.kts` の `cleanupXvfb` には group 指定がないため
    `tasks --group fabric` には現れない。本フェーズで `tasks.register("cleanupXvfb")`
    の定義に `group = "verification"` を追加する (理由: Xvfb クリーンアップは検証系
    補助タスクであり、`fabric` group は Loom 専用のため使わない)。
  - **QA 使用ツール**: `bash` (gradle)
  - **手順**:
    1. `./gradlew :1.21.11:tasks --all -q | grep -E '^(runClienttest|cleanupXvfb)\\b'`
       で両タスクの存在を確認。
    2. `./gradlew :1.21.11:runClienttest --dry-run` を実行し、ログ末尾の
       実行予定タスクリストに `:1.21.11:cleanupXvfb` が含まれることを確認
       (finalizer は dry-run のスケジュールにも現れる)。
  - **期待結果**:
    - 手順 1: `runClienttest` と `cleanupXvfb` が 1 行ずつヒット。
    - 手順 2: 出力に `:1.21.11:cleanupXvfb SKIPPED` (または同等の dry-run 表記) が含まれる。
  - **失敗時の対処**: dry-run に cleanupXvfb が出ない場合、`runClienttest` 取得ロジックを
    `subprojects { ... }` で囲む or controller 側へ移す対応を C-5b として追加。

### Phase D: ビルド検証
- [ ] **D-1** `./gradlew :1.21.11:build` を実行し、ビルド成功を確認。
  - **QA 手順**: `./gradlew :1.21.11:build`
  - **期待結果**: exit 0 / `BUILD SUCCESSFUL` / `versions/1.21.11/build/libs/fabpose-*.jar` が生成。

- [ ] **D-2** `./gradlew chiseledBuild` を実行し、jar が生成されることを確認。
  - **QA 手順**: `./gradlew chiseledBuild && ls versions/1.21.11/build/libs/`
  - **期待結果**: `fabpose-*+1.21.11.jar` が出力。

- [ ] **D-3** `./gradlew :1.21.11:runServertest` を実行し、既存 GameTest が pass。
  - **QA 手順**: `./gradlew :1.21.11:runServertest`
  - **期待結果**: exit 0、`versions/1.21.11/build/servertest/junit.xml` の `<failure>` 要素が 0。

- [ ] **D-4** `./gradlew :1.21.11:runClienttest` を実行し、Xvfb 起動含めて pass。
  - **QA 手順**: `./gradlew :1.21.11:runClienttest`
  - **期待結果**: exit 0、ログに `Started Xvfb on display :NN` が出現、
    `versions/1.21.11/build/clienttest/junit.xml` の `<failure>` 要素が 0。

- [ ] **D-5** lintKotlin / formatKotlin が動作することを確認。
  - **QA 手順**: `./gradlew :1.21.11:lintKotlin`
  - **期待結果**: exit 0。

- [ ] **D-6** publish タスクが動作することを確認。
  - **QA 手順**: `./gradlew :1.21.11:publishToMavenLocal && ls ~/.m2/repository/net/yukulab/fabpose/`
  - **期待結果**: exit 0。`~/.m2/repository/net/yukulab/fabpose/<version>/` に jar/pom が配置。

### Phase E: CI / リリースワークフロー対応
- [ ] **E-1** `.github/workflows/build.yml` の matrix を Stonecutter サブプロジェクト指定に書き換え。
  - **方針**: タスク名 (`build` / `runServertest` / `runClienttest`) と MC バージョンを
    matrix 2軸 (`version × task`) で展開し、各ジョブは
    `./gradlew :${{ matrix.version }}:${{ matrix.task }}` を実行する。
    `chiseledBuild` 置換ではなく per-version 実行とすることで、将来 MC バージョンを
    追加した際に CI 行列を `version` 配列に追記するだけで拡張できる。
  - **QA 使用ツール**: `bash` (grep), `read`
  - **手順**:
    1. `grep -rn 'gradlew' .github/workflows/`
    2. matrix 形式に書き換え (`version: ['1.21.11']`, `task: ['build', 'runServertest', 'runClienttest']`)。
    3. `grep -rnE './gradlew (build|runServertest|runClienttest)\b' .github/workflows/`
  - **期待結果**: 手順 3 で `:<version>:` プレフィックスなしの直接呼び出しが 0 件。

- [ ] **E-2** リリースワークフロー (タグ → publish) の jar パスを修正。
  - **方針**: タグ形式 `v<modver>+<mcver>` から `steps.tag.outputs._0` (modver) と
    `steps.tag.outputs._1` (mcver) を取り出し、ビルドは
    `./gradlew :${{ steps.tag.outputs._1 }}:build`、jar パスは
    `versions/${{ steps.tag.outputs._1 }}/build/libs/fabpose-${{ steps.tag.outputs._0 }}+${{ steps.tag.outputs._1 }}.jar`
    に書き換える。
  - **QA 使用ツール**: `bash` (grep)
  - **手順**:
    1. `grep -rn 'build/libs' .github/workflows/`
    2. ヒットした行を `versions/<mc>/build/libs/` に書き換え。
    3. `grep -rnE '(^|[^/])build/libs' .github/workflows/` で旧パス残存をチェック。
  - **期待結果**: 手順 3 でルート `build/libs` への参照が 0 件
    (`versions/<mc>/build/libs` のみヒット)。

- [ ] **E-3** Gradle キャッシュキーを Stonecutter 構成に合わせて更新。
  - **QA 使用ツール**: `bash` (grep)
  - **手順**: `grep -rn 'gradle/wrapper\|gradle.properties' .github/workflows/`
  - **期待結果**: キャッシュ key 計算に使われるファイル一覧に
    `versions/**/gradle.properties` が含まれている (含まれていなければ追記)。

### Phase F: ドキュメント更新
- [ ] **F-1** `AGENTS.md` / `CLAUDE.md` の "Build and Run" セクションを更新:
  - 旧 `./gradlew build` → 新 `./gradlew chiseledBuild` または `./gradlew :1.21.11:build`
  - 旧 `./gradlew runServer` → 新 `./gradlew :1.21.11:runServer`
  - アクティブバージョン切り替え方法 (controller の `stonecutter active "X"` を編集 or
    Stonecutter 公式ドキュメントの推奨手順) を明記。
  - **QA 使用ツール**: `bash` (grep)
  - **手順**:
    1. `grep -nE './gradlew (build|runServer|runClient|runServertest|runClienttest)\\b' AGENTS.md CLAUDE.md`
    2. ヒット行を新コマンドへ置換。
    3. 手順 1 を再実行。
  - **期待結果**: 手順 3 でヒット 0。
    `grep -n 'chiseledBuild\|:1.21.11:' AGENTS.md CLAUDE.md` で 1 件以上ヒット。

- [ ] **F-2** 本計画書 (`plans/stonecutter-migration-modern.md`) のチェックリストを
  完了状態に更新し、PR の参照リンクを追記。
  - **QA 使用ツール**: `bash` (grep)
  - **手順**: `grep -c '^- \\[ \\]' plans/stonecutter-migration-modern.md`
  - **期待結果**: 出力が `0` (未完了タスクなし)。
    PR URL が計画書末尾に追記されていること (`grep -n 'github.com.*pull' plans/stonecutter-migration-modern.md` で 1 件以上)。

---

## 4. 既知のリスク / 注意点

| # | リスク | 対策 |
|---|--------|------|
| R1 | Loom 1.14-SNAPSHOT × Stonecutter 0.9.3 の組合せ未検証 | Phase A-2 で実例確認。問題があれば Loom を 1.13 安定版へ落とす案を検討 |
| R2 | Gradle 9 未満だと Stonecutter が動かない | Phase A-1 で確認、必要なら wrapper 更新を Phase B-0 として追加 |
| R3 | `runClienttest` の Xvfb 起動 task が `tasks.named` で取得失敗 | Phase C-5 で検証、`afterEvaluate` でラップする等の対応 |
| R4 | `processResources` の `expand` が Stonecutter プリプロセッサと衝突 | 1.21.11 単独では条件コメントが無いので衝突しない。将来 `fabric.mod.json` に Stonecutter コメントを入れる際に再検討 |
| R5 | `injected_interfaces` の intermediary 名 (`class_1657`) が将来バージョンで変わる | 1.21.11 では問題なし。バージョン追加時に確認項目として追記 |
| R6 | publish の jar パス変更で外部リリース URL が壊れる | Phase E-2 で対応。リリース済 v0.x の URL は不変 (タグ済み artifact のため) |

---

## 5. ロールバック手順

Phase D で致命的な問題が発生した場合:
1. 作業ブランチ `feat/stonecutter-modern` を破棄。
2. main は無傷のため即座に元の状態に戻る。
3. Issue に失敗原因を記録し、計画書を改訂してから再着手。

---

## 6. 完了の定義 (Definition of Done)

- [ ] Phase A〜F の全チェックボックスが完了
- [ ] `./gradlew chiseledBuild` がローカル / CI 両方で成功
  - **QA 手順**: ローカルは `./gradlew chiseledBuild`、CI は GitHub Actions の
    最新 run が緑であること。
  - **期待結果**: いずれも exit 0。
- [ ] `./gradlew :1.21.11:runServertest` / `:runClienttest` がローカルで成功
  - **QA 手順**: 上記 D-3 / D-4 と同じ。
- [ ] 生成された jar の中身が Stonecutter 化前と一致
  - **QA 使用ツール**: `bash` (jar, jq, sha256sum)
  - **比較対象 (Stonecutter 化前)**: `feat/stonecutter-modern` 作業前の main で
    `./gradlew build` を実行して得た `build/libs/fabpose-*.jar` を
    `temp/baseline-fabpose.jar` として保存しておく。
  - **手順**:
    1. `jar tf temp/baseline-fabpose.jar | sort > /tmp/baseline.list`
    2. `jar tf versions/1.21.11/build/libs/fabpose-*.jar | sort > /tmp/new.list`
    3. `diff /tmp/baseline.list /tmp/new.list`
    4. `unzip -p temp/baseline-fabpose.jar fabric.mod.json | jq -S . > /tmp/baseline.modjson`
    5. `unzip -p versions/1.21.11/build/libs/fabpose-*.jar fabric.mod.json | jq -S . > /tmp/new.modjson`
    6. `diff /tmp/baseline.modjson /tmp/new.modjson`
    7. `unzip -p temp/baseline-fabpose.jar fabpose.mixins.json | jq -S . > /tmp/baseline.mixins`
    8. `unzip -p versions/1.21.11/build/libs/fabpose-*.jar fabpose.mixins.json | jq -S . > /tmp/new.mixins`
    9. `diff /tmp/baseline.mixins /tmp/new.mixins`
  - **期待結果**:
    - 手順 3: エントリ集合差分 0 (Stonecutter が追加するメタファイル
      `stonecutter.json` 等が出る場合は許容し、本欄に明記)。
    - 手順 6: `version` フィールド以外で差分 0。
    - 手順 9: 差分 0。
- [ ] PR がレビュー承認され main にマージ
- [ ] AGENTS.md / CLAUDE.md が新コマンド体系を反映 (F-1 の QA を再実行して 0 ヒット)

---

## 7. 次計画への引き継ぎ事項 (legacy グループ向け)

本計画完了後、別途作成予定の legacy グループ計画書 (`plans/stonecutter-migration-legacy.md`,
未作成) で扱う想定:
- 新ブランチ `legacy/1.21` を切る (`1.21.10` ブランチを起点)
- Yarn mappings 統一 (or 各バージョンで `yarn_mappings` プロパティ管理)
- 旧 PosingEntity システムを正とする
- 対象バージョン: 1.20.1, 1.21, 1.21.1, 1.21.4, 1.21.7, 1.21.10
- ブランチ間の差分 (Yarn class 名変更等) を Stonecutter `?if` で吸収する範囲の精査
- 既存ブランチの削除是非
