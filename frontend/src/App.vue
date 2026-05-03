<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { ref } from 'vue'
import { isLoggedIn, getUser, logout } from './utils/auth'

const router = useRouter()
const loggedIn = ref(isLoggedIn())
const user = ref(getUser())

router.afterEach(() => {
  loggedIn.value = isLoggedIn()
  user.value = getUser()
})

const handleLogout = () => {
  logout()
  loggedIn.value = false
  user.value = null
  router.push('/login')
}
</script>

<template>
  <header>
    <div class="navbar">
      <span class="site-title">DB Management</span>
      <nav>
        <template v-if="loggedIn">
          <RouterLink to="/tables">テーブル管理</RouterLink>
          <RouterLink v-if="user?.role !== 'VIEWER'" to="/csv-bulk">CSV管理</RouterLink>
          <span class="user-info">{{ user?.username }}（{{ user?.role }}）</span>
          <button class="btn-logout" @click="handleLogout">ログアウト</button>
        </template>
        <template v-else>
          <RouterLink to="/login">ログイン</RouterLink>
        </template>
      </nav>
    </div>
  </header>
  <RouterView />
</template>

<style scoped>
header {
  background: #2c3e50;
  padding: 0 2rem;
}
.navbar {
  display: flex;
  align-items: center;
  height: 52px;
  gap: 1.5rem;
}
.site-title {
  color: white;
  font-weight: bold;
  font-size: 1.1rem;
  white-space: nowrap;
}
nav {
  display: flex;
  align-items: center;
  gap: 0;
  flex: 1;
}
nav a {
  display: inline-block;
  padding: 0 1rem;
  color: #ccc;
  text-decoration: none;
  font-size: 0.9rem;
  border-left: 1px solid #455;
  line-height: 52px;
}
nav a:first-of-type { border: 0; }
nav a:hover { color: white; background: rgba(255,255,255,0.08); }
nav a.router-link-exact-active { color: white; font-weight: bold; }
.user-info {
  margin-left: auto;
  color: #aaa;
  font-size: 0.85rem;
  white-space: nowrap;
}
.btn-logout {
  margin-left: 0.75rem;
  padding: 4px 12px;
  background: #d9534f;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
}
.btn-logout:hover { background: #c0392b; }
</style>
