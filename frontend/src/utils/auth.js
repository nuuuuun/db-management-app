export const setToken = (token) => localStorage.setItem('jwt_token', token)
export const getToken = () => localStorage.getItem('jwt_token')
export const removeToken = () => localStorage.removeItem('jwt_token')
export const isLoggedIn = () => !!getToken()

export const setUser = (user) => localStorage.setItem('current_user', JSON.stringify(user))
export const getUser = () => JSON.parse(localStorage.getItem('current_user') || 'null')
export const removeUser = () => localStorage.removeItem('current_user')

export const logout = () => {
  removeToken()
  removeUser()
}
