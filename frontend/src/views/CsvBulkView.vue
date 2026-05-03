<template>
  <div class="csv-bulk">
    <h1>CSV 一括インポート / エクスポート</h1>

    <!-- エクスポート -->
    <div class="section">
      <h2>全テーブル エクスポート</h2>
      <p class="desc">全テーブルのデータを1つのCSVファイルでダウンロードします。</p>
      <button class="btn-export" @click="doExport" :disabled="exporting">
        {{ exporting ? 'エクスポート中...' : '⬇ CSVでダウンロード' }}
      </button>
      <p v-if="exportError" class="error">{{ exportError }}</p>
    </div>

    <hr />

    <!-- インポート -->
    <div class="section">
      <h2>全テーブル 一括インポート</h2>
      <p class="desc">
        CSVファイルを選択してください。<code>#TABLE:テーブル名</code> のマーカーで各テーブルのセクションを区切ります。<br>
        エクスポートしたファイルをそのままインポートに使えます。
      </p>

      <div class="field">
        <label>インポートモード</label>
        <select v-model="mode">
          <option value="append">追記（既存データ保持）</option>
          <option value="overwrite">上書き（既存データを全削除してから挿入）</option>
        </select>
      </div>
      <div class="field">
        <label>CSVファイル</label>
        <input type="file" accept=".csv" @change="onFileChange" />
      </div>
      <button class="btn-import" @click="doImport" :disabled="!importFile || importing">
        {{ importing ? 'インポート中...' : '⬆ インポート実行' }}
      </button>
      <p v-if="importError" class="error">{{ importError }}</p>

      <!-- 結果テーブル -->
      <div v-if="importResults" class="results">
        <h3>インポート結果</h3>

        <!-- サマリー -->
        <table class="summary-table">
          <thead>
            <tr><th>テーブル</th><th>結果</th><th>エラー件数</th></tr>
          </thead>
          <tbody>
            <tr v-for="(result, table) in importResults" :key="table"
                :class="result.error ? 'row-error' : result.skipped ? 'row-skip' : 'row-ok'">
              <td>{{ table }}</td>
              <td>
                <span v-if="result.error">❌ {{ result.error }}</span>
                <span v-else-if="result.skipped">⚠ {{ result.skipped }}</span>
                <span v-else>✅ {{ result.inserted }} 件インポート</span>
              </td>
              <td>{{ result.errors ? result.errors.length : 0 }} 件</td>
            </tr>
          </tbody>
        </table>

        <!-- エラー詳細 -->
        <template v-for="(result, table) in importResults" :key="table">
          <div v-if="result.errors && result.errors.length > 0" class="error-detail">
            <h4>{{ table }} のエラー詳細</h4>
            <table class="error-table">
              <thead>
                <tr><th>行番号</th><th>行の内容</th><th>エラー内容</th></tr>
              </thead>
              <tbody>
                <tr v-for="err in result.errors" :key="err.line">
                  <td class="line-no">{{ err.line }}</td>
                  <td class="line-content"><code>{{ err.content }}</code></td>
                  <td class="line-msg">{{ err.message }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>
    </div>

    <!-- CSVフォーマット説明 -->
    <div class="section hint">
      <h2>CSVフォーマット</h2>
      <pre>#TABLE:PROJECTS
ANKEN_ID,ANKEN_NAME,CLIENT,MANAGER,STATUS,START_DATE,END_DATE,REMARKS
ANK-001,プロジェクト001,株式会社サンプル商事,山田太郎,進行中,2024-01-01,2024-12-31,備考1
ANK-002,プロジェクト002,有限会社テスト産業,鈴木花子,完了,2024-02-01,2024-08-31,

#TABLE:REQUIREMENTS
YOQYU_ID,ANKEN_ID,YOQYU_NAME,DESCRIPTION,PRIORITY,STATUS,REQUESTED_BY,REMARKS
YQY-001,ANK-001,要求事項001,要求1の詳細説明です,高,完了,山田太郎,備考1</pre>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'CsvBulkView',
  data() {
    return {
      exporting: false,
      exportError: null,
      importFile: null,
      mode: 'append',
      importing: false,
      importError: null,
      importResults: null,
    }
  },
  methods: {
    async doExport() {
      this.exporting = true
      this.exportError = null
      try {
        const response = await api.get('/api/export/all', { responseType: 'blob' })
        const url = window.URL.createObjectURL(new Blob([response.data], { type: 'text/csv' }))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'db-export.csv')
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (e) {
        this.exportError = 'エクスポートに失敗しました: ' + e.message
      } finally {
        this.exporting = false
      }
    },
    onFileChange(e) {
      this.importFile = e.target.files[0]
      this.importResults = null
      this.importError = null
    },
    async doImport() {
      if (!this.importFile) return
      this.importing = true
      this.importResults = null
      this.importError = null
      try {
        const formData = new FormData()
        formData.append('file', this.importFile)
        formData.append('mode', this.mode)
        const res = await api.post('/api/import/all', formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        this.importResults = res.data
      } catch (e) {
        this.importError = e.response?.data?.error || 'インポートに失敗しました: ' + e.message
      } finally {
        this.importing = false
      }
    },
  },
}
</script>

<style scoped>
.csv-bulk { padding: 2rem; max-width: 900px; }
h1 { margin-bottom: 1.5rem; }
.section { margin-bottom: 2rem; }
.section h2 { font-size: 1.1rem; margin-bottom: 0.5rem; border-bottom: 2px solid #eee; padding-bottom: 0.25rem; }
.desc { font-size: 0.9rem; color: #555; margin-bottom: 1rem; line-height: 1.6; }
hr { border: none; border-top: 1px solid #ddd; margin: 2rem 0; }
.field { margin-bottom: 1rem; }
.field label { display: block; font-size: 0.85rem; color: #555; margin-bottom: 4px; }
select, input[type="file"] { padding: 6px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 0.9rem; }
.btn-export {
  padding: 10px 24px; background: #4a7fc1; color: white;
  border: none; border-radius: 4px; cursor: pointer; font-size: 0.95rem;
}
.btn-import {
  padding: 10px 24px; background: #4a9e6b; color: white;
  border: none; border-radius: 4px; cursor: pointer; font-size: 0.95rem;
}
button:disabled { background: #aaa; cursor: default; }
.error { color: red; margin-top: 0.5rem; font-size: 0.9rem; }
.results { margin-top: 1.5rem; }
.results h3 { font-size: 1rem; margin-bottom: 0.75rem; }
.summary-table, .error-table { width: 100%; border-collapse: collapse; margin-bottom: 1rem; }
th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; font-size: 0.88rem; }
th { background: #f4f4f4; }
.row-ok { background: #f0fff4; }
.row-error { background: #fff0f0; }
.row-skip { background: #fffbea; }
.error-detail { margin-top: 1rem; }
.error-detail h4 { font-size: 0.9rem; color: #c0392b; margin-bottom: 0.4rem; }
.line-no { width: 70px; text-align: center; font-weight: bold; color: #c0392b; }
.line-content { max-width: 400px; overflow-x: auto; }
.line-content code { font-size: 0.82rem; background: #f8f8f8; padding: 2px 4px; border-radius: 3px; word-break: break-all; }
.line-msg { color: #c0392b; font-size: 0.85rem; }
.hint pre {
  background: #f4f4f4; border: 1px solid #ddd; border-radius: 4px;
  padding: 0.75rem; font-size: 0.85rem; overflow-x: auto;
}
.hint h3 { font-size: 0.9rem; margin: 1rem 0 0.5rem; }
code { background: #eee; padding: 1px 5px; border-radius: 3px; font-size: 0.85rem; }
</style>
