<template>
  <div class="editor-wrapper">
    <div class="editor-main">
      <h2 style="text-align: left; margin-bottom: 12px;">{{ roomId }}번 방</h2>

      <div v-if="hasWritePermission" class="status green">✍️ 코드 편집 권한이 있습니다<span v-if="isOwner"> (방장)</span></div>
      <div v-else class="status gray">🔒 편집 권한이 없습니다</div>

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
import { autocompletion } from '@codemirror/autocomplete'
import { basicSetup } from 'codemirror'
import { insertTab } from '@codemirror/commands'

// ✅ 정적 Java 키워드 목록
const staticJavaKeywords = [
  "abstract", "assert", "boolean", "break", "byte", "case", "catch",
  "char", "class", "const", "continue", "default", "do", "double",
  "else", "enum", "extends", "final", "finally", "float", "for",
  "goto", "if", "implements", "import", "instanceof", "int",
  "interface", "long", "native", "new", "null", "package",
  "private", "protected", "public", "return", "short", "static",
  "strictfp", "super", "switch", "synchronized", "this", "throw",
  "throws", "transient", "try", "void", "volatile", "while",
  "System", "out", "println", "Scanner", "List", "ArrayList", "String", "Main",
  "java", "util"
]

// ✅ 변수명 추출
function extractVariableNames(code) {
  const regex = /\b(?:int|double|float|char|boolean|long|short|byte|String|var)\s+([a-zA-Z_][a-zA-Z0-9_]*)/g
  const result = new Set()
  let match
  while ((match = regex.exec(code)) !== null) {
    result.add(match[1])
  }
  return Array.from(result)
}

// ✅ 자동완성 구성
function dynamicCompletion(context) {
  const word = context.matchBefore(/\w+/)
  if (!word) return null

  const code = context.state.doc.toString()
  const variables = extractVariableNames(code)

  const suggestions = [
    ...staticJavaKeywords.map(k => ({ label: k, type: 'keyword' })),
    ...variables.map(v => ({ label: v, type: 'variable' }))
  ]

  return {
    from: word.from,
    options: suggestions,
    validFor: /^\w*$/
  }
}

const myCompletions = autocompletion({
  override: [dynamicCompletion],
  activateOnTyping: true // 자동추천
})

// ✅ 기본 변수
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

const updateListener = EditorView.updateListener.of(update => {
  if (update.docChanged && hasWritePermission.value) {
    const newCode = update.state.doc.toString()

    if (websocket?.readyState === WebSocket.OPEN) {
      websocket.send(JSON.stringify({
        type: 'codeChange',
        content: newCode,
        roomId: roomId.value,
        userId: userId
      }))
    }

    console.log('[DB 저장 요청] 코드 길이:', newCode.length)
    axios.put(`http://localhost:8080/api/room/${roomId.value}`, {
      code: newCode
    }).then(() => {
      console.log('[DB 저장 성공]')
    }).catch(err => {
      console.error('❌ 코드 저장 실패:', err)
    })
  }
})


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
      if (!suppressUpdate && editorView) {
          suppressUpdate = true
          editorView.dispatch({
            changes: { from: 0, to: editorView.state.doc.length, insert: msg.content },
            selection: 1})
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
        participants.value = msg.participants || []
        break
    }
  }

  websocket.onerror = (err) => {
    console.error('❌ WebSocket 오류:', err)
  }

  websocket.onclose = (event) => {
    console.warn('🔌 WebSocket 종료:', event.code, event.reason)
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

  console.log('🎯 코드 확인:', initialCode.value)

  editorView = new EditorView({
    state: EditorState.create({
      doc: initialCode.value,
      extensions: [
        basicSetup,
        java(),
        lineNumbers(),
        highlightActiveLine(),
        keymap.of([
          ...defaultKeymap,
          { key: "Tab", run: insertTab } // Tab 키 삽입 지원
        ]),
        editableCompartment.of(EditorView.editable.of(false)),
        myCompletions,
        updateListener
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
