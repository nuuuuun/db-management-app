<template>
  <div class="users">
    <h1>ユーザー一覧</h1>

    <!-- 追加フォーム（VIEWER は非表示） -->
    <div v-if="userRole !== 'VIEWER'" class="form-section">
      <h2>{{ editingUser ? 'ユーザー編集' : 'ユーザー追加' }}</h2>
      <form @submit.prevent="editingUser ? updateUser() : createUser()">
        <input v-model="form.username" placeholder="ユーザー名" required />
        <input v-model="form.email" placeholder="メールアドレス" type="email" required />
        <select v-model="form.role" required>
          <option value="">ロールを選択</option>
          <option value="ADMIN">ADMIN</option>
          <option value="EDITOR">EDITOR</option>
          <option value="VIEWER">VIEWER</option>
        </select>
        <button type="submit">{{ editingUser ? '更新' : '追加' }}</button>
        <button v-if="editingUser" type="button" @click="cancelEdit">キャンセル</button>
      </form>
    </div>

    <!-- 一覧テーブル -->
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
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="user in users" :key="user.id">
          <td>{{ user.id }}</td>
          <td>{{ user.username }}</td>
          <td>{{ user.email }}</td>
          <td>{{ user.role }}</td>
          <td>
            <button v-if="userRole !== 'VIEWER'" class="btn-edit" @click="startEdit(user)">編集</button>
            <button v-if="userRole === 'ADMIN'" class="btn-delete" @click="deleteUser(user.id)">削除</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import axios from '../api'
import { getUser } from '../utils/auth'

export default {
  name: 'UsersView',
  data() {
    return {
      users: [],
      loading: true,
      error: null,
      editingUser: null,
      form: { username: '', email: '', role: '' },
      userRole: getUser()?.role || 'VIEWER',
    }
  },
  async mounted() {
    await this.fetchUsers()
  },
  methods: {
    async fetchUsers() {
      this.loading = true
      try {
        const response = await axios.get('/api/users')
        this.users = response.data
      } catch (e) {
        this.error = 'APIの取得に失敗しました: ' + e.message
      } finally {
        this.loading = false
      }
    },
    async createUser() {
      try {
        await axios.post('/api/users', this.form)
        this.form = { username: '', email: '', role: '' }
        await this.fetchUsers()
      } catch (e) {
        alert('追加に失敗しました: ' + e.message)
      }
    },
    startEdit(user) {
      this.editingUser = user
      this.form = { username: user.username, email: user.email, role: user.role }
    },
    async updateUser() {
      try {
        await axios.put(`/api/users/${this.editingUser.id}`, this.form)
        this.cancelEdit()
        await this.fetchUsers()
      } catch (e) {
        alert('更新に失敗しました: ' + e.message)
      }
    },
    cancelEdit() {
      this.editingUser = null
      this.form = { username: '', email: '', role: '' }
    },
    async deleteUser(id) {
      if (!confirm('本当に削除しますか？')) return
      try {
        await axios.delete(`/api/users/${id}`)
        await this.fetchUsers()
      } catch (e) {
        alert('削除に失敗しました: ' + e.message)
      }
    },
  },
}
</script>

<style scoped>
.users {
  padding: 2rem;
}
.form-section {
  background: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 1.5rem;
  margin-bottom: 2rem;
}
.form-section h2 {
  margin-top: 0;
  font-size: 1rem;
}
form {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  align-items: center;
}
input, select {
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
}
form button[type="submit"] {
  background: #4a9e6b;
  color: white;
}
form button[type="button"] {
  background: #999;
  color: white;
}
table {
  width: 100%;
  border-collapse: collapse;
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
.btn-edit {
  background: #4a7fc1;
  color: white;
  margin-right: 4px;
}
.btn-delete {
  background: #d9534f;
  color: white;
}
</style>
