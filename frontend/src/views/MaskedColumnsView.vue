<template>
  <div class="masked-columns">
    <h1>カラムマスキング設定</h1>
    <p class="desc">特定のカラムをロール別に非表示にします。EDITOR に設定するとEDITOR・VIEWERの両方に適用されます。</p>

    <div class="form-section">
      <h2>新規追加</h2>
      <form @submit.prevent="addMasking">
        <select v-model="form.tableName" @change="loadColumns" required>
          <option value="">テーブルを選択</option>
          <option v-for="t in tables" :key="t" :value="t">{{ t }}</option>
        </select>
        <select v-model="form.columnName" required :disabled="!form.tableName">
          <option value="">カラムを選択</option>
          <option v-for="c in columns" :key="c" :value="c">{{ c }}</option>
        </select>
        <select v-model="form.role" required>
          <option value="">非表示にするロール</option>
          <option value="VIEWER">VIEWER（閲覧者）</option>
          <option value="EDITOR">EDITOR（編集者・閲覧者）</option>
        </select>
        <button type="submit">追加</button>
      </form>
    </div>

    <h2>現在の設定</h2>
    <p v-if="maskedColumns.length === 0">設定がありません</p>
    <table v-else>
      <thead>
        <tr>
          <th>テーブル</th>
          <th>カラム</th>
          <th>非表示ロール</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="mc in maskedColumns" :key="mc.id">
          <td>{{ mc.tableName }}</td>
          <td>{{ mc.columnName }}</td>
          <td>{{ mc.role }}</td>
          <td>
            <button class="btn-delete" @click="deleteMasking(mc.id)">削除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import api from '../api'

export default {
  name: 'MaskedColumnsView',
  data() {
    return {
      tables: [],
      columns: [],
      maskedColumns: [],
      form: { tableName: '', columnName: '', role: '' },
    }
  },
  async mounted() {
    await Promise.all([this.loadTables(), this.loadMaskedColumns()])
  },
  methods: {
    async loadTables() {
      const res = await api.get('/api/tables')
      this.tables = res.data
    },
    async loadColumns() {
      this.form.columnName = ''
      if (!this.form.tableName) { this.columns = []; return }
      const res = await api.get(`/api/tables/${this.form.tableName}/columns`)
      this.columns = res.data
    },
    async loadMaskedColumns() {
      const res = await api.get('/api/masked-columns')
      this.maskedColumns = res.data
    },
    async addMasking() {
      await api.post('/api/masked-columns', this.form)
      this.form = { tableName: '', columnName: '', role: '' }
      this.columns = []
      await this.loadMaskedColumns()
    },
    async deleteMasking(id) {
      if (!confirm('削除しますか？')) return
      await api.delete(`/api/masked-columns/${id}`)
      await this.loadMaskedColumns()
    },
  },
}
</script>

<style scoped>
.masked-columns { padding: 2rem; }
.desc { color: #666; font-size: 0.9rem; margin-bottom: 1.5rem; }
.form-section {
  background: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 1.5rem;
  margin-bottom: 2rem;
}
.form-section h2 { margin-top: 0; font-size: 1rem; }
form { display: flex; gap: 0.5rem; flex-wrap: wrap; align-items: center; }
select, input {
  padding: 6px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
}
button {
  padding: 6px 14px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  background: #4a9e6b;
  color: white;
}
.btn-delete { background: #d9534f; }
table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }
th { background: #f4f4f4; }
</style>
