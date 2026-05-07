export const TABLE_LABELS = {
  PROJECTS:          '案件管理',
  REQUIREMENTS:      '要求管理',
  SPECIFICATIONS:    '要件管理',
  APPLICATIONS:      'アプリ管理',
  ENVIRONMENTS:      '環境管理',
  OPERATION_HISTORY: '操作履歴',
  USERS:             'ユーザー',
}

export const COLUMN_LABELS = {
  PROJECTS: {
    ID:           'ID',
    TOROKU_DATE:  '登録日',
    ANKEN_ID:     '案件ID',
    ANKEN_NAME:   '案件名',
    JIRA_LINK:    'Jiraリンク',
    TR_JIRA_LINK: 'TRJiraリンク',
    KOUTEI:       '工程',
    STATUS:       'status',
    GAIYOU:       '概要',
    ITAKU_NO:     '業務委託管理No.',
  },
  REQUIREMENTS: {
    ID:           'ID',
    TOROKU_DATE:  '登録日',
    ANKEN_NAME:   '案件名',
    YOQYU_MOTO:   '要求元',
    YOQYU_ID:     '要求ID',
    YOQYU_SHIYOU: '要求仕様',
    YOQYU_NAME:   '要求名',
    STATUS:       'status',
    HAIKEI:       '背景理由やリンク等',
    YOQYU_GAIYOU: '要求概要',
    JIRA_LINK:    'Jiraリンク',
    SHIRYO_LINK:  '資料リンク',
  },
  SPECIFICATIONS: {
    ID:           'ID',
    TOROKU_DATE:  '登録日',
    YOQYU_ID:     '要求ID',
    YOKEN_ID:     '要件ID',
    YOKEN_GAIYOU: '要件概要',
    JIRA_LINK:    'Jiraリンク',
    STATUS:       'status',
    BIKO:         '備考',
  },
  APPLICATIONS: {
    ID:           'ID',
    TOROKU_DATE:  '登録日',
    APP_ID:       'アプリID',
    APP_NAME:     'アプリ名',
    KANRYO_DATE:  '完了日',
    BIKO:         '備考',
    DIFF_VER:     'DIFF_Ver',
    CVOS_VER:     'CVOS_Ver',
    KTS_VER:      'KTS_Ver',
  },
  ENVIRONMENTS: {
    ID:               'ID',
    ENV_ID:           '環境ID',
    KANKYO:           '環境',
    EDABAN:           '枝番',
    ENV_NAME:         '環境名',
    DEPLOY_PLAN_DATE: 'デプロイ予定日',
    DEPLOY_DONE_DATE: 'デプロイ完了日',
    APP_ID:           'アプリID',
    APP_NAME:         'アプリ名',
    TOKYO:            '東京',
    OSAKA:            '大阪',
  },
  OPERATION_HISTORY: {
    ID:           'ID',
    OPERATED_AT:  '操作日時',
    OPERATOR:     '操作者',
    TARGET_TABLE: '対象テーブル',
    TARGET_ID:    '対象ID',
    OPERATION:    '操作内容',
    BEFORE_VALUE: '変更前',
    AFTER_VALUE:  '変更後',
  },
}

export const FIELD_DEFS = {
  PROJECTS: {
    TOROKU_DATE:  { type: 'date' },
    KOUTEI:       { type: 'select', options: ['要件定義', '設計', '開発', 'テスト', 'リリース'] },
    STATUS:       { type: 'select', options: ['提案中', '進行中', '完了', '中止'] },
  },
  REQUIREMENTS: {
    TOROKU_DATE:  { type: 'date' },
    STATUS:       { type: 'select', options: ['未対応', '対応中', '完了'] },
  },
  SPECIFICATIONS: {
    TOROKU_DATE:  { type: 'date' },
    STATUS:       { type: 'select', options: ['未着手', '実装中', '完了', '却下'] },
  },
  APPLICATIONS: {
    TOROKU_DATE:  { type: 'date' },
    KANRYO_DATE:  { type: 'date' },
  },
  ENVIRONMENTS: {
    DEPLOY_PLAN_DATE: { type: 'date' },
    DEPLOY_DONE_DATE: { type: 'date' },
    KANKYO:  { type: 'select', options: ['開発', 'ステージング', '本番', 'テスト'] },
    TOKYO:   { type: 'select', options: ['', '稼働中', '停止中', '構築中'] },
    OSAKA:   { type: 'select', options: ['', '稼働中', '停止中', '構築中'] },
  },
}

export const COLUMN_LINKS = {
  REQUIREMENTS: {
    ANKEN_NAME: { table: 'PROJECTS', filterCol: 'ANKEN_NAME' },
  },
  SPECIFICATIONS: {
    YOQYU_ID: { table: 'REQUIREMENTS', filterCol: 'YOQYU_ID' },
  },
  ENVIRONMENTS: {
    APP_ID: { table: 'APPLICATIONS', filterCol: 'APP_ID' },
  },
}

export function fieldDef(tableName, colName) {
  return FIELD_DEFS[tableName]?.[colName] || { type: 'text' }
}

export function tableLabel(tableName) {
  return TABLE_LABELS[tableName] || tableName
}

export function colLabel(tableName, colName) {
  return COLUMN_LABELS[tableName]?.[colName] || colName
}

export function columnLink(tableName, colName) {
  return COLUMN_LINKS[tableName]?.[colName] || null
}
