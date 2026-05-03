<template>
  <div class="row-edit">
    <div class="header">
      <RouterLink :to="`/tables/${tableName}`">← テーブルに戻る</RouterLink>
      <h1>{{ displayTableName }} - 行編集</h1>
      <p class="pk-info">{{ pkColumn }}: {{ rowId }}</p>
    </div>

    <div v-if="!originalRow" class="error">データが見つかりません。テーブル一覧から再度選択してください。</div>
    <template v-else>
      <div class="form-section">
        <div v-for="col in editableColumns" :key="col" class="form-field">
          <label>{{ colLabel(col) }}</label>
          <input v-model="editedRow[col]" :placeholder="colLabel(col)" />
        </div>
      </div>
      <div class="form-actions">
        <RouterLink :to="`/tables/${tableName}`" class="btn-cancel">キャンセル</RouterLink>
        <button class="btn-confirm" @click="goConfirm">確認</button>
      </div>
    </template>
  </div>
</template>

<script>
import { tableLabel, colLabel } from '../utils/labels'

export default {
  name: 'TableRowEditView',
  data() {
    const stored = sessionStorage.getItem('editRowData')
    const state = stored ? JSON.parse(stored) : {}
    const originalRow = state.row || null
    const pkColumn = state.pkColumn || 'ID'
    const columns = state.columns || []
    return {
      originalRow,
      pkColumn,
      columns,
      editedRow: originalRow ? { ...originalRow } : {},
    }
  },
  computed: {
    tableName() { return this.$route.params.name },
    rowId() { return this.$route.params.id },
    displayTableName() { return tableLabel(this.tableName) },
    editableColumns() {
      return this.columns.filter(col => col !== this.pkColumn)
    },
  },
  methods: {
    colLabel(col) { return colLabel(this.tableName, col) },
  },
  methods: {
    goConfirm() {
      sessionStorage.setItem('editConfirmData', JSON.stringify({
        originalRow: this.originalRow,
        editedRow: this.editedRow,
        pkColumn: this.pkColumn,
        columns: this.columns,
      }))
      this.$router.push({
        name: 'table-row-edit-confirm',
        params: { name: this.tableName, id: this.rowId },
      })
    },
  },
}
</script>

<style scoped>
.row-edit { padding: 2rem; max-width: 800px; }
.header { margin-bottom: 1.5rem; }
.header a { color: #4a7fc1; text-decoration: none; font-size: 0.9rem; }
.header h1 { margin: 0.5rem 0 0.25rem; }
.pk-info { margin: 0; color: #666; font-size: 0.9rem; }
.form-section { background: #f9f9f9; border: 1px solid #ddd; border-radius: 6px; padding: 1.5rem; margin-bottom: 1.5rem; }
.form-field { margin-bottom: 1rem; }
.form-field label { display: block; font-size: 0.85rem; color: #555; margin-bottom: 4px; font-weight: bold; }
.form-field input { width: 100%; padding: 8px 10px; border: 1px solid #ccc; border-radius: 4px; font-size: 0.95rem; box-sizing: border-box; }
.form-actions { display: flex; gap: 1rem; }
.btn-confirm { padding: 8px 24px; background: #4a7fc1; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 0.95rem; }
.btn-cancel { padding: 8px 24px; background: #999; color: white; border-radius: 4px; text-decoration: none; font-size: 0.95rem; }
.error { color: red; }
</style>
