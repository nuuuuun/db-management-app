<template>
  <div class="users">
    <h1>ユーザー一覧</h1>
    <p v-if="loading">読み込み中...</p>
    <p v-else-if="error" style="color: red;">{{ error }}</p>
    <p v-else-if="users.length === 0">ユーザーが登録されていません。</p>
    <table v-else>
      <thead>
        <tr>
          <th>ID</th>
          <th>ユーザー名</th>
          <th>メール</th>
          <th>ロール</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in users" :key="user.id">
          <td>{{ user.id }}</td>
          <td>{{ user.username }}</td>
          <td>{{ user.email }}</td>
          <td>{{ user.role }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'UsersView',
  data() {
    return {
      users: [],
      loading: true,
      error: null,
    }
  },
  async mounted() {
    try {
      const response = await axios.get('/api/users')
      this.users = response.data
    } catch (e) {
      this.error = 'APIの取得に失敗しました: ' + e.message
    } finally {
      this.loading = false
    }
  },
}
</script>

<style scoped>
.users {
  padding: 2rem;
}
table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1rem;
}
th, td {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}
th {
  background-color: #f4f4f4;
}
tr:hover {
  background-color: #f9f9f9;
}
</style>
