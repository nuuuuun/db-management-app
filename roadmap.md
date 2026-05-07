# デプロイ・運用ロードマップ

作成日: 2026-05-04

---

## 全体の流れ

```
[Phase 1] ローカル動作確認         ✅ 完了
    ↓
[Phase 2] AWSリソース構築          ← 現在地
    ↓
[Phase 3] 初回手動デプロイ
    ↓
[Phase 4] CodePipeline自動化
    ↓
[Phase 5] 本番運用
```

---

## Phase 1: ローカル動作確認

**目的**: Docker Compose でアプリが正常に動くことを確認してからAWSへ進む。

```powershell
docker compose up --build
# → http://localhost:18080 で確認
```

### チェックリスト

- [x] `docker compose up --build` が成功する
- [x] `http://localhost:18080` でログイン画面が表示される
- [x] admin / password123 でログインできる
- [x] テーブル一覧・レコード一覧が表示される
- [x] CRUD操作（追加・編集・削除）が動作する
- [x] CSVインポートが動作する
- [x] Editor / Viewer のカラムマスキングが正しく機能する

---

## Phase 2: AWSリソース構築

**目的**: アプリを動かすための AWS インフラを用意する。

### 2-1. ECR（コンテナレジストリ）

```
ECRリポジトリ名: db-management
リージョン: ap-northeast-1
```

- [ ] ECR リポジトリを作成する
- [ ] ローカルから手動でイメージをプッシュして動作確認（初回のみ）

### 2-2. RDS（PostgreSQL）

```
エンジン: PostgreSQL 16
インスタンス: db.t3.micro（開発）/ db.t3.small以上（本番）
DB名: dbmanagement
ユーザー: postgres
マルチAZ: 不要（開発）/ 有効（本番）
パブリックアクセス: 無効（ECSからのみアクセス）
```

- [ ] VPC・サブネットグループを確認する
- [ ] RDS インスタンスを作成する
- [ ] セキュリティグループ設定（ECSタスクからの5432ポートのみ許可）
- [ ] RDSエンドポイントをメモする

### 2-3. Secrets Manager（機密情報管理）

`taskdef.json` が参照するシークレットを登録する。

```
シークレット名: db-management/prod
キー一覧:
  DB_URL       : jdbc:postgresql://<RDSエンドポイント>:5432/dbmanagement
  DB_USERNAME  : postgres
  DB_PASSWORD  : （設定したパスワード）
  JWT_SECRET   : （本番用のランダム文字列）
  INIT_PASSWORD: （adminの初期パスワード）
```

- [ ] Secrets Manager にシークレットを作成する
- [ ] `taskdef.json` の `<AWS_ACCOUNT_ID>` を実際のアカウントIDに置き換える

### 2-4. ECS（Fargate）

```
クラスター名: db-management
サービス名: db-management
タスク定義: taskdef.json を使用
CPU: 512 / メモリ: 1024
```

- [ ] ECSクラスターを作成する
- [ ] ECSタスク実行ロール（`ECSTaskExecutionRole`）に以下のポリシーをアタッチする
  - `AmazonECSTaskExecutionRolePolicy`
  - Secrets Manager の読み取り権限
- [ ] タスク定義を登録する（`taskdef.json`）
- [ ] ECSサービスを作成する

### 2-5. ALB（ロードバランサー）

> **ドメインなしのためCloudFrontでHTTPS化する。ACM証明書不要。**
> アクセスURL: `https://xxxx.cloudfront.net`（CloudFrontが自動発行）

```
ターゲットグループ: ポート8080 / ヘルスチェックパス: /actuator/health
リスナー: HTTP 80のみ（CloudFrontからのアクセスを受ける）
ALBはインターネット向け（internet-facing）だが、
セキュリティグループでCloudFrontのIPのみ許可する
```

- [ ] ALBを作成する（internet-facing）
- [ ] ターゲットグループを作成する（ポート8080 / ECSサービスに紐付け）
- [ ] ALBリスナーをHTTP 80で設定する
- [ ] ALBのセキュリティグループをCloudFrontのマネージドプレフィックスリストに限定する
  - `pl-58a04531`（CloudFront origin facing、ap-northeast-1）

### 2-6. CloudFront（HTTPS化）

```
オリジン: ALBのDNS名
プロトコル: HTTP（ALBとの通信）
ビューワープロトコル: Redirect HTTP to HTTPS
キャッシュ: CachingDisabled（APIサーバーのためキャッシュしない）
```

- [ ] CloudFrontディストリビューションを作成する
- [ ] オリジンにALBのDNS名を設定する（HTTP）
- [ ] ビューワープロトコルポリシーを「Redirect HTTP to HTTPS」に設定する
- [ ] キャッシュポリシーを `CachingDisabled` にする
- [ ] `https://xxxx.cloudfront.net` でアクセスできることを確認する

---

## Phase 3: 初回手動デプロイ

**目的**: CI/CDを組む前に、手動で一度 AWS 上で動かして疎通確認する。

```powershell
# ECRにログイン
aws ecr get-login-password --region ap-northeast-1 | docker login --username AWS --password-stdin <AWS_ACCOUNT_ID>.dkr.ecr.ap-northeast-1.amazonaws.com

# イメージビルド & プッシュ
docker build -t <AWS_ACCOUNT_ID>.dkr.ecr.ap-northeast-1.amazonaws.com/db-management:latest .
docker push <AWS_ACCOUNT_ID>.dkr.ecr.ap-northeast-1.amazonaws.com/db-management:latest

# ECSサービスを強制更新
aws ecs update-service --cluster db-management --service db-management --force-new-deployment
```

### チェックリスト

- [ ] ECRにイメージがプッシュされる
- [ ] ECSタスクが起動する（CloudWatch Logsでエラーがないか確認）
- [ ] ALB経由でブラウザからアクセスできる
- [ ] RDSへの接続・スキーマ初期化が成功する
- [ ] `https://xxxx.cloudfront.net` でログイン・CRUD操作が正常に動作する

---

## Phase 4: CodePipeline による自動化

**目的**: `git push` するだけで自動でビルド・デプロイされる環境を作る。

### パイプライン構成

```
[Source]        GitHub / CodeCommit
    ↓
[Build]         CodeBuild（buildspec.yml でDockerビルド & ECRプッシュ）
    ↓
[Deploy]        CodeDeploy（ECSサービスを新イメージで更新）
```

### 4-1. CodeBuild の準備

`buildspec.yml` はすでに存在する。以下を設定する。

```
ビルドプロジェクト名: db-management-build
環境変数（CodeBuildに設定）:
  AWS_ACCOUNT_ID: （AWSアカウントID）
  ECR_REPO_NAME: db-management
  CONTAINER_NAME: db-management
```

- [ ] CodeBuild プロジェクトを作成する
- [ ] CodeBuild のサービスロールに ECR プッシュ権限を追加する
  - `AmazonEC2ContainerRegistryPowerUser`
- [ ] CodeBuild でテストビルドを実行して成功を確認する

### 4-2. CodePipeline の作成

```
パイプライン名: db-management-pipeline
ソース: GitHub（対象リポジトリ・ブランチを指定）
ビルド: CodeBuild（db-management-build）
デプロイ: Amazon ECS（クラスター・サービスを指定）
```

- [ ] CodePipeline を作成する
- [ ] GitHub との接続（CodeStar Connections）を設定する
- [ ] デプロイステージで `imagedefinitions.json` を使う設定にする
- [ ] テストとして `git push` → パイプラインが自動起動することを確認する

---

## Phase 5: 本番運用

### 運用上の確認事項

| 項目 | 内容 |
|------|------|
| ログ確認 | CloudWatch Logs（ロググループ: `/ecs/db-management`）|
| DBバックアップ | RDS自動バックアップ（保持期間7日）|
| スケーリング | ECSサービスの最小/最大タスク数を設定 |
| アクセスURL | `https://xxxx.cloudfront.net`（ドメインなし構成） |
| モニタリング | CloudWatch アラーム（CPU・メモリ・5xxエラー）|

---

## 現在のファイル対応表

| ファイル | 用途 | フェーズ |
|---------|------|---------|
| `Dockerfile` | マルチステージビルド（Vue.js + Spring Boot） | Phase 3以降 |
| `docker-compose.yml` | ローカル開発・動作確認 | Phase 1 |
| `buildspec.yml` | CodeBuild でのビルド・ECRプッシュ手順 | Phase 4 |
| `taskdef.json` | ECS タスク定義（`<AWS_ACCOUNT_ID>`の置き換えが必要） | Phase 2-3 |

---

## 参考：AWSコスト概算（東京リージョン）

| リソース | スペック | 月額目安 |
|---------|---------|---------|
| ECS Fargate | 0.25vCPU / 0.5GB × 24h | ~$10 |
| RDS PostgreSQL | db.t3.micro | ~$15 |
| ALB | 最小構成 | ~$20 |
| ECR | 1GBストレージ | ~$0.1 |
| **合計** | | **~$45/月** |

> ※ 開発環境は使わないときタスクを0にするとFargate代を節約できる。
