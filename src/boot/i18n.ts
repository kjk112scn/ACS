import { boot } from 'quasar/wrappers'
import i18n from 'src/i18n'

export default boot(({ app }) => {
  // i18n 플러그인 등록
  app.use(i18n)
})
