<template>
  <div class="w-screen h-screen flex flex-col p-4">
    <h2 class="text-xl font-bold mb-2">Room: {{ roomId }}</h2>

    <textarea
      v-model="code"
      @input="onCodeChange"
      class="flex-grow w-full p-4 font-mono text-base resize-none border border-gray-300 rounded"
    ></textarea>
  </div>
</template>


<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const roomId = route.query.roomId
const userId = route.query.userId

const code = ref('')
let socket

const onCodeChange = () => {
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify({
      type: 'codeChange',
      roomId,
      userId,
      content: code.value,
    }))
  }
}

onMounted(() => {
  socket = new WebSocket('ws://localhost:8080/ws/code')

  socket.onmessage = (event) => {
    const msg = JSON.parse(event.data)
    if (msg.type === 'codeChange' && msg.userId !== userId) {
      code.value = msg.content
    }
  }

  socket.onopen = () => {
    console.log('WebSocket 연결됨')
  }

  socket.onerror = (e) => {
    console.error('WebSocket 에러', e)
  }
})
</script>
