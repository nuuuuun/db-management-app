<template>
  <div class="tables">
    <div class="tables-header">
      <h1>テーブル一覧</h1>
      <button class="btn-excel-all" @click="downloadAllExcel">全テーブル Excelダウンロード</button>
    </div>
    <p v-if="loading">読み込み中...</p>
    <p v-else-if="error" style="color:red;">{{ error }}</p>
    <ul v-else class="table-list">
      <li v-for="table in tables" :key="table">
        <RouterLink :to="`/tables/${table}`">{{ tableLabel(table) }}</RouterLink>
      </li>
    </ul>
  </div>
</template>

<script>
import axios from '../api'
import { extractError } from '../utils/error'
import { tableLabel } from '../utils/labels'

export default {
  name: 'TablesView',
  data() {
    return { tables: [], loading: true, error: null }
  },
  methods: {
    tableLabel,
    async downloadAllExcel() {
      try {
        const res = await axios.get('/api/export/excel/all', { responseType: 'blob' })
        const url = URL.createObjectURL(res.data)
        const a = document.createElement('a')
        a.href = url
        a.download = 'db-export-all.xlsx'
        a.click()
        URL.revokeObjectURL(url)
      } catch (e) {
        alert('Excelダウンロードに失敗しました: ' + extractError(e))
      }
    },
  },
  async mounted() {
    try {
      const res = await axios.get('/api/tables')
      this.tables = res.data
    } catch (e) {
      this.error = 'テーブル一覧の取得に失敗しました: ' + extractError(e)
    } finally {
      this.loading = false
    }
  },
}
</script>

<style scoped>
.tables { padding: 2rem; }
.tables-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1rem; }
.tables-header h1 { margin: 0; }
.btn-excel-all {
  padding: 8px 18px; border: none; border-radius: 4px; cursor: pointer;
  font-size: 0.9rem; background: #217346; color: white;
}
.btn-excel-all:hover { background: #1a5c38; }
.table-list {
  list-style: none;
  padding: 0;
  margin-top: 1rem;
}
.table-list li {
  margin-bottom: 0.5rem;
}
.table-list a {
  display: inline-block;
  padding: 8px 16px;
  background: #f4f4f4;
  border: 1px solid #ddd;
  border-radius: 4px;
  text-decoration: none;
  color: #333;
  font-weight: bold;
  min-width: 200px;
}
.table-list a:hover {
  background: #e0e0e0;
}
</style>
