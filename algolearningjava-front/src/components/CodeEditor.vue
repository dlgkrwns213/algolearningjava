<template>
  <div class="editor-wrapper">
    <div class="editor-main">
      <h2 style="text-align: left; margin-bottom: 12px;">{{ roomId }}번 방</h2>

      <div v-if="hasWritePermission" class="status green">✍️ 코드 편집 권한이 있습니다<span v-if="isOwner"> (방장)</span></div>
      <div v-else class="status gray">🔒 편집 권한이 없습니다<span v-if="isOwner"> (방장)</span></div>

      <div ref="editorContainer" class="editor-container"></div>
    </div>

    <div class="sidebar">
      <h3>참여자</h3>
      <ul>
        <li v-for="p in participants" :key="p">
          <span :style="{ fontWeight: writableUserIds.includes(p) ? 'bold' : 'normal' }">
            {{ p }}
            <span v-if="p === userId"> (나)</span>
            <span v-if="p === ownerId"> (방장)</span>
          </span>

          <template v-if="isOwner">
            <button
              @click="toggleWriter(p)"
              :class="writableUserIds.includes(p) ? 'btn-remove' : 'btn-add'"
            >
              {{ writableUserIds.includes(p) ? '권한 회수' : '권한 부여' }}
            </button>
          </template>

        </li>
      </ul>
    </div>
  </div>
</template>


<script setup>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import {
  EditorView,
  keymap,
  highlightActiveLine,
  lineNumbers
} from '@codemirror/view'
import {
  EditorState,
  Compartment
} from '@codemirror/state'
import { defaultKeymap } from '@codemirror/commands'
import { java } from '@codemirror/lang-java'

const route = useRoute()
const roomId = ref(route.query.roomId || 'unknown')
const userId = localStorage.getItem('userId') || 'user-9999'
const editorContainer = ref(null)

let editorView = null
let websocket = null
let suppressUpdate = false

const editableCompartment = new Compartment()

const initialCode = ref('// 로딩 중...')
const ownerId = ref('')
const participants = ref([]) // string[]
const writableUserIds = ref([]) // string[]

const isOwner = computed(() => userId === ownerId.value)
const hasWritePermission = computed(() => writableUserIds.value.includes(userId))

async function fetchInitialCode() {
  try {
    const res = await axios.get(`http://localhost:8080/api/room/${roomId.value}`)
    initialCode.value = res.data.code || ''
  } catch (e) {
    console.error('❌ 코드 불러오기 실패:', e)
    initialCode.value = '// 초기 코드 없음'
  }
}

function toggleWriter(targetId) {
  const already = writableUserIds.value.includes(targetId)
  websocket.send(JSON.stringify({
    type: already ? 'removeWriter' : 'setWriter',
    roomId: roomId.value,
    userId: userId,
    targetUserId: targetId
  }))
}

function setupWebSocket() {
  const wsProtocol = location.protocol === 'https:' ? 'wss' : 'ws'
  const wsUrl = `${wsProtocol}://${location.hostname}:8080/ws/code?roomId=${roomId.value}&userId=${userId}`
  websocket = new WebSocket(wsUrl)

  websocket.onopen = () => {
    console.log('✅ WebSocket 연결됨')
    websocket.send(JSON.stringify({
      type: 'join',
      roomId: roomId.value,
      userId: userId
    }))
  }

  websocket.onmessage = (event) => {
    const msg = JSON.parse(event.data)

    switch (msg.type) {
      case 'codeChange':
        if (!suppressUpdate) {
          suppressUpdate = true
          editorView.dispatch({
            changes: { from: 0, to: editorView.state.doc.length, insert: msg.content }
          })
          setTimeout(() => suppressUpdate = false, 50)
        }
        break

      case 'writerListChanged':
        writableUserIds.value = msg.targetUserIds || []
        updateEditorEditable()
        break

      case 'ownerInfo':
        ownerId.value = msg.ownerId
        break

      case 'participantList':
        console.log(msg);
        participants.value = msg.participants || []
        console.log(participants.value);
        break
    }
  }

  websocket.onerror = (err) => {
    console.error('❌ WebSocket 오류:', err)
  }

  websocket.onclose = (event) => {
    console.warn('🔌 WebSocket 연결 종료됨', event.code, event.reason)
  }
}

function updateEditorEditable() {
  if (!editorView) return
  const editable = writableUserIds.value.includes(userId)
  editorView.dispatch({
    effects: editableCompartment.reconfigure(EditorView.editable.of(editable))
  })
}

onMounted(async () => {
  await fetchInitialCode()

  const updateListener = EditorView.updateListener.of(update => {
    if (update.docChanged && !suppressUpdate && hasWritePermission.value) {
      const newCode = update.state.doc.toString()
      if (websocket?.readyState === WebSocket.OPEN) {
        websocket.send(JSON.stringify({
          type: 'codeChange',
          content: newCode,
          roomId: roomId.value,
          userId: userId
        }))
      }
    }
  })

  editorView = new EditorView({
    state: EditorState.create({
      doc: initialCode.value,
      extensions: [
        java(),
        lineNumbers(),
        highlightActiveLine(),
        keymap.of(defaultKeymap),
        updateListener,
        editableCompartment.of(EditorView.editable.of(false))
      ]
    }),
    parent: editorContainer.value
  })

  setupWebSocket()
})

onBeforeUnmount(() => {
  if (editorView) editorView.destroy()
  if (websocket) websocket.close()
})
</script>

<style scoped>
.editor-container {
  width: 600px;
  height: 400px;
  border: 1px solid #ccc;
  text-align: left;
  margin-top: 20px;
}

.editor-wrapper {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
}

.editor-main {
  flex: 1;
}

.sidebar {
  position: fixed;
  top: 20px;       /* 위에서 20px 떨어짐 */
  right: 20px;     /* 오른쪽에서 20px 떨어짐 */
  width: 200px;
  border-left: 1px solid #ccc;
  padding-left: 16px;
  background: white; /* 배경색 추가하면 가독성 좋아짐 */
  box-shadow: 0 0 8px rgba(0,0,0,0.1); /* 약간의 그림자 */
  z-index: 1000;   /* 다른 요소 위로 */
  text-align: left;
  margin-top: 0;   /* 기존 margin-top 제거 */
  margin-left: 0;  /* 기존 margin-left 제거 */
  padding: 10px;
}

.sidebar ul {
  padding-left: 0; /* 기본 들여쓰기 제거 */
  margin: 0;
  list-style: none;
}

.sidebar ul li {
  margin-bottom: 6px; /* 간격 줄임 (기존보다 작게) */
  line-height: 1.3;   /* 텍스트 줄 간격도 조절 가능 */
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sidebar ul li:last-child {
  margin-bottom: 0; /* 마지막 아이템 밑 간격 제거 */
}

.sidebar ul li button {
  font-size: 11px;      /* 버튼 글자 작게 */
  padding: 2px 4px;     /* 버튼 패딩 작게 */
  margin-left: 8px;
  cursor: pointer;
}


.btn-add {
  border: 1.5px solid #28a745; /* 초록색 */
  background-color: white;
  color: #28a745;
  border-radius: 4px;
}

.btn-add:hover {
  background-color: #28a745;
  color: white;
}

.btn-remove {
  border: 1.5px solid #dc3545; /* 빨간색 */
  background-color: white;
  color: #dc3545;
  border-radius: 4px;
}

.btn-remove:hover {
  background-color: #dc3545;
  color: white;
}

.status {
  font-weight: bold;
  margin-bottom: 10px;
}

.status.green {
  color: green;
}
.status.gray {
  color: gray;
}

.permission-btn {
  margin-left: 8px;
  font-size: 12px;
  padding: 2px 6px;
}

</style>
