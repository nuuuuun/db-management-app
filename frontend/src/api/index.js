import axios from 'axios'
import { getToken, logout } from '../utils/auth'
import router from '../router'

const api = axios.create()

api.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      logout()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default api
