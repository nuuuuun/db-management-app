import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { isLoggedIn } from '../utils/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/', name: 'home', component: HomeView, meta: { requiresAuth: true } },
    { path: '/about', name: 'about', component: () => import('../views/AboutView.vue') },
    { path: '/users', name: 'users', component: () => import('../views/UsersView.vue'), meta: { requiresAuth: true } },
    { path: '/tables', name: 'tables', component: () => import('../views/TablesView.vue'), meta: { requiresAuth: true } },
    { path: '/tables/:name', name: 'table-detail', component: () => import('../views/TableDetailView.vue'), meta: { requiresAuth: true } },
    { path: '/tables/:name/import', name: 'csv-import', component: () => import('../views/CsvImportView.vue'), meta: { requiresAuth: true } },
    { path: '/tables/:name/edit/:id', name: 'table-row-edit', component: () => import('../views/TableRowEditView.vue'), meta: { requiresAuth: true } },
    { path: '/tables/:name/edit/:id/confirm', name: 'table-row-edit-confirm', component: () => import('../views/TableRowEditConfirmView.vue'), meta: { requiresAuth: true } },
    { path: '/masked-columns', name: 'masked-columns', component: () => import('../views/MaskedColumnsView.vue'), meta: { requiresAuth: true } },
    { path: '/csv-bulk', name: 'csv-bulk', component: () => import('../views/CsvBulkView.vue'), meta: { requiresAuth: true } },
  ],
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    next('/login')
  } else if (to.name === 'login' && isLoggedIn()) {
    next('/')
  } else {
    next()
  }
})

export default router
