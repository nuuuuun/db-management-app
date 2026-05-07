<template>
  <div class="register-page">
    <div class="register-card">
      <h1>アカウント登録</h1>
      <p v-if="successMsg" class="msg-success">{{ successMsg }}</p>
      <p v-if="errorMsg" class="msg-error">{{ errorMsg }}</p>
      <form @submit.prevent="submit">
        <div class="field">
          <label>ユーザー名 <span class="required">*</span></label>
          <input v-model="form.username" type="text" placeholder="例: yamada_taro" required />
        </div>
        <div class="field">
          <label>メールアドレス <span class="required">*</span></label>
          <input v-model="form.email" type="email" placeholder="例: yamada@example.com" required />
        </div>
        <div class="field">
          <label>パスワード <span class="required">*</span></label>
          <input v-model="form.password" type="password" placeholder="8文字以上推奨" required minlength="4" />
        </div>
        <div class="field">
          <label>パスワード（確認） <span class="required">*</span></label>
          <input v-model="form.passwordConfirm" type="password" placeholder="同じパスワードを入力" required />
        </div>
        <div class="field">
          <label>ロール <span class="required">*</span></label>
          <select v-model="form.role" required>
            <option value="">選択してください</option>
            <option value="ADMIN">ADMIN（管理者）</option>
            <option value="EDITOR">EDITOR（編集者）</option>
            <option value="VIEWER">VIEWER（閲覧者）</option>
          </select>
        </div>
        <div class="actions">
          <button type="submit" class="btn-submit" :disabled="loading">
            {{ loading ? '登録中...' : '登録' }}
          </button>
          <RouterLink to="/users" class="btn-back">ユーザー一覧に戻る</RouterLink>
        </div>
      </form>
    </div>
  </div>
</template>

<script>
import api from '../api'
import { extractError } from '../utils/error'

export default {
  name: 'RegisterView',
  data() {
    return {
      form: { username: '', email: '', password: '', passwordConfirm: '', role: '' },
      loading: false,
      successMsg: '',
      errorMsg: '',
    }
  },
  methods: {
    async submit() {
      this.successMsg = ''
      this.errorMsg = ''

      if (this.form.password !== this.form.passwordConfirm) {
        this.errorMsg = 'パスワードが一致しません'
        return
      }

      this.loading = true
      try {
        await api.post('/api/users', {
          username: this.form.username,
          email: this.form.email,
          password: this.form.password,
          role: this.form.role,
        })
        this.successMsg = `アカウント「${this.form.username}」を登録しました`
        this.form = { username: '', email: '', password: '', passwordConfirm: '', role: '' }
      } catch (e) {
        this.errorMsg = '登録に失敗しました: ' + extractError(e)
      } finally {
        this.loading = false
      }
    },
  },
}
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  padding: 3rem 1rem;
}
.register-card {
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 2rem 2.5rem;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.07);
}
h1 {
  font-size: 1.3rem;
  margin: 0 0 1.5rem;
  color: #2c3e50;
}
.field {
  margin-bottom: 1rem;
}
label {
  display: block;
  font-size: 0.85rem;
  color: #555;
  margin-bottom: 4px;
}
.required {
  color: #d9534f;
}
input, select {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.95rem;
  box-sizing: border-box;
}
input:focus, select:focus {
  outline: none;
  border-color: #4a9e6b;
}
.actions {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1.5rem;
}
.btn-submit {
  padding: 8px 24px;
  background: #4a9e6b;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 0.95rem;
  cursor: pointer;
}
.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.btn-submit:hover:not(:disabled) {
  background: #3d8a5c;
}
.btn-back {
  font-size: 0.9rem;
  color: #555;
  text-decoration: none;
}
.btn-back:hover {
  text-decoration: underline;
}
.msg-success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 1rem;
  font-size: 0.9rem;
}
.msg-error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 1rem;
  font-size: 0.9rem;
}
</style>
