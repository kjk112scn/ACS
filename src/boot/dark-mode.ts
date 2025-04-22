import { boot } from 'quasar/wrappers'

export default boot(({ app }) => {
  const $q = app.config.globalProperties.$q
  const savedDarkMode = localStorage.getItem('isDarkMode')
  if (savedDarkMode === null) {
    $q.dark.set(true)
    localStorage.setItem('isDarkMode', 'true')
  } else {
    $q.dark.set(savedDarkMode === 'true')
  }
})
