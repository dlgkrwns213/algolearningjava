import { createRouter, createWebHistory } from 'vue-router'
import JoinPage from '../views/JoinRoomView.vue'    
import CodeEditor from '../components/CodeEditor.vue' 

const routes = [
  {
    path: '/',
    name: 'home',
    component: JoinPage
  },
  {
    path: '/editor',
    name: 'editor',
    component: CodeEditor
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
