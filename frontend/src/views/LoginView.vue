<template>
  <div class="login">
    <div class="login-box">
      <h1>ログイン</h1>
      <form @submit.prevent="login">
        <div>
          <label>ユーザー名</label>
          <input v-model="form.username" type="text" required />
        </div>
        <div>
          <label>パスワード</label>
          <input v-model="form.password" type="password" required />
        </div>
        <p v-if="error" class="error">{{ error }}</p>
        <button type="submit" :disabled="loading">
          {{ loading ? 'ログイン中...' : 'ログイン' }}
        </button>
      </form>
      <p class="hint">テスト用: admin / password123</p>
    </div>
  </div>
</template>

<script>
import axios from 'axios'
import { setToken, setUser } from '../utils/auth'

export default {
  name: 'LoginView',
  data() {
    return {
      form: { username: '', password: '' },
      loading: false,
      error: null,
    }
  },
  methods: {
    async login() {
      this.loading = true
      this.error = null
      try {
        const res = await axios.post('/api/auth/login', this.form)
        setToken(res.data.token)
        setUser({ username: res.data.username, role: res.data.role })
        this.$router.push('/')
      } catch {
        this.error = 'ユーザー名またはパスワードが正しくありません'
      } finally {
        this.loading = false
      }
    },
  },
}
</script>

<style scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}
.login-box {
  background: #f9f9f9;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 2rem;
  width: 320px;
}
h1 { margin-top: 0; font-size: 1.4rem; }
div { margin-bottom: 1rem; }
label { display: block; margin-bottom: 4px; font-size: 0.9rem; color: #555; }
input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.95rem;
  box-sizing: border-box;
}
button {
  width: 100%;
  padding: 10px;
  background: #4a9e6b;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
}
button:disabled { background: #aaa; cursor: default; }
.error { color: red; font-size: 0.9rem; margin: 0 0 0.5rem; }
.hint { color: #999; font-size: 0.8rem; margin-top: 1rem; text-align: center; }
</style>
