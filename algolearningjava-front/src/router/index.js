import { createRouter, createWebHistory } from 'vue-router'
import JoinRoomView from '../views/JoinRoomView.vue'
import CodeEditor from '../components/CodeEditor.vue'

const routes = [
  { path: '/', component: JoinRoomView },
  { path: '/editor', name: 'editor', component: CodeEditor },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})
