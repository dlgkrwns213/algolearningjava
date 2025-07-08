<template>
  <div ref="editorContainer" class="editor-container"></div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { EditorState } from '@codemirror/state'
import { EditorView, keymap, lineNumbers } from '@codemirror/view'
import { defaultKeymap } from '@codemirror/commands'
import { autocompletion } from '@codemirror/autocomplete'

const editorContainer = ref(null)

const keywords = ["apple", "banana", "count", "Scanner", "String"]

const myCompletions = autocompletion({
  override: [
    (context) => {
      let word = context.matchBefore(/\w*/)
      if (!word) return null
      return {
        from: word.from,
        options: keywords.map(k => ({ label: k, type: "keyword" })),
        validFor: /^\w*$/
      }
    }
  ],
  activateOnTyping: true
})

onMounted(() => {
  new EditorView({
    state: EditorState.create({
      doc: "// 여기에 'app', 'ban' 입력해보세요",
      extensions: [
        lineNumbers(),
        keymap.of(defaultKeymap),
        myCompletions
      ]
    }),
    parent: editorContainer.value
  })
})
</script>

<style scoped>
.editor-container {
  width: 600px;
  height: 400px;
  border: 1px solid #ccc;
}
</style>
