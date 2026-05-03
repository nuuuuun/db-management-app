<template>
  <div class="row-confirm">
    <div class="header">
      <h1>{{ displayTableName }} - 編集内容の確認</h1>
      <p class="pk-info">{{ pkColumn }}: {{ rowId }}</p>
    </div>

    <div v-if="!originalRow" class="error">データが見つかりません。テーブル一覧から再度選択してください。</div>
    <template v-else>
      <table class="confirm-table">
        <thead>
          <tr>
            <th>カラム</th>
            <th>変更前</th>
            <th>変更後</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="col in editableColumns" :key="col" :class="{ changed: originalRow[col] !== editedRow[col] }">
            <td class="col-name">{{ colLabel(col) }}</td>
            <td class="old-val">{{ originalRow[col] }}</td>
            <td class="new-val">{{ editedRow[col] }}</td>
          </tr>
        </tbody>
      </table>

      <p class="changed-note" v-if="hasChanges">※ 黄色のハイライトは変更があるカラムです</p>
      <p class="no-change" v-else>変更はありません</p>

      <div class="form-actions">
        <button class="btn-back" @click="$router.back()">戻る</button>
        <button class="btn-submit" @click="submit" :disabled="loading || !hasChanges">
          {{ loading ? '保存中...' : '確定' }}
        </button>
      </div>
    </template>
  </div>
</template>

<script>
import axios from '../api'
import { tableLabel, colLabel } from '../utils/labels'

export default {
  name: 'TableRowEditConfirmView',
  data() {
    const stored = sessionStorage.getItem('editConfirmData')
    const state = stored ? JSON.parse(stored) : {}
    return {
      originalRow: state.originalRow || null,
      editedRow: state.editedRow || {},
      pkColumn: state.pkColumn || 'ID',
      columns: state.columns || [],
      loading: false,
    }
  },
  computed: {
    tableName() { return this.$route.params.name },
    rowId() { return this.$route.params.id },
    displayTableName() { return tableLabel(this.tableName) },
    editableColumns() {
      return this.columns.filter(col => col !== this.pkColumn)
    },
    hasChanges() {
      return this.editableColumns.some(col => this.originalRow[col] !== this.editedRow[col])
    },
  },
  methods: {
    colLabel(col) { return colLabel(this.tableName, col) },
    async submit() {
      this.loading = true
      const body = {}
      this.editableColumns.forEach(col => { body[col] = this.editedRow[col] })
      try {
        await axios.put(`/api/tables/${this.tableName}/rows/${this.rowId}`, body)
        this.$router.push(`/tables/${this.tableName}`)
      } catch (e) {
        alert('保存に失敗しました: ' + e.message)
        this.loading = false
      }
    },
  },
}
</script>

<style scoped>
.row-confirm { padding: 2rem; max-width: 800px; }
.header { margin-bottom: 1.5rem; }
.header h1 { margin: 0 0 0.25rem; }
.pk-info { margin: 0; color: #666; font-size: 0.9rem; }
.confirm-table { width: 100%; border-collapse: collapse; margin-bottom: 0.75rem; }
.confirm-table th, .confirm-table td { border: 1px solid #ddd; padding: 8px 14px; text-align: left; }
.confirm-table th { background: #f4f4f4; font-size: 0.9rem; }
.col-name { font-weight: bold; color: #444; width: 180px; }
.old-val { color: #888; }
.new-val { color: #2a6ebb; font-weight: bold; }
tr.changed { background-color: #fffbea; }
.changed-note { font-size: 0.85rem; color: #888; margin: 0 0 1rem; }
.no-change { color: #999; margin-bottom: 1rem; }
.form-actions { display: flex; gap: 1rem; }
.btn-back { padding: 8px 24px; background: #999; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 0.95rem; }
.btn-submit { padding: 8px 24px; background: #4a9e6b; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 0.95rem; }
.btn-submit:disabled { background: #aaa; cursor: default; }
.error { color: red; }
</style>
