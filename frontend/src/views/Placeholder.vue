<template>
  <div class="placeholder-page">
    <el-empty description="页面开发中">
      <template #image>
        <div class="placeholder-icon-wrap">
          <el-icon :size="48" color="#3B82F6"><Box /></el-icon>
        </div>
      </template>
      <template #description>
        <p class="placeholder-text">{{ $route.meta.title || '页面' }} 正在开发中</p>
        <p class="placeholder-subtext">敬请期待</p>
      </template>
      <el-button type="primary" class="back-btn" @click="$router.push('/')">返回首页</el-button>
    </el-empty>
  </div>
</template>

<script>
export default {
  name: 'PlaceholderPage'
}
</script>

<style scoped>
.placeholder-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  font-family: 'Inter', sans-serif;
}

.placeholder-icon-wrap {
  width: 100px;
  height: 100px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(59, 130, 246, 0.05) 100%);
  border: 1px solid rgba(59, 130, 246, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  box-shadow: 
    0 0 0 1px rgba(59, 130, 246, 0.1) inset,
    0 8px 32px rgba(59, 130, 246, 0.15);
}

.placeholder-text {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-size: 20px;
  font-weight: 600;
  color: #F8FAFC;
  margin-bottom: 8px;
}

.placeholder-subtext {
  font-size: 14px;
  color: rgba(248, 250, 252, 0.5);
}

.back-btn {
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
  border: none;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
  font-family: 'Inter', sans-serif;
  font-weight: 500;
}

.back-btn:hover {
  background: linear-gradient(135deg, #60A5FA 0%, #3B82F6 100%);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.45);
}
</style>
