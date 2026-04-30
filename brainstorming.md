# DB管理表示アプリ 壁打ちメモ

作成日: 2026-04-30

---

## アプリ概要

業務用DBの内容をブラウザ上で閲覧・編集できるWebアプリ。
社内・社外のユーザーが権限レベルに応じて操作できる。
ローカル開発からスタートし、最終的にAWS RDS上で運用する。

---

## 要件整理

| 項目 | 内容 |
|------|------|
| 対象DB | SQLite（開発）→ AWS RDS（本番） |
| UI | Webブラウザ（CRUD + 閲覧の両方） |
| ユーザー | 社内 + 社外、約15人 |
| 権限 | 3段階 + カラム単位の非表示制御 |
| フロント | Vue.js（Python優先だったが後述の理由で変更） |
| バック | Java（Spring Boot） |
| テーブル数 | 20〜30テーブル |
| 最大行数 | 3,000行/テーブル |

---

## 権限設計

### 3段階ロール

| ロール | 権限 |
|--------|------|
| Admin（管理者） | 全テーブル・全カラムの閲覧・追加・編集・削除、ユーザー管理 |
| Editor（編集者） | 許可テーブルの閲覧・追加・編集（削除不可）、マスク対象カラムは非表示 |
| Viewer（閲覧者） | 許可テーブルの閲覧のみ（読み取り専用）、マスク対象カラムは非表示 |

### カラムマスキング

- 特定カラム（例：給与、マイナンバー、個人情報など）をロールによって **列ごと非表示** にする
- `*****` に置換するのではなく、**カラム自体をAPIレスポンスから除外**する
- 設定方法：テーブル×カラム×ロールの組み合わせをDBまたは設定ファイルで管理

---

## 技術スタック

### フロントエンド：Vue.js

Pythonでブラウザアプリを作ることも検討したが、以下の理由でVue.jsを推奨。

| 検討案 | 評価 |
|--------|------|
| Streamlit（Python） | 認証・権限管理・CRUD対応が困難 ✗ |
| Dash（Python） | 可能だが認証実装が複雑 △ |
| FastAPI + Jinja2（Python） | JavaバックエンドとのAPI二重管理が発生 △ |
| **Vue.js** | Spring Boot REST APIとの相性◎、権限制御も実装しやすい ✓ |
| Thymeleaf（Java） | フロントをJavaに統一できるが、UIの柔軟性が低い △ |

### バックエンド：Java Spring Boot

- REST APIサーバーとして動作
- Spring Security + JWT でログイン・認証
- JPA / Hibernate でDB操作（SQLite → RDS 移行もほぼ設定変更のみ）
- カラムマスキングはAPIレスポンス生成時に除外

### データベース

| 環境 | DB |
|------|----|
| ローカル開発 | SQLite |
| 本番 | AWS RDS（MySQL または PostgreSQL） |

Spring Boot + JPA の組み合わせで、DB切り替えはほぼ `application.properties` の変更のみ。

---

## システム構成図（概要）

```
[ブラウザ]
    │  Vue.js SPA
    │  axios で REST API 呼び出し
    ▼
[Spring Boot サーバー]
    │  Spring Security（JWT認証・ロール制御）
    │  REST API エンドポイント
    │  カラムマスキングロジック
    │  JPA / Hibernate
    ▼
[DB]
  ローカル: SQLite
  本番:     AWS RDS (MySQL / PostgreSQL)
```

---

## カラムマスキングの実装方針

```
masked_columns テーブル（または設定ファイル）
  - table_name : テーブル名
  - column_name: 非表示にするカラム名
  - role       : 非表示にする対象ロール（EDITOR / VIEWER）

APIレスポンス生成時に、ログインユーザーのロールと照合して
対象カラムをJSONから除外して返す
```

---

## 開発フェーズ

### Phase 1：ローカル（SQLite）
- [ ] Spring Boot プロジェクト初期化
- [ ] ユーザー認証（JWT）実装
- [ ] テーブル一覧・レコード一覧API
- [ ] CRUD API（追加・編集・削除）
- [ ] カラムマスキング実装
- [ ] Vue.js フロントエンド（一覧・詳細・編集画面）
- [ ] ロール別表示制御

### Phase 2：AWS移行
- [ ] RDS（MySQL or PostgreSQL）セットアップ
- [ ] Spring Boot の DB接続設定変更
- [ ] EC2 または ECS へのデプロイ
- [ ] HTTPS対応（ALB + ACM）

---

## 懸念点・決定事項

| 項目 | 内容 |
|------|------|
| RDBMSの選択 | MySQL か PostgreSQL かは Phase 2 前に決定が必要 |
| カラムマスキング設定のUI | 管理者がブラウザ上で設定できると便利（Phase 2以降） |
| テーブル定義の自動認識 | DBスキーマを自動読み取りして一覧生成するか、手動設定か |
| 外部ユーザーのアカウント管理 | 招待制 or 自己登録制？ |
| Vue.jsのビルド配信方法 | Spring Bootに同梱 or 別途S3/CloudFront |

---

## ネクストアクション

- [ ] RDBMSをMySQL / PostgreSQLどちらにするか決定
- [ ] テーブル定義の自動認識 or 手動設定の方針決定
- [ ] 外部ユーザーのアカウント管理方式を決定
- [ ] Spring Boot プロジェクト初期化（Phase 1 キックオフ）
