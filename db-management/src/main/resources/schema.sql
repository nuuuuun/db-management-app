-- ============================================================
-- 案件管理テーブル
-- ============================================================
DROP TABLE IF EXISTS PROJECTS;
CREATE TABLE PROJECTS (
    ID           BIGINT AUTO_INCREMENT PRIMARY KEY,
    TOROKU_DATE  DATE,
    ANKEN_ID     VARCHAR(20)  UNIQUE,
    ANKEN_NAME   VARCHAR(200),
    JIRA_LINK    VARCHAR(300),
    TR_JIRA_LINK VARCHAR(300),
    KOUTEI       VARCHAR(50),
    STATUS       VARCHAR(20),
    GAIYOU       VARCHAR(1000),
    ITAKU_NO     VARCHAR(50)
);

-- ============================================================
-- 要求管理テーブル
-- ============================================================
DROP TABLE IF EXISTS REQUIREMENTS;
CREATE TABLE REQUIREMENTS (
    ID           BIGINT AUTO_INCREMENT PRIMARY KEY,
    TOROKU_DATE  DATE,
    ANKEN_NAME   VARCHAR(200),
    YOQYU_MOTO   VARCHAR(100),
    YOQYU_ID     VARCHAR(20)  UNIQUE,
    YOQYU_SHIYOU VARCHAR(200),
    YOQYU_NAME   VARCHAR(200),
    STATUS       VARCHAR(20),
    HAIKEI       VARCHAR(1000),
    YOQYU_GAIYOU VARCHAR(1000),
    JIRA_LINK    VARCHAR(300),
    SHIRYO_LINK  VARCHAR(300)
);

-- ============================================================
-- 要件管理テーブル
-- ============================================================
DROP TABLE IF EXISTS SPECIFICATIONS;
CREATE TABLE SPECIFICATIONS (
    ID           BIGINT AUTO_INCREMENT PRIMARY KEY,
    TOROKU_DATE  DATE,
    YOQYU_ID     VARCHAR(20),
    YOKEN_ID     VARCHAR(20)  UNIQUE,
    YOKEN_GAIYOU VARCHAR(1000),
    JIRA_LINK    VARCHAR(300),
    STATUS       VARCHAR(20),
    BIKO         VARCHAR(500)
);

-- ============================================================
-- アプリ管理テーブル
-- ============================================================
DROP TABLE IF EXISTS APPLICATIONS;
CREATE TABLE APPLICATIONS (
    ID           BIGINT AUTO_INCREMENT PRIMARY KEY,
    TOROKU_DATE  DATE,
    APP_ID       VARCHAR(20)  UNIQUE,
    APP_NAME     VARCHAR(200),
    KANRYO_DATE  DATE,
    BIKO         VARCHAR(500),
    DIFF_VER     VARCHAR(50),
    CVOS_VER     VARCHAR(50),
    KTS_VER      VARCHAR(50)
);

-- ============================================================
-- 環境管理テーブル
-- ============================================================
DROP TABLE IF EXISTS ENVIRONMENTS;
CREATE TABLE ENVIRONMENTS (
    ID               BIGINT AUTO_INCREMENT PRIMARY KEY,
    ENV_ID           VARCHAR(20)  UNIQUE,
    KANKYO           VARCHAR(50),
    EDABAN           VARCHAR(20),
    ENV_NAME         VARCHAR(100),
    DEPLOY_PLAN_DATE DATE,
    DEPLOY_DONE_DATE DATE,
    APP_ID           VARCHAR(20),
    APP_NAME         VARCHAR(200),
    TOKYO            VARCHAR(50),
    OSAKA            VARCHAR(50)
);

-- ============================================================
-- 操作ヒストリー
-- ============================================================
CREATE TABLE IF NOT EXISTS OPERATION_HISTORY (
    ID            BIGINT AUTO_INCREMENT PRIMARY KEY,
    OPERATED_AT   TIMESTAMP    NOT NULL,
    OPERATOR      VARCHAR(100) NOT NULL,
    TARGET_TABLE  VARCHAR(100) NOT NULL,
    TARGET_ID     VARCHAR(100),
    OPERATION     VARCHAR(20)  NOT NULL,
    BEFORE_VALUE  CLOB,
    AFTER_VALUE   CLOB
);
ALTER TABLE OPERATION_HISTORY ADD COLUMN IF NOT EXISTS BEFORE_VALUE CLOB;
ALTER TABLE OPERATION_HISTORY ADD COLUMN IF NOT EXISTS AFTER_VALUE CLOB;
