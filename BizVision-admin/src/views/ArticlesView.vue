<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { createArticle, deleteArticle, listArticles, updateArticle } from '../api/articles'
import { formatDateTime } from '../utils/format'
import { useToast } from '../composables/useToast'
import BaseModal from '../components/BaseModal.vue'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import PaginationBar from '../components/PaginationBar.vue'
import StateBlock from '../components/StateBlock.vue'

const toast = useToast()

const page = ref(1)
const size = ref(10)
const items = ref([])
const total = ref(0)
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await listArticles(page.value, size.value)
    items.value = data.list
    total.value = data.total
    const maxPage = Math.max(1, Math.ceil(data.total / size.value))
    if (page.value > maxPage) page.value = maxPage
  } catch (e) {
    items.value = []
    total.value = 0
    error.value = e.message
  } finally {
    loading.value = false
  }
}

watch([page, size], load)
onMounted(load)

/* ---------------- 新增 / 编辑 ---------------- */
const form = reactive({
  open: false,
  mode: 'create',
  id: null,
  name: '',
  author: '',
  content: '',
  errors: {},
  submitting: false,
})

function resetForm() {
  form.id = null
  form.name = ''
  form.author = ''
  form.content = ''
  form.errors = {}
  form.submitting = false
}

function openCreate() {
  resetForm()
  form.mode = 'create'
  form.open = true
}

function openEdit(row) {
  resetForm()
  form.mode = 'edit'
  form.id = row.id
  form.name = row.name
  form.author = row.author
  form.content = row.content
  form.open = true
}

function validate() {
  const errors = {}
  const name = form.name.trim()
  if (!name) errors.name = '请输入文章标题'
  else if (name.length > 100) errors.name = '标题不能超过 100 个字符'

  const author = form.author.trim()
  if (!author) errors.author = '请输入作者'
  else if (author.length > 50) errors.author = '作者不能超过 50 个字符'

  if (!form.content.trim()) errors.content = '请输入文章内容'

  form.errors = errors
  return Object.keys(errors).length === 0
}

async function submitForm() {
  if (!validate()) return
  form.submitting = true
  const payload = {
    name: form.name.trim(),
    author: form.author.trim(),
    content: form.content,
  }
  try {
    if (form.mode === 'create') {
      await createArticle(payload)
      toast.success('文章已发布')
      form.open = false
      if (page.value === 1) load()
      else page.value = 1
    } else {
      await updateArticle(form.id, payload)
      toast.success('文章已保存')
      form.open = false
      load()
    }
  } catch (e) {
    toast.error(e.message)
  } finally {
    form.submitting = false
  }
}

/* ---------------- 查看 ---------------- */
const preview = reactive({ open: false, row: null })

function openPreview(row) {
  preview.row = row
  preview.open = true
}

function editFromPreview() {
  const row = preview.row
  preview.open = false
  if (row) openEdit(row)
}

/* ---------------- 删除 ---------------- */
const remove = reactive({ open: false, row: null, submitting: false })

function askRemove(row) {
  remove.row = row
  remove.submitting = false
  remove.open = true
}

async function confirmRemove() {
  remove.submitting = true
  try {
    await deleteArticle(remove.row.id)
    toast.success('文章已删除（软删除，数据仍在库中）')
    remove.open = false
    load()
  } catch (e) {
    toast.error(e.message)
  } finally {
    remove.submitting = false
  }
}

function excerpt(text, length = 42) {
  const value = (text || '').replace(/\s+/g, ' ').trim()
  return value.length > length ? value.slice(0, length) + '…' : value || '—'
}
</script>

<template>
  <div class="space-y-5">
    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div class="flex items-center gap-2 text-sm text-slate-500">
        <span>共 <span class="font-semibold text-slate-800">{{ total }}</span> 篇文章</span>
        <span class="badge bg-slate-100 text-slate-500">删除为软删除</span>
      </div>
      <div class="flex items-center gap-2">
        <button class="btn-secondary" :disabled="loading" @click="load">
          <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.7">
            <path d="M16 10a6 6 0 1 1-1.8-4.3M16 4v3.5h-3.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          刷新
        </button>
        <button class="btn-primary" @click="openCreate">
          <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M10 4.5v11M4.5 10h11" stroke-linecap="round" />
          </svg>
          写文章
        </button>
      </div>
    </div>

    <div class="card overflow-hidden">
      <StateBlock
        :loading="loading"
        :error="error"
        :empty="items.length === 0"
        empty-text="还没有文章"
        empty-hint="点右上角「写文章」发布第一篇"
        @retry="load"
      >
        <template #empty-action>
          <button class="btn-primary mt-2" @click="openCreate">写文章</button>
        </template>

        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-slate-100">
            <thead class="bg-slate-50/70">
              <tr>
                <th class="th w-16">ID</th>
                <th class="th">标题</th>
                <th class="th w-28">作者</th>
                <th class="th w-40">创建时间</th>
                <th class="th w-24">状态</th>
                <th class="th w-48 text-right">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="row in items" :key="row.id" class="transition-colors hover:bg-slate-50/60">
                <td class="td font-mono text-xs text-slate-400">#{{ row.id }}</td>
                <td class="td">
                  <div class="group cursor-pointer text-left" @click="openPreview(row)">
                    <p class="font-medium text-slate-800 group-hover:text-indigo-600">{{ row.name }}</p>
                    <p class="mt-0.5 text-xs text-slate-400">{{ excerpt(row.content) }}</p>
                  </div>
                </td>
                <td class="td">{{ row.author || '—' }}</td>
                <td class="td whitespace-nowrap text-xs text-slate-500">{{ formatDateTime(row.createdAt) }}</td>
                <td class="td">
                  <span
                    class="badge"
                    :class="row.isOn === 1 ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'"
                  >
                    {{ row.isOn === 1 ? '已发布' : '已下架' }}
                  </span>
                </td>
                <td class="td">
                  <div class="flex items-center justify-end gap-1">
                    <button class="btn-secondary h-8 !px-2.5 !text-xs" @click="openPreview(row)">查看</button>
                    <button class="btn-secondary h-8 !px-2.5 !text-xs" @click="openEdit(row)">编辑</button>
                    <button
                      class="btn-ghost h-8 !px-2.5 !text-xs text-rose-600 hover:bg-rose-50 hover:text-rose-700"
                      @click="askRemove(row)"
                    >
                      删除
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </StateBlock>

      <PaginationBar
        v-if="!loading && !error && total > 0"
        v-model:page="page"
        v-model:size="size"
        :total="total"
      />
    </div>

    <p class="text-xs text-slate-400">
      提示：删除走 <code class="rounded bg-slate-100 px-1 py-0.5 font-mono text-[11px]">DELETE /article/{id}</code>，
      后端执行 <code class="rounded bg-slate-100 px-1 py-0.5 font-mono text-[11px]">UPDATE articles SET is_on = 0 WHERE id = ?</code>。
    </p>

    <!-- 新增 / 编辑 -->
    <BaseModal
      :open="form.open"
      :title="form.mode === 'create' ? '写文章' : '编辑文章'"
      :subtitle="form.mode === 'create' ? '发布后立即出现在列表中' : `文章 ID #${form.id}`"
      width="max-w-2xl"
      @close="form.open = false"
    >
      <form class="space-y-4" @submit.prevent="submitForm">
        <div>
          <label class="field-label" for="article-name">标题</label>
          <input
            id="article-name"
            v-model="form.name"
            class="field-input"
            :class="{ 'field-input-error': form.errors.name }"
            placeholder="一句话说清这篇讲什么"
            maxlength="100"
          />
          <div class="mt-1 flex items-center justify-between">
            <p v-if="form.errors.name" class="field-error !mt-0">{{ form.errors.name }}</p>
            <span v-else></span>
            <span class="text-xs text-slate-400">{{ form.name.length }}/100</span>
          </div>
        </div>

        <div>
          <label class="field-label" for="article-author">作者</label>
          <input
            id="article-author"
            v-model="form.author"
            class="field-input"
            :class="{ 'field-input-error': form.errors.author }"
            placeholder="例如：jia"
            maxlength="50"
          />
          <p v-if="form.errors.author" class="field-error">{{ form.errors.author }}</p>
        </div>

        <div>
          <label class="field-label" for="article-content">正文</label>
          <textarea
            id="article-content"
            v-model="form.content"
            rows="10"
            class="field-input resize-y leading-relaxed"
            :class="{ 'field-input-error': form.errors.content }"
            placeholder="支持换行，暂不支持富文本"
          ></textarea>
          <div class="mt-1 flex items-center justify-between">
            <p v-if="form.errors.content" class="field-error !mt-0">{{ form.errors.content }}</p>
            <span v-else></span>
            <span class="text-xs text-slate-400">{{ form.content.length }} 字</span>
          </div>
        </div>
      </form>

      <template #footer>
        <button type="button" class="btn-secondary" @click="form.open = false">取消</button>
        <button type="button" class="btn-primary" :disabled="form.submitting" @click="submitForm">
          <span
            v-if="form.submitting"
            class="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white/40 border-t-white"
          ></span>
          {{ form.mode === 'create' ? '发布' : '保存' }}
        </button>
      </template>
    </BaseModal>

    <!-- 查看 -->
    <BaseModal
      :open="preview.open"
      :title="preview.row?.name || ''"
      :subtitle="`${preview.row?.author || '—'} · ${formatDateTime(preview.row?.createdAt)}`"
      width="max-w-2xl"
      @close="preview.open = false"
    >
      <p class="whitespace-pre-wrap text-sm leading-7 text-slate-700">{{ preview.row?.content }}</p>
      <template #footer>
        <button type="button" class="btn-secondary" @click="preview.open = false">关闭</button>
        <button type="button" class="btn-primary" @click="editFromPreview">编辑这篇</button>
      </template>
    </BaseModal>

    <ConfirmDialog
      :open="remove.open"
      title="删除文章"
      :description="`确定删除「${remove.row?.name ?? ''}」吗？这是软删除，数据库里 is_on 会被置为 0，列表和详情都不再返回它。`"
      confirm-text="删除"
      :loading="remove.submitting"
      @confirm="confirmRemove"
      @close="remove.open = false"
    />
  </div>
</template>
