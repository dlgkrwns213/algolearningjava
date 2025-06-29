<template>
  <div>
    <h2 style="text-align: left; margin-bottom: 12px;">{{ roomId }}번 방</h2>
    <div ref="editorContainer" class="editor-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { EditorView, keymap, highlightActiveLine, lineNumbers } from '@codemirror/view'
import { EditorState } from '@codemirror/state'
import { defaultKeymap } from '@codemirror/commands'
import { java } from '@codemirror/lang-java'

const route = useRoute()
const roomId = ref(route.query.roomId || 'unknown')
const editorContainer = ref(null)

let editorView = null
let websocket = null
let suppressUpdate = false // 무한루프 방지

const initialCode = `public class HelloWorld {
  public static void main(String[] args) {
    System.out.println("Hello, CodeMirror!");
  }
}`

// ✅ WebSocket 연결 및 수신 처리
function setupWebSocket() {
  const wsProtocol = location.protocol === 'https:' ? 'wss' : 'ws'
  const wsUrl = `${wsProtocol}://${location.hostname}:8080/ws/code?roomId=${roomId.value}`

  websocket = new WebSocket(wsUrl)

  websocket.onopen = () => {
    console.log('✅ WebSocket 연결됨')
  }

  websocket.onmessage = (event) => {
    const data = JSON.parse(event.data)
    if (data.type === 'codeChange' && !suppressUpdate) {
      suppressUpdate = true
      const transaction = editorView.state.update({
        changes: { from: 0, to: editorView.state.doc.length, insert: data.code }
      })
      editorView.update([transaction])
      suppressUpdate = false
    }
  }

  websocket.onerror = (err) => {
    console.error('❌ WebSocket 오류:', err)
  }

  websocket.onclose = () => {
    console.warn('🔌 WebSocket 연결 종료됨')
  }
}

// ✅ 에디터 생성 및 업데이트 리스너
onMounted(() => {
  setupWebSocket()

  const updateListener = EditorView.updateListener.of(update => {
    if (update.docChanged && !suppressUpdate) {
      const newCode = update.state.doc.toString()
      if (websocket?.readyState === WebSocket.OPEN) {
        websocket.send(JSON.stringify({
          type: 'codeChange',
          code: newCode,
          roomId: roomId.value
        }))
      }
    }
  })

  editorView = new EditorView({
    state: EditorState.create({
      doc: initialCode,
      extensions: [
        java(),
        lineNumbers(),
        highlightActiveLine(),
        keymap.of(defaultKeymap),
        updateListener
      ]
    }),
    parent: editorContainer.value
  })
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
}
</style>
