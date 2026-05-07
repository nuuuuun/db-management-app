-- ============================================================
-- ユーザーテーブル
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email    VARCHAR(255),
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL
);
-- 既存テーブルに email カラムがない場合に追加
ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- ============================================================
-- 案件管理テーブル
-- ============================================================
CREATE TABLE IF NOT EXISTS projects (
    id           BIGSERIAL PRIMARY KEY,
    toroku_date  DATE,
    anken_id     VARCHAR(20)  UNIQUE,
    anken_name   VARCHAR(200),
    jira_link    VARCHAR(300),
    tr_jira_link VARCHAR(300),
    koutei       VARCHAR(50),
    status       VARCHAR(20),
    gaiyou       VARCHAR(1000),
    itaku_no     VARCHAR(50)
);

-- ============================================================
-- 要求管理テーブル
-- ============================================================
CREATE TABLE IF NOT EXISTS requirements (
    id           BIGSERIAL PRIMARY KEY,
    toroku_date  DATE,
    anken_name   VARCHAR(200),
    yoqyu_moto   VARCHAR(100),
    yoqyu_id     VARCHAR(20)  UNIQUE,
    yoqyu_shiyou VARCHAR(200),
    yoqyu_name   VARCHAR(200),
    status       VARCHAR(20),
    haikei       VARCHAR(1000),
    yoqyu_gaiyou VARCHAR(1000),
    jira_link    VARCHAR(300),
    shiryo_link  VARCHAR(300)
);

-- ============================================================
-- 要件管理テーブル
-- ============================================================
CREATE TABLE IF NOT EXISTS specifications (
    id           BIGSERIAL PRIMARY KEY,
    toroku_date  DATE,
    yoqyu_id     VARCHAR(20),
    yoken_id     VARCHAR(20)  UNIQUE,
    yoken_gaiyou VARCHAR(1000),
    jira_link    VARCHAR(300),
    status       VARCHAR(20),
    biko         VARCHAR(500)
);

-- ============================================================
-- アプリ管理テーブル
-- ============================================================
CREATE TABLE IF NOT EXISTS applications (
    id           BIGSERIAL PRIMARY KEY,
    toroku_date  DATE,
    app_id       VARCHAR(20)  UNIQUE,
    app_name     VARCHAR(200),
    kanryo_date  DATE,
    biko         VARCHAR(500),
    diff_ver     VARCHAR(50),
    cvos_ver     VARCHAR(50),
    kts_ver      VARCHAR(50)
);

-- ============================================================
-- 環境管理テーブル
-- ============================================================
CREATE TABLE IF NOT EXISTS environments (
    id               BIGSERIAL PRIMARY KEY,
    env_id           VARCHAR(20)  UNIQUE,
    kankyo           VARCHAR(50),
    edaban           VARCHAR(20),
    env_name         VARCHAR(100),
    deploy_plan_date DATE,
    deploy_done_date DATE,
    app_id           VARCHAR(20),
    app_name         VARCHAR(200),
    tokyo            VARCHAR(50),
    osaka            VARCHAR(50)
);

-- ============================================================
-- 操作ヒストリー
-- ============================================================
CREATE TABLE IF NOT EXISTS operation_history (
    id            BIGSERIAL PRIMARY KEY,
    operated_at   TIMESTAMP    NOT NULL,
    operator      VARCHAR(100) NOT NULL,
    target_table  VARCHAR(100) NOT NULL,
    target_id     VARCHAR(100),
    operation     VARCHAR(20)  NOT NULL,
    before_value  TEXT,
    after_value   TEXT
);

-- ============================================================
-- マスクカラム設定テーブル
-- ============================================================
CREATE TABLE IF NOT EXISTS masked_columns (
    id          BIGSERIAL PRIMARY KEY,
    table_name  VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    role        VARCHAR(20)  NOT NULL
);
