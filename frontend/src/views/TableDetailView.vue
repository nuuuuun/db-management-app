<template>
  <div class="table-detail">
    <div class="header">
      <RouterLink to="/tables">← テーブル一覧に戻る</RouterLink>
      <h1>{{ displayTableName }}</h1>
    </div>

    <div class="actions">
      <template v-if="userRole !== 'VIEWER'">
        <button class="btn-add" @click="toggleAddForm">
          {{ showAddForm ? 'キャンセル' : '+ 新規追加' }}
        </button>
        <button class="btn-edit-mode" :class="{ active: editMode }" @click="toggleEditMode">
          {{ editMode ? '選択キャンセル' : '編集' }}
        </button>
      </template>
      <button class="btn-excel" @click="downloadExcel">Excelダウンロード</button>
    </div>

    <!-- 新規追加フォーム -->
    <div v-if="showAddForm" class="add-form">
      <h3>新規行の追加</h3>
      <div class="add-form-fields">
        <div v-for="col in editableColumns" :key="col" class="form-field">
          <label>{{ colLabel(col) }}</label>
          <select v-if="fieldDef(col).type === 'select'" v-model="newRow[col]">
            <option value="">-- 選択 --</option>
            <option v-for="opt in fieldDef(col).options" :key="opt" :value="opt">{{ opt }}</option>
          </select>
          <input v-else-if="fieldDef(col).type === 'date'" type="date" v-model="newRow[col]" />
          <input v-else v-model="newRow[col]" :placeholder="colLabel(col)" />
        </div>
      </div>
      <button class="btn-save" @click="insertRow">追加</button>
    </div>

    <p v-if="loading">読み込み中...</p>
    <p v-else-if="error" style="color:red;">{{ error }}</p>
    <template v-else>
      <!-- 検索バー -->
      <div class="search-bar">
        <select v-model="filterCol" class="filter-col-select">
          <option value="">全カラム</option>
          <option v-for="col in columns" :key="col" :value="col">{{ colLabel(col) }}</option>
        </select>
        <input
          v-model="searchInput"
          class="search-input"
          placeholder="キーワードで検索..."
          @keyup.enter="doSearch"
        />
        <button class="btn-search" @click="doSearch">検索</button>
        <button v-if="search" class="btn-clear" @click="clearSearch">クリア</button>
        <span v-if="search" class="search-label">
          「{{ search }}」{{ filterCol ? `（${colLabel(filterCol)}）` : '' }}で絞り込み中
        </span>
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
                <template v-if="editMode && col === pkColumn">{{ row[col] }}</template>
                <template v-else-if="editingIndex === i && col !== pkColumn">
                  <input v-model="editRow[col]" class="edit-input" />
                </template>
                <template v-else-if="!editMode && columnLink(col) && row[col]">
                  <a :href="getLinkedHref(col, row[col])" target="_blank" rel="noopener noreferrer" class="cell-link">{{ row[col] }}</a>
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
import { extractError } from '../utils/error'
import { getUser } from '../utils/auth'
import { tableLabel, colLabel, fieldDef, columnLink } from '../utils/labels'

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
      filterCol: '',
      sortCol: '',
      sortDir: 'asc',
    }
  },
  computed: {
    tableName() { return this.$route.params.name },
    displayTableName() { return tableLabel(this.tableName) },
    editableColumns() { return this.columns.filter(col => col !== this.pkColumn) },
  },
  async mounted() {
    if (this.$route.query.search) {
      this.searchInput = this.$route.query.search
      this.search = this.$route.query.search
    }
    if (this.$route.query.filterCol) {
      this.filterCol = this.$route.query.filterCol
    }
    await this.fetchData()
  },
  methods: {
    colLabel(col) { return colLabel(this.tableName, col) },
    fieldDef(col) { return fieldDef(this.tableName, col) },
    columnLink(col) { return columnLink(this.tableName, col) },
    getLinkedRoute(col, value) {
      const link = columnLink(this.tableName, col)
      if (!link || !value) return null
      return { path: `/tables/${link.table}`, query: { search: value, filterCol: link.filterCol } }
    },
    getLinkedHref(col, value) {
      const link = columnLink(this.tableName, col)
      if (!link || !value) return null
      const params = new URLSearchParams({ search: value, filterCol: link.filterCol })
      return `/tables/${link.table}?${params.toString()}`
    },
    async fetchData() {
      this.loading = true
      this.error = null
      try {
        const params = { page: this.page, size: this.size }
        if (this.search) {
          params.search = this.search
          if (this.filterCol) params.filterCol = this.filterCol
        }
        if (this.sortCol) { params.sortCol = this.sortCol; params.sortDir = this.sortDir }
        const res = await axios.get(`/api/tables/${this.tableName}`, { params })
        this.columns = res.data.columns
        this.rows = res.data.rows
        this.total = res.data.total
        this.totalPages = res.data.totalPages
        if (this.columns.length > 0) this.pkColumn = this.columns[0]
      } catch (e) {
        this.error = 'データの取得に失敗しました: ' + extractError(e)
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
      this.filterCol = ''
      this.page = 0
      this.fetchData()
    },
    toggleSort(col) {
      if (this.sortCol === col) {
        if (this.sortDir === 'asc') { this.sortDir = 'desc' }
        else { this.sortCol = ''; this.sortDir = 'asc' }
      } else {
        this.sortCol = col; this.sortDir = 'asc'
      }
      this.page = 0
      this.fetchData()
    },
    goPage(p) { this.page = p; this.fetchData() },
    onSizeChange() { this.page = 0; this.fetchData() },
    toggleAddForm() { this.showAddForm = !this.showAddForm; this.newRow = {} },
    toggleEditMode() { this.editMode = !this.editMode; this.selectedRowId = null },
    selectRowForEdit(row) {
      this.selectedRowId = row[this.pkColumn]
      sessionStorage.setItem('editRowData', JSON.stringify({
        row: JSON.parse(JSON.stringify(row)),
        pkColumn: this.pkColumn,
        columns: this.columns,
      }))
      this.$router.push({ name: 'table-row-edit', params: { name: this.tableName, id: this.selectedRowId } })
    },
    cancelEdit() { this.editingIndex = null; this.editRow = {} },
    async insertRow() {
      try {
        await axios.post(`/api/tables/${this.tableName}/rows`, this.newRow)
        this.newRow = {}
        this.showAddForm = false
        await this.fetchData()
      } catch (e) {
        alert('追加に失敗しました: ' + extractError(e))
      }
    },
    async downloadExcel() {
      try {
        const params = {}
        if (this.search) {
          params.search = this.search
          if (this.filterCol) params.filterCol = this.filterCol
        }
        const res = await axios.get(`/api/export/excel/${this.tableName}`, {
          params,
          responseType: 'blob',
        })
        const url = URL.createObjectURL(res.data)
        const a = document.createElement('a')
        a.href = url
        a.download = `${this.displayTableName}.xlsx`
        a.click()
        URL.revokeObjectURL(url)
      } catch (e) {
        alert('Excelダウンロードに失敗しました: ' + extractError(e))
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
.btn-add { padding: 6px 14px; border-radius: 4px; font-size: 0.9rem; cursor: pointer; border: none; background: #4a9e6b; color: white; }
.btn-edit-mode { padding: 6px 14px; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem; background: #f0a500; color: white; }
.btn-edit-mode.active { background: #999; }
.btn-excel { padding: 6px 14px; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem; background: #217346; color: white; }
tr.selectable { cursor: pointer; }
tr.selectable:hover { background-color: #fff3cd !important; }

/* 新規追加フォーム */
.add-form { background: #f9f9f9; border: 1px solid #ddd; border-radius: 6px; padding: 1rem 1.5rem; margin-bottom: 1rem; }
.add-form h3 { margin: 0 0 0.75rem; font-size: 0.95rem; }
.add-form-fields { display: flex; flex-wrap: wrap; gap: 0.5rem 1rem; margin-bottom: 0.75rem; }
.form-field { display: flex; flex-direction: column; gap: 2px; min-width: 160px; }
.form-field label { font-size: 0.75rem; color: #666; }
.form-field input, .form-field select {
  padding: 4px 8px; border: 1px solid #ccc; border-radius: 4px;
  font-size: 0.9rem; background: white;
}

/* 検索バー */
.search-bar { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.75rem; flex-wrap: wrap; }
.filter-col-select { padding: 6px 8px; border: 1px solid #ccc; border-radius: 4px; font-size: 0.9rem; background: white; min-width: 120px; }
.search-input { padding: 6px 12px; border: 1px solid #ccc; border-radius: 4px; font-size: 0.9rem; width: 220px; }
.btn-search { padding: 6px 14px; background: #4a7fc1; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem; }
.btn-clear { padding: 6px 14px; background: #aaa; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem; }
.search-label { font-size: 0.85rem; color: #e67e22; }

.table-meta { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.5rem; }
.count { color: #666; font-size: 0.9rem; }
.page-size-label { font-size: 0.85rem; color: #555; }
.page-size-label select { margin-left: 0.25rem; padding: 2px 6px; border-radius: 4px; border: 1px solid #ccc; }

.table-wrapper { overflow-x: auto; overflow-y: auto; max-height: calc(100vh - 300px); }
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

.sortable-th { cursor: pointer; user-select: none; white-space: nowrap; }
.sortable-th:hover { background-color: #2e4a7a; }
.sort-icon { display: inline-flex; flex-direction: column; font-size: 0.6rem; line-height: 1; margin-left: 4px; vertical-align: middle; color: rgba(255,255,255,0.4); }
.sort-icon span.active { color: #fff; font-weight: bold; }

.edit-input { width: 100%; padding: 2px 6px; border: 1px solid #aaa; border-radius: 3px; font-size: 0.9rem; box-sizing: border-box; }
.btn-save { padding: 3px 10px; border: none; border-radius: 4px; cursor: pointer; font-size: 0.8rem; margin: 0 2px; background: #4a9e6b; color: white; }

.pagination { display: flex; align-items: center; justify-content: center; gap: 1rem; margin-top: 1rem; }
.pagination button { padding: 4px 14px; border: 1px solid #ccc; border-radius: 4px; background: white; cursor: pointer; font-size: 0.9rem; }
.pagination button:disabled { color: #aaa; cursor: default; }
.page-info { font-size: 0.9rem; color: #555; }
.cell-link { color: #1a6bbf; text-decoration: underline; cursor: pointer; }
.cell-link:hover { color: #0d47a1; }
</style>
