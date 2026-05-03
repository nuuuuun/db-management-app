<template>
  <div class="csv-import">
    <div class="header">
      <RouterLink :to="`/tables/${tableName}`">← テーブルに戻る</RouterLink>
      <h1>{{ tableName }} - CSVインポート</h1>
    </div>

    <div class="form-section">
      <div class="field">
        <label>インポートモード</label>
        <select v-model="mode">
          <option value="append">追記（既存データ保持）</option>
          <option value="overwrite">上書き（既存データを全削除してから挿入）</option>
        </select>
      </div>
      <div class="field">
        <label>CSVファイル（1行目をヘッダーとして扱います）</label>
        <input type="file" accept=".csv" @change="onFileChange" />
      </div>
      <button @click="doImport" :disabled="!file || loading">
        {{ loading ? 'インポート中...' : 'インポート実行' }}
      </button>
    </div>

    <div v-if="result" class="result">
      <p class="success">✅ {{ result.inserted }} 件インポートしました</p>
      <div v-if="result.errors.length > 0" class="errors">
        <p>⚠️ エラー {{ result.errors.length }} 件:</p>
        <ul>
          <li v-for="e in result.errors" :key="e">{{ e }}</li>
        </ul>
      </div>
    </div>

    <div v-if="error" class="error">{{ error }}</div>

    <div class="hint">
      <h3>CSVフォーマット例</h3>
      <pre>USERNAME,EMAIL,ROLE
yamada,yamada@example.com,VIEWER
suzuki,suzuki@example.com,EDITOR</pre>
    </div>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'CsvImportView',
  data() {
    return {
      file: null,
      mode: 'append',
      loading: false,
      result: null,
      error: null,
    }
  },
  computed: {
    tableName() { return this.$route.params.name },
  },
  methods: {
    onFileChange(e) {
      this.file = e.target.files[0]
      this.result = null
      this.error = null
    },
    async doImport() {
      if (!this.file) return
      this.loading = true
      this.result = null
      this.error = null
      try {
        const formData = new FormData()
        formData.append('file', this.file)
        formData.append('mode', this.mode)
        const res = await api.post(`/api/import/${this.tableName}?mode=${this.mode}`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        this.result = res.data
      } catch (e) {
        this.error = e.response?.data?.error || 'インポートに失敗しました'
      } finally {
        this.loading = false
      }
    },
  },
}
</script>

<style scoped>
.csv-import { padding: 2rem; }
.header { margin-bottom: 1.5rem; }
.header a { color: #4a7fc1; text-decoration: none; font-size: 0.9rem; }
.header h1 { margin: 0.5rem 0 0; }
.form-section {
  background: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}
.field { margin-bottom: 1rem; }
label { display: block; margin-bottom: 4px; font-size: 0.9rem; color: #555; }
select, input[type="file"] {
  padding: 6px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
}
button {
  padding: 8px 20px;
  background: #4a9e6b;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.95rem;
}
button:disabled { background: #aaa; cursor: default; }
.result { margin-bottom: 1rem; }
.success { color: #2a7a4a; font-weight: bold; }
.errors { background: #fff8e1; border: 1px solid #ffc107; border-radius: 4px; padding: 1rem; margin-top: 0.5rem; }
.error { color: red; }
.hint { margin-top: 2rem; }
.hint h3 { font-size: 0.95rem; margin-bottom: 0.5rem; }
pre {
  background: #f4f4f4;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 0.75rem;
  font-size: 0.85rem;
}
</style>
