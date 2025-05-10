import Vue from 'vue'
import App from './App.vue'
import router from './router'
import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import '@/assets/global.css'
import '@/assets/fonts/font-awesome.css'

Vue.config.productionTip = false
Vue.use(ElementUI, { size: "small"});
Vue.prototype.$bus = new Vue();
// 任意组件或 request.js
console.log("当前 BASE_API:", process.env.VUE_APP_BASE_API);//为什么是8082端口，我需要是8080

console.log("NODE_ENV:", process.env.NODE_ENV);//development

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
