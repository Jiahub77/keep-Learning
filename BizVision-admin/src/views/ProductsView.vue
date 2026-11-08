<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { createProduct, decreaseStock, deleteProduct, listProducts, updateProduct } from '../api/products'
import { centsToYuan, formatMoney, yuanToCents } from '../utils/format'
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
    const data = await listProducts(page.value, size.value)
    items.value = data.list
    total.value = data.total
    // 删除后当前页可能越界，回退到最后一页
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
  price: '',
  stock: '',
  errors: {},
  submitting: false,
})

function resetForm() {
  form.id = null
  form.name = ''
  form.price = ''
  form.stock = ''
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
  form.price = centsToYuan(row.priceInCents)
  form.stock = String(row.stock)
  form.open = true
}

function validate() {
  const errors = {}
  const name = form.name.trim()
  if (!name) errors.name = '请输入商品名称'
  else if (name.length > 100) errors.name = '名称不能超过 100 个字符'

  const price = String(form.price).trim()
  if (!price) errors.price = '请输入价格'
  else if (!/^\d+(\.\d{1,2})?$/.test(price)) errors.price = '价格需为数字，最多两位小数'
  else if (yuanToCents(price) > 2147483647) errors.price = '价格超出可存储范围'

  const stock = String(form.stock).trim()
  if (!stock) errors.stock = '请输入库存'
  else if (!/^\d+$/.test(stock)) errors.stock = '库存必须是非负整数'
  else if (Number(stock) > 2147483647) errors.stock = '库存超出可存储范围'

  form.errors = errors
  return Object.keys(errors).length === 0
}

async function submitForm() {
  if (!validate()) return
  form.submitting = true
  const payload = {
    name: form.name.trim(),
    priceInCents: yuanToCents(form.price),
    stock: Number(form.stock),
  }
  try {
    if (form.mode === 'create') {
      await createProduct(payload)
      toast.success('商品已创建')
      form.open = false
      // 列表按 id 倒序，新商品在第一页
      if (page.value === 1) load()
      else page.value = 1
    } else {
      await updateProduct(form.id, payload)
      toast.success('商品已保存')
      form.open = false
      load()
    }
  } catch (e) {
    toast.error(e.message)
  } finally {
    form.submitting = false
  }
}

/* ---------------- 扣减库存 ---------------- */
const stockForm = reactive({ open: false, id: null, name: '', quantity: '1', submitting: false, error: '' })

function openStock(row) {
  stockForm.id = row.id
  stockForm.name = row.name
  stockForm.quantity = '1'
  stockForm.error = ''
  stockForm.submitting = false
  stockForm.open = true
}

async function submitStock() {
  const quantity = String(stockForm.quantity).trim()
  if (!/^\d+$/.test(quantity) || Number(quantity) <= 0) {
    stockForm.error = '扣减数量必须是大于 0 的整数'
    return
  }
  stockForm.submitting = true
  stockForm.error = ''
  try {
    await decreaseStock(stockForm.id, Number(quantity))
    toast.success(`已扣减 ${quantity} 件库存`)
    stockForm.open = false
    load()
  } catch (e) {
    stockForm.error = e.message
  } finally {
    stockForm.submitting = false
  }
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
    await deleteProduct(remove.row.id)
    toast.success('商品已删除')
    remove.open = false
    load()
  } catch (e) {
    toast.error(e.message)
  } finally {
    remove.submitting = false
  }
}

function stockTone(stock) {
  if (stock <= 0) return 'bg-rose-50 text-rose-700'
  if (stock <= 10) return 'bg-amber-50 text-amber-700'
  return 'bg-slate-100 text-slate-600'
}
</script>

<template>
  <div class="space-y-5">
    <!-- 标题 + 操作 -->
    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div class="flex items-center gap-2 text-sm text-slate-500">
        <span>共 <span class="font-semibold text-slate-800">{{ total }}</span> 件商品</span>
        <span class="badge bg-slate-100 text-slate-500">价格单位：元</span>
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
          新增商品
        </button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="card overflow-hidden">
      <StateBlock
        :loading="loading"
        :error="error"
        :empty="items.length === 0"
        empty-text="还没有商品"
        empty-hint="点右上角「新增商品」创建第一条数据"
        @retry="load"
      >
        <template #empty-action>
          <button class="btn-primary mt-2" @click="openCreate">新增商品</button>
        </template>

        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-slate-100">
            <thead class="bg-slate-50/70">
              <tr>
                <th class="th w-16">ID</th>
                <th class="th">商品名称</th>
                <th class="th w-32">价格</th>
                <th class="th w-32">库存</th>
                <th class="th w-56 text-right">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="row in items" :key="row.id" class="transition-colors hover:bg-slate-50/60">
                <td class="td font-mono text-xs text-slate-400">#{{ row.id }}</td>
                <td class="td">
                  <p class="font-medium text-slate-800">{{ row.name }}</p>
                </td>
                <td class="td font-medium tabular-nums">{{ formatMoney(row.priceInCents) }}</td>
                <td class="td">
                  <span class="badge" :class="stockTone(row.stock)">
                    {{ row.stock }}
                    <span v-if="row.stock <= 0" class="opacity-80">售罄</span>
                    <span v-else-if="row.stock <= 10" class="opacity-80">偏低</span>
                  </span>
                </td>
                <td class="td">
                  <div class="flex items-center justify-end gap-1">
                    <button class="btn-secondary h-8 !px-2.5 !text-xs" @click="openStock(row)">扣库存</button>
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
      提示：库存扣减走 <code class="rounded bg-slate-100 px-1 py-0.5 font-mono text-[11px]">POST /products/decrease-stock/{id}</code>，
      请求体是一个裸数字；库存不足时后端返回 409。
    </p>

    <!-- 新增 / 编辑弹窗 -->
    <BaseModal
      :open="form.open"
      :title="form.mode === 'create' ? '新增商品' : '编辑商品'"
      :subtitle="form.mode === 'create' ? '价格以元为单位，提交时自动换算成分' : `商品 ID #${form.id}`"
      @close="form.open = false"
    >
      <form class="space-y-4" @submit.prevent="submitForm">
        <div>
          <label class="field-label" for="product-name">商品名称</label>
          <input
            id="product-name"
            v-model="form.name"
            class="field-input"
            :class="{ 'field-input-error': form.errors.name }"
            placeholder="例如：不锈钢保温杯 500ml"
            maxlength="100"
          />
          <p v-if="form.errors.name" class="field-error">{{ form.errors.name }}</p>
        </div>

        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="field-label" for="product-price">价格（元）</label>
            <input
              id="product-price"
              v-model="form.price"
              class="field-input tabular-nums"
              :class="{ 'field-input-error': form.errors.price }"
              inputmode="decimal"
              placeholder="0.00"
            />
            <p v-if="form.errors.price" class="field-error">{{ form.errors.price }}</p>
            <p v-else class="mt-1 text-xs text-slate-400">
              入库为 {{ form.price && /^\d+(\.\d{1,2})?$/.test(String(form.price).trim()) ? yuanToCents(form.price) : '—' }} 分
            </p>
          </div>
          <div>
            <label class="field-label" for="product-stock">库存</label>
            <input
              id="product-stock"
              v-model="form.stock"
              class="field-input tabular-nums"
              :class="{ 'field-input-error': form.errors.stock }"
              inputmode="numeric"
              placeholder="0"
            />
            <p v-if="form.errors.stock" class="field-error">{{ form.errors.stock }}</p>
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
          {{ form.mode === 'create' ? '创建' : '保存' }}
        </button>
      </template>
    </BaseModal>

    <!-- 扣减库存弹窗 -->
    <BaseModal :open="stockForm.open" title="扣减库存" :subtitle="stockForm.name" width="max-w-md" @close="stockForm.open = false">
      <form class="space-y-4" @submit.prevent="submitStock">
        <div>
          <label class="field-label" for="stock-quantity">扣减数量</label>
          <input
            id="stock-quantity"
            v-model="stockForm.quantity"
            class="field-input tabular-nums"
            :class="{ 'field-input-error': stockForm.error }"
            inputmode="numeric"
          />
          <p v-if="stockForm.error" class="field-error">{{ stockForm.error }}</p>
          <p v-else class="mt-1 text-xs text-slate-400">扣减后库存不能为负，否则后端返回 409「库存不足」。</p>
        </div>
      </form>

      <template #footer>
        <button type="button" class="btn-secondary" @click="stockForm.open = false">取消</button>
        <button type="button" class="btn-primary" :disabled="stockForm.submitting" @click="submitStock">
          <span
            v-if="stockForm.submitting"
            class="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white/40 border-t-white"
          ></span>
          确认扣减
        </button>
      </template>
    </BaseModal>

    <!-- 删除确认 -->
    <ConfirmDialog
      :open="remove.open"
      title="删除商品"
      :description="`确定删除「${remove.row?.name ?? ''}」吗？该操作会直接从数据库移除，无法恢复。`"
      confirm-text="删除"
      :loading="remove.submitting"
      @confirm="confirmRemove"
      @close="remove.open = false"
    />
  </div>
</template>
