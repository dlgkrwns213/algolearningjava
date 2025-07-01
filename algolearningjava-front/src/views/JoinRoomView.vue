<template>
  <div class="p-4">
    <h2 class="text-xl font-bold mb-2">방 입장</h2>
    <input v-model="roomId" placeholder="Room ID" class="input" />
    <input v-model="userId" placeholder="User ID" class="input" />
    <input v-model="userName" placeholder="User Name" class="input" />
    <button @click="joinRoom" class="btn mt-2">입장</button>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

const roomId = ref('')
const userId = ref('')
const userName = ref('')
const router = useRouter()

const joinRoom = async () => {
  try {
    await axios.post('http://localhost:8080/api/room/join', {
      roomId: roomId.value,
      userId: userId.value,
      userName: userName.value,
    }, { withCredentials: true })

    // localStorage에 저장
    localStorage.setItem('userId', userId.value)

    router.push({ name: 'editor', query: { roomId: roomId.value, userId: userId.value } })
  } catch (e) {
    console.error('입장 실패:', e)
  }
}

</script>

<style scoped>
.input {
  display: block;
  margin-bottom: 10px;
  padding: 8px;
  width: 100%;
}
.btn {
  padding: 8px 12px;
  background-color: #007bff;
  color: white;
}
</style>
