# DB管理アプリ

社内業務データ（案件・要求・要件・アプリ・環境）を一元管理するWebアプリケーション。

---

## 起動方法

### ローカル起動（Docker不要） ← 現在推奨

PostgreSQL をローカルにインストールして直接起動する方法。

**前提条件:**
- [PostgreSQL 16](https://www.postgresql.org/download/) がインストール済みで起動していること
- [Node.js 20 LTS](https://nodejs.org/) がインストール済みであること
- [JDK 21 LTS](https://adoptium.net/) がインストール済みであること

**起動:**
```powershell
# 初回 or フロントエンドに変更があった場合（ビルドして起動）
.\scripts\start-local.ps1

# 開発者向け（HMR有効・バックエンドとフロントエンドを別プロセス）
.\scripts\start-dev.ps1
```

**アクセス:** http://localhost:8080

**初期ユーザー:**
| ユーザー名 | パスワード | ロール |
|-----------|-----------|-------|
| admin | password123 | ADMIN |
| editor | password123 | EDITOR |
| viewer | password123 | VIEWER |

> ⚠️ 初回ログイン後は必ずパスワードを変更してください。

---

### Docker起動（AWS移行時に使用）

Docker Desktop がインストール済みの環境向け。本番 / AWS ECS 移行の際に使用。

**前提条件:**
- Docker Desktop がインストール済みで起動していること
- `.env` ファイルが存在すること（`.env.example` をコピーして作成）

**起動:**
```powershell
docker compose up -d --build
```

**アクセス:** http://localhost:18080

---

## 環境変数（ローカル起動時）

ローカル起動時はデフォルト値で動作するため `.env` ファイルは不要。
本番環境では必ず環境変数で上書きすること。

| 変数名 | デフォルト値 | 説明 |
|-------|------------|------|
| DB_URL | jdbc:postgresql://localhost:5432/dbmanagement | DB接続URL |
| DB_USERNAME | postgres | DBユーザー名 |
| DB_PASSWORD | postgres | DBパスワード |
| JWT_SECRET | （開発用デフォルト） | JWT署名鍵 |
| INIT_PASSWORD | password123 | 初期ユーザーパスワード |

---

## プロジェクト構成

```
db-management-app/
├── db-management/          # Spring Boot バックエンド (Java 21)
├── frontend/               # Vue.js フロントエンド
├── scripts/
│   ├── start-local.ps1     # ローカル起動（ビルド込み）
│   └── start-dev.ps1       # 開発者向け起動（HMR有効）
├── devDoc/                 # 開発・保守ドキュメント
├── docker-compose.yml      # Docker起動用（AWS移行時）
├── Dockerfile              # Docker イメージビルド定義
└── .env.example            # 環境変数テンプレート
```

---

## ドキュメント

[devDoc/index.html](devDoc/index.html) を開くと全ドキュメントにアクセスできます。
