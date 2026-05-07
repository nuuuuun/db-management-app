# 不具合・セキュリティ監査レポート

作成日: 2026-05-05

対象フォルダ: `D:\プログラミング\git\db-management-app`

## 結論

問題は検知されました。特に、再起動時のデータ消失リスク、平文シークレット、権限昇格、汎用テーブル編集APIによる過剰権限が重大です。この状態のまま本番利用するのは危険です。

## 確認したこと

- バックエンド: Spring Boot / PostgreSQL / JWT認証
- フロントエンド: Vue / Vite / Axios
- 設定ファイル、認証、認可、CSV取込、CSV/Excel出力、DB初期化処理を中心に確認
- `npm run build` は成功
- `npm audit --audit-level=moderate` は `0 vulnerabilities`
- バックエンドテストは Maven Wrapper の不具合により未実行

## 重大な問題

### 1. 本番でもDB初期化SQLが毎回実行され、データ消失の恐れがある

`application-prod.properties` でも `spring.sql.init.mode=always` が設定されています。一方で `schema.sql` には `DROP TABLE IF EXISTS ... CASCADE` が含まれているため、アプリ再起動や再デプロイ時に業務データが削除される可能性があります。

該当箇所:

- `db-management/src/main/resources/application-prod.properties`
- `db-management/src/main/resources/schema.sql`

推奨対応:

- 本番では `spring.sql.init.mode=never` にする
- `schema.sql` から本番データを破壊する `DROP TABLE` を排除する
- Flyway / Liquibase などのマイグレーション管理へ移行する

### 2. シークレットや初期パスワードが平文で保存されている

DBパスワード、JWT秘密鍵、初期ユーザーパスワードがリポジトリ内に平文で存在します。また `RDS masterpass.txt` という秘匿情報と思われるファイルも存在します。

該当箇所:

- `docker-compose.yml`
- `db-management/src/main/resources/application.properties`
- `RDS masterpass.txt`

推奨対応:

- 漏洩済み前提でDBパスワード、JWT秘密鍵、RDSパスワードをローテーションする
- シークレットをリポジトリから削除する
- `.env`、AWS Secrets Manager、SSM Parameter Store などへ移す
- Git履歴に含まれている場合は履歴からの削除も検討する

### 3. EDITORがADMINへ権限昇格できる可能性がある

`UserController` では `VIEWER` 以外ならユーザー作成・更新が可能です。更新時に `role` をリクエスト値のまま保存しているため、`EDITOR` が自分や他ユーザーを `ADMIN` に変更できる可能性があります。

また、ユーザー作成時にパスワードをBCryptでハッシュ化していません。

該当箇所:

- `db-management/src/main/java/com/example/db_management/UserController.java`

推奨対応:

- ユーザー管理APIは `ADMIN` のみに制限する
- `role` 変更は明示的にADMINだけ許可する
- ユーザー作成時は必ずパスワードをハッシュ化する
- 入力DTOを作り、Entityを直接リクエストに使わない

### 4. 汎用テーブル編集APIが業務テーブル以外にも作用する可能性がある

`TableController` はDBメタデータから取得したテーブルを編集対象にしています。そのため、`users` や `masked_columns`、`operation_history` など、本来専用制御が必要なテーブルまで編集対象になり得ます。

該当箇所:

- `db-management/src/main/java/com/example/db_management/TableController.java`

推奨対応:

- 編集可能テーブルを業務テーブルの allowlist に限定する
- `users`、`masked_columns`、`operation_history` などを明示的に除外する
- 操作ごとに `ADMIN` / `EDITOR` の権限を分離する

## 高リスクの問題

### 5. CSV一括エクスポートで全テーブル情報が漏れる可能性がある

CSV一括エクスポートは `VIEWER` 以外に許可されていますが、取得対象が全テーブルで、マスク処理もありません。`users` テーブルが含まれた場合、JDBCで取得しているため `@JsonIgnore` は効きません。

該当箇所:

- `db-management/src/main/java/com/example/db_management/CsvBulkController.java`

推奨対応:

- エクスポート対象を業務テーブル allowlist に限定する
- `users` などの管理テーブルを除外する
- マスク設定をCSV出力にも適用する
- 必要に応じて一括エクスポートは `ADMIN` のみに制限する

### 6. CSV上書き取込が即時全件削除を行う

CSV取込の `overwrite` モードでは `DELETE FROM table` を直接実行しています。確認、バックアップ、トランザクション制御、ロールバック方針が見えないため、誤操作や途中失敗時のデータ損失リスクが高いです。

該当箇所:

- `db-management/src/main/java/com/example/db_management/CsvImportController.java`
- `db-management/src/main/java/com/example/db_management/CsvBulkController.java`

推奨対応:

- `overwrite` は `ADMIN` のみに制限する
- 事前検証を全件通してから削除・投入する
- トランザクション内で実行する
- バックアップまたは復元手段を用意する

### 7. DB内部エラーをレスポンスに返している

CSV取込時の例外メッセージをそのままレスポンスに含めています。制約名、SQL、内部構造などが利用者に漏れる可能性があります。

該当箇所:

- `db-management/src/main/java/com/example/db_management/CsvImportController.java`
- `db-management/src/main/java/com/example/db_management/CsvBulkController.java`

推奨対応:

- 利用者向けには汎用エラーメッセージを返す
- 詳細エラーはサーバーログのみに記録する
- エラーコードや行番号だけをレスポンスに含める

## 中リスク・品質上の問題

### 8. 操作履歴の記録失敗を握りつぶしている

`OperationHistoryService` で例外を無視しているため、監査ログが欠落しても検知できません。

該当箇所:

- `db-management/src/main/java/com/example/db_management/OperationHistoryService.java`

推奨対応:

- 例外をログ出力する
- 重要操作では履歴記録失敗時に処理全体を失敗させるか、少なくとも監視対象にする

### 9. 本番設定ファイルに文字化けがある

`application-prod.properties` に文字化けがあり、`spring.datasource.url=${DB_URL}` がコメント行に埋もれているように見えます。意図した本番設定が読み込まれない可能性があります。

該当箇所:

- `db-management/src/main/resources/application-prod.properties`

推奨対応:

- UTF-8で保存し直す
- 設定キーが正しく独立した行になっていることを確認する
- 本番起動時の実効設定をログまたはテストで確認する

### 10. Maven Wrapper がこの環境で起動できない

`mvnw.cmd test` 実行時に `Cannot index into a null array` が発生し、バックエンドテストを実行できませんでした。原因は `mvnw.cmd` 内の `.Target[0]` 参照付近です。

該当箇所:

- `db-management/mvnw.cmd`

推奨対応:

- Maven Wrapper を再生成する
- もしくは該当箇所をPowerShell環境で安全に動く形へ修正する
- CI上で `mvn test` が実行できる状態にする

## 追加で気になった点

- JWTを `localStorage` に保存しているため、XSSが発生した場合にトークンを盗まれやすいです。
- CSRFは無効化されています。JWTヘッダー方式なら一般的には成立しにくいですが、将来Cookie認証へ変える場合は再検討が必要です。
- `h2-console` が permitAll になっています。現状PostgreSQL構成ですが、本番では不要な許可なので削除が望ましいです。
- `pom.xml` が Spring Boot `3.5.15-SNAPSHOT` と snapshot repository を使っています。本番用途では安定版へ固定するのが望ましいです。

## 検証結果

### 成功

```text
frontend: npm run build
結果: 成功
```

```text
frontend: npm audit --audit-level=moderate
結果: found 0 vulnerabilities
```

### 未実行

```text
backend: mvnw.cmd test
結果: Maven Wrapper の起動エラーにより未実行
```

エラー概要:

```text
Cannot index into a null array.
Cannot start maven from wrapper
```

## 開発部・開発統括部 見解

> 本表は改修着手前の内部評価として作成。運用環境が**社内LANのみ**（インターネット非公開）であることを前提に、各問題のリスク評価を行っている。

| # | 問題概要 | 深刻度 | 開発部 見解 | 開発統括部 見解 | 改修必要有無 |
|---|---------|--------|------------|----------------|------------|
| 1 | 本番でDB初期化SQLが毎回実行・データ消失リスク | 🔴 重大 | `schema.sql` の `DROP TABLE IF EXISTS` を確認済み。現状の docker compose 構成では Volume 永続化があるため即時消失は起きないが、`application-prod.properties` の `spring.sql.init.mode=always` は本番運用前に必ず変更が必要。Flyway 移行は工数大だが、まず `never` に変更＋`DROP` 除去だけでリスク大幅低減できる。 | 本番稼働前の最優先事項。設定変更と `schema.sql` の `DROP` 削除は工数が小さく効果が大きいため即時対応を承認する。Flyway 移行は次フェーズで検討。 | ✅ **必須** |
| 2 | シークレット・初期パスワードが平文でリポジトリに存在 | 🔴 重大 | `docker-compose.yml` のデフォルト値はローカル開発用として許容範囲内だが、`application.properties` の JWT_SECRET デフォルト値がリポジトリに含まれる点は問題。社内 LAN 運用のみなら `.env` ファイルに切り出す対応で十分。Git 履歴への混入も確認が必要。 | 社内 LAN 限定のため深刻度はやや低下するが、パスワードと秘密鍵を Git 管理外に置く運用ルールは必ず整備する。`RDS masterpass.txt` が Git 追跡対象であれば即削除。 | ✅ **必須**（運用ルール策定含む） |
| 3 | EDITOR がADMINへ権限昇格できる | 🔴 重大 | `UserController.java` を確認。`VIEWER` 判定のみで ADMIN/EDITOR を区別していないため、EDITOR が `role: "ADMIN"` を含む PUT リクエストを送れば昇格可能な状態。また新規ユーザー作成時のパスワード BCrypt ハッシュ化が未実装であることも確認済み。いずれも修正工数は小さい。 | ユーザー管理は ADMIN 専用とすべきで、権限昇格は設計上あってはならない。工数が小さく優先度が高いため先行対応を承認する。パスワードのハッシュ化漏れも同時に修正。 | ✅ **必須** |
| 4 | 汎用テーブル編集 API が `users` 等の管理テーブルにも作用 | 🔴 重大 | `TableController.java` は DB メタデータから全テーブルを取得しており、`users`・`masked_columns`・`operation_history` も編集対象になり得る。allowlist の追加は数行の修正で対応可能。 | `users` テーブルへの直接編集が可能な状態は権限設計の根幹を崩す。allowlist 追加は即時対応を承認。業務テーブル以外を全て除外する方針で実装する。 | ✅ **必須** |
| 5 | CSV 一括エクスポートで全テーブル（`users` 含む）が出力される | 🟠 高 | `CsvBulkController.java` を確認。`ExcelExportController` と異なりマスク処理が未実装で、`users` テーブルも出力対象になる。allowlist 追加とマスク処理の適用が必要。 | 社内利用でもパスワードハッシュや権限情報の漏洩は問題。allowlist 追加を必須、マスク適用を推奨として対応を承認。 | ✅ **必須** |
| 6 | CSV 上書き取込が即時全件削除（トランザクション未使用） | 🟠 高 | `CsvImportController.java` にて `DELETE FROM table` 後に INSERT している。トランザクション未使用のため途中失敗でテーブルが空になるリスクがある。`@Transactional` 付与と事前バリデーションの追加で大幅改善可能。 | 業務データの誤消去は致命的。`@Transactional` 追加は即時対応を承認。事前確認 UI については次スプリントで対応。 | ✅ **必須** |
| 7 | DB 内部エラー（SQL・制約名）をレスポンスに返している | 🟠 高 | `CsvImportController.java` にて `e.getMessage()` をそのままレスポンスに返している。社内 LAN 限定のため外部への情報漏洩リスクは低いが、設計として好ましくない。汎用メッセージに差し替えてサーバーログに詳細を出力する対応を推奨。 | 社内利用のため緊急度は低い。ただし詳細エラーをログに残す改修は品質向上にもつながるため推奨対応として承認。他の改修と同時実施が効率的。 | 🔶 **推奨** |
| 8 | 操作履歴の記録失敗を握りつぶしている | 🟡 中 | `OperationHistoryService.java` にて例外を無視している。監査ログが欠落しても気づけない状態。`log.error()` 追加は1行の対応。重要操作では履歴記録失敗時に処理全体を失敗させるかは業務要件次第。 | 監査証跡の信頼性に関わる。最低限ログ出力は必ず追加する。処理全体を失敗させるかは運用担当者と要件確認の上で決定する。 | 🔶 **推奨** |
| 9 | 本番設定ファイルに文字化けがある | 🟡 中 | `application-prod.properties` を実際に確認したところ設定値（`DB_URL`・`JWT_SECRET` 等）は正常に読み込まれており、文字化けはコメント行のみ。現時点で実害なし。UTF-8 で保存し直すことでメンテナンス性が向上する。 | 実害は現時点で確認されていない。可読性・保守性の観点から他の改修と合わせて対応推奨。 | 🔶 **推奨** |
| 10 | Maven Wrapper が Windows 環境で起動できない | 🟡 中 | Docker ビルドはコンテナ内で `mvn` を直接実行するため本番動作への影響はない。開発時に PowerShell からテストを実行できない問題。`mvnw.cmd` の再生成または修正で対応可能。 | 本番への直接影響はないが、テスト実行できない状態は品質担保の観点で課題。CI 整備と合わせて対応推奨。 | 🔶 **推奨** |
| 11 | JWT を localStorage に保存（XSS リスク） | 🟢 低 | 社内 LAN 運用かつ表示コンテンツが業務データに限定されるため、現実的な XSS リスクは極めて低い。将来的な公開を検討する段階で httpOnly Cookie への移行を検討する。 | 現状の社内 LAN 運用では許容範囲内と判断。将来の外部公開時に再評価する。 | ➖ **現状対応不要** |
| 12 | CSRF 保護が無効化されている | 🟢 低 | JWT ヘッダー認証方式では CSRF 攻撃は成立しにくい。現在の実装では問題なし。Cookie 認証へ変更する場合は再検討が必要。 | 現状の認証方式では問題なし。対応不要。 | ➖ **対応不要** |
| 13 | `h2-console` が `permitAll` のまま残っている | 🟢 低 | 現状は PostgreSQL 構成のため H2 Console は実際には無効。設定として残っているのは不要だが実害はない。次回リリース時に削除で十分。 | 実害なし。清掃対応として任意で削除。 | ➖ **任意** |
| 14 | Spring Boot が SNAPSHOT 版を使用 | 🟡 中 | `pom.xml` が `3.5.15-SNAPSHOT` を参照している。スナップショット版は予告なく変更されるため本番環境には不向き。安定版（GA）への固定を推奨。 | 本番運用前に安定版へ固定する。リビルド時に予期しない変更が入るリスクを排除する。 | 🔶 **推奨** |

---

## 優先対応順

1. 本番で `schema.sql` を実行しない設定に変更する
2. 平文シークレットを削除し、漏洩済み前提でローテーションする
3. ユーザー管理APIを `ADMIN` のみに制限する
4. 汎用テーブル編集・CSV入出力の対象を業務テーブル allowlist に限定する
5. CSV上書き取込をトランザクション化し、事前検証を追加する
6. DB内部エラーをレスポンスに返さない
7. 操作履歴の失敗をログ・監視できるようにする
8. Maven Wrapper を修正し、バックエンドテストを実行可能にする
