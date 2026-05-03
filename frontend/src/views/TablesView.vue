<template>
  <div class="tables">
    <h1>テーブル一覧</h1>
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
import { tableLabel } from '../utils/labels'

export default {
  name: 'TablesView',
  data() {
    return { tables: [], loading: true, error: null }
  },
  methods: { tableLabel },
  async mounted() {
    try {
      const res = await axios.get('/api/tables')
      this.tables = res.data
    } catch (e) {
      this.error = 'テーブル一覧の取得に失敗しました: ' + e.message
    } finally {
      this.loading = false
    }
  },
}
</script>

<style scoped>
.tables { padding: 2rem; }
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
