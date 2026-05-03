<template>
  <div class="table-detail">
    <div class="header">
      <RouterLink to="/tables">← テーブル一覧に戻る</RouterLink>
      <h1>{{ displayTableName }}</h1>
    </div>

    <div v-if="userRole !== 'VIEWER'" class="actions">
      <button class="btn-add" @click="toggleAddForm">
        {{ showAddForm ? 'キャンセル' : '+ 新規追加' }}
      </button>
      <button class="btn-edit-mode" :class="{ active: editMode }" @click="toggleEditMode">
        {{ editMode ? '選択キャンセル' : '編集' }}
      </button>
    </div>

    <!-- 新規追加フォーム -->
    <div v-if="showAddForm" class="add-form">
      <h3>新規行の追加</h3>
      <div class="add-form-fields">
        <div v-for="col in editableColumns" :key="col" class="form-field">
          <label>{{ colLabel(col) }}</label>
          <input v-model="newRow[col]" :placeholder="colLabel(col)" />
        </div>
      </div>
      <button class="btn-save" @click="insertRow">追加</button>
    </div>

    <p v-if="loading">読み込み中...</p>
    <p v-else-if="error" style="color:red;">{{ error }}</p>
    <template v-else>
      <!-- 検索バー -->
      <div class="search-bar">
        <input
          v-model="searchInput"
          class="search-input"
          placeholder="キーワードで検索..."
          @keyup.enter="doSearch"
        />
        <button class="btn-search" @click="doSearch">検索</button>
        <button v-if="search" class="btn-clear" @click="clearSearch">クリア</button>
        <span v-if="search" class="search-label">「{{ search }}」で絞り込み中</span>
      </div>

      <div class="table-meta">
        <span class="count">全 {{ total }} 件</span>
        <label class="page-size-label">
          表示件数:
          <select v-model="size" @change="onSizeChange">
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
            <option :value="100">100</option>
          </select>
        </label>
      </div>

      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th v-for="col in columns" :key="col" class="sortable-th" @click="toggleSort(col)">
                {{ colLabel(col) }}
                <span class="sort-icon">
                  <span :class="sortCol === col && sortDir === 'asc' ? 'active' : ''">▲</span>
                  <span :class="sortCol === col && sortDir === 'desc' ? 'active' : ''">▼</span>
                </span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in rows" :key="i"
                :class="{ editing: editingIndex === i, selectable: editMode }"
                @click="editMode ? selectRowForEdit(row) : null">
              <td v-for="col in columns" :key="col">
                <template v-if="editMode && col === pkColumn">
                  {{ row[col] }}
                </template>
                <template v-else-if="editingIndex === i && col !== pkColumn">
                  <input v-model="editRow[col]" class="edit-input" />
                </template>
                <template v-else>{{ row[col] }}</template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ページネーション -->
      <div class="pagination">
        <button :disabled="page === 0" @click="goPage(page - 1)">← 前</button>
        <span class="page-info">{{ page + 1 }} / {{ totalPages }} ページ</span>
        <button :disabled="page >= totalPages - 1" @click="goPage(page + 1)">次 →</button>
      </div>
    </template>
  </div>
</template>

<script>
import axios from '../api'
import { getUser } from '../utils/auth'
import { tableLabel, colLabel } from '../utils/labels'

export default {
  name: 'TableDetailView',
  data() {
    return {
      columns: [],
      rows: [],
      loading: true,
      error: null,
      userRole: getUser()?.role || 'VIEWER',
      page: 0,
      size: 20,
      total: 0,
      totalPages: 1,
      pkColumn: 'ID',
      editingIndex: null,
      editRow: {},
      showAddForm: false,
      newRow: {},
      editMode: false,
      selectedRowId: null,
      searchInput: '',
      search: '',
      sortCol: '',
      sortDir: 'asc',
    }
  },
  computed: {
    tableName() {
      return this.$route.params.name
    },
    editableColumns() {
      return this.columns.filter(col => col !== this.pkColumn)
    },
    displayTableName() {
      return tableLabel(this.tableName)
    },
  },
  async mounted() {
    await this.fetchData()
  },
  methods: {
    colLabel(col) { return colLabel(this.tableName, col) },
    async fetchData() {
      this.loading = true
      this.error = null
      try {
        const params = { page: this.page, size: this.size }
        if (this.search) params.search = this.search
        if (this.sortCol) { params.sortCol = this.sortCol; params.sortDir = this.sortDir }
        const res = await axios.get(`/api/tables/${this.tableName}`, { params })
        this.columns = res.data.columns
        this.rows = res.data.rows
        this.total = res.data.total
        this.totalPages = res.data.totalPages
        if (this.columns.length > 0) this.pkColumn = this.columns[0]
      } catch (e) {
          this.error = 'データの取得に失敗しました: ' + e.message
      } finally {
        this.loading = false
      }
    },
    doSearch() {
      this.search = this.searchInput.trim()
      this.page = 0
      this.fetchData()
    },
    clearSearch() {
      this.searchInput = ''
      this.search = ''
      this.page = 0
      this.fetchData()
    },
    toggleSort(col) {
      if (this.sortCol === col) {
        if (this.sortDir === 'asc') {
          this.sortDir = 'desc'
        } else {
          this.sortCol = ''
          this.sortDir = 'asc'
        }
      } else {
        this.sortCol = col
        this.sortDir = 'asc'
      }
      this.page = 0
      this.fetchData()
    },
    goPage(p) {
      this.page = p
      this.fetchData()
    },
    onSizeChange() {
      this.page = 0
      this.fetchData()
    },
    toggleAddForm() {
      this.showAddForm = !this.showAddForm
      this.newRow = {}
    },
    toggleEditMode() {
      this.editMode = !this.editMode
      this.selectedRowId = null
    },
    selectRowForEdit(row) {
      this.selectedRowId = row[this.pkColumn]
      sessionStorage.setItem('editRowData', JSON.stringify({
        row: JSON.parse(JSON.stringify(row)),
        pkColumn: this.pkColumn,
        columns: this.columns,
      }))
      this.$router.push({
        name: 'table-row-edit',
        params: { name: this.tableName, id: this.selectedRowId },
      })
    },
    startEdit(row, index) {
      this.editingIndex = index
      this.editRow = { ...row }
    },
    cancelEdit() {
      this.editingIndex = null
      this.editRow = {}
    },
    async updateRow(row) {
      const id = row[this.pkColumn]
      const body = { ...this.editRow }
      delete body[this.pkColumn]
      try {
        await axios.put(`/api/tables/${this.tableName}/rows/${id}`, body)
        this.cancelEdit()
        await this.fetchData()
      } catch (e) {
        alert('更新に失敗しました: ' + e.message)
      }
    },
    async deleteRow(row) {
      if (!confirm('この行を削除しますか？')) return
      const id = row[this.pkColumn]
      try {
        await axios.delete(`/api/tables/${this.tableName}/rows/${id}`)
        await this.fetchData()
      } catch (e) {
        alert('削除に失敗しました: ' + e.message)
      }
    },
    async insertRow() {
      try {
        await axios.post(`/api/tables/${this.tableName}/rows`, this.newRow)
        this.newRow = {}
        this.showAddForm = false
        await this.fetchData()
      } catch (e) {
        alert('追加に失敗しました: ' + e.message)
      }
    },
  },
}
</script>

<style scoped>
.table-detail { padding: 2rem; }
.header { margin-bottom: 1rem; }
.header a { color: #4a7fc1; text-decoration: none; font-size: 0.9rem; }
.header h1 { margin: 0.5rem 0 0; }
.actions { display: flex; gap: 0.5rem; margin-bottom: 1rem; }
.btn-add {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 0.9rem;
  cursor: pointer;
  border: none;
}
.btn-add { background: #4a9e6b; color: white; }
.btn-edit-mode { padding: 6px 14px; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem; background: #f0a500; color: white; }
.btn-edit-mode.active { background: #999; }
tr.selectable { cursor: pointer; }
tr.selectable:hover { background-color: #fff3cd !important; }

.add-form {
  background: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 1rem 1.5rem;
  margin-bottom: 1rem;
}
.add-form h3 { margin: 0 0 0.75rem; font-size: 0.95rem; }
.add-form-fields { display: flex; flex-wrap: wrap; gap: 0.5rem 1rem; margin-bottom: 0.75rem; }
.form-field { display: flex; flex-direction: column; gap: 2px; }
.form-field label { font-size: 0.75rem; color: #666; }
.form-field input { padding: 4px 8px; border: 1px solid #ccc; border-radius: 4px; font-size: 0.9rem; }

/* 検索バー */
.search-bar {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}
.search-input {
  padding: 6px 12px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
  width: 280px;
}
.btn-search {
  padding: 6px 14px;
  background: #4a7fc1;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
}
.btn-clear {
  padding: 6px 14px;
  background: #aaa;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
}
.search-label {
  font-size: 0.85rem;
  color: #e67e22;
}

.table-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
}
.count { color: #666; font-size: 0.9rem; }
.page-size-label { font-size: 0.85rem; color: #555; }
.page-size-label select { margin-left: 0.25rem; padding: 2px 6px; border-radius: 4px; border: 1px solid #ccc; }

.table-wrapper { overflow-x: auto; overflow-y: auto; max-height: calc(100vh - 280px); }
table { width: 100%; border-collapse: collapse; }
th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; white-space: pre-wrap; }
th {
  background-color: #3a5a8a;
  color: white;
  position: sticky;
  top: 0;
  z-index: 1;
}
tbody tr:nth-child(odd)  { background-color: #ffffff; }
tbody tr:nth-child(even) { background-color: #e8f4fc; }
tbody tr:hover { background-color: #cce8f8; }
tr.editing { background-color: #fffbea; }

/* ソートヘッダー */
.sortable-th {
  cursor: pointer;
  user-select: none;
  white-space: nowrap;
}
.sortable-th:hover { background-color: #e8e8e8; }
.sort-icon {
  display: inline-flex;
  flex-direction: column;
  font-size: 0.6rem;
  line-height: 1;
  margin-left: 4px;
  vertical-align: middle;
  color: #bbb;
}
.sort-icon span.active { color: #4a7fc1; font-weight: bold; }

.edit-input {
  width: 100%;
  padding: 2px 6px;
  border: 1px solid #aaa;
  border-radius: 3px;
  font-size: 0.9rem;
  box-sizing: border-box;
}
.btn-save, .btn-cancel {
  padding: 3px 10px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.8rem;
  margin: 0 2px;
}
.btn-save   { background: #4a9e6b; color: white; }
.btn-cancel { background: #999; color: white; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 1rem;
}
.pagination button {
  padding: 4px 14px;
  border: 1px solid #ccc;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  font-size: 0.9rem;
}
.pagination button:disabled { color: #aaa; cursor: default; }
.page-info { font-size: 0.9rem; color: #555; }
</style>
