<template>
  <div class="logo-container" :style="containerStyle">
    <svg
      :width="sizeInPx"
      :height="sizeInPx"
      viewBox="0 0 120 120"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      class="logo-svg"
    >
      <defs>
        <!-- 中心核心渐变 -->
        <radialGradient id="coreGradient" cx="50%" cy="50%" r="50%">
          <stop offset="0%" :stop-color="colors.coreCenter" />
          <stop offset="100%" :stop-color="colors.coreEdge" />
        </radialGradient>
        
        <!-- 外框渐变 -->
        <linearGradient id="frameGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" :stop-color="colors.frameStart" />
          <stop offset="100%" :stop-color="colors.frameEnd" />
        </linearGradient>
        
        <!-- 玻璃高光渐变 -->
        <linearGradient id="glassHighlight" x1="0%" y1="0%" x2="0%" y2="100%">
          <stop offset="0%" stop-color="white" stop-opacity="0.4" />
          <stop offset="100%" stop-color="white" stop-opacity="0" />
        </linearGradient>
        
        <!-- 连线渐变 -->
        <linearGradient id="lineGradient" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" :stop-color="colors.lineStart" />
          <stop offset="100%" :stop-color="colors.lineEnd" />
        </linearGradient>
        
        <!-- 外围节点渐变 -->
        <radialGradient id="nodeGradient" cx="50%" cy="50%" r="50%">
          <stop offset="0%" :stop-color="colors.nodeCenter" />
          <stop offset="100%" :stop-color="colors.nodeEdge" />
        </radialGradient>
        
        <!-- 发光滤镜 -->
        <filter id="glow" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
          <feMerge>
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
        
        <!-- 中心发光滤镜 -->
        <filter id="coreGlow" x="-100%" y="-100%" width="300%" height="300%">
          <feGaussianBlur stdDeviation="4" result="coloredBlur"/>
          <feMerge>
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
      </defs>
      
      <!-- 外框六边形 -->
      <path
        d="M60 8 L103.3 33 V83 L60 108 L16.7 83 V33 Z"
        :stroke="colors.frameStroke"
        stroke-width="3"
        fill="url(#frameGradient)"
        fill-opacity="0.15"
        stroke-linejoin="round"
        class="hexagon-frame"
      />
      
      <!-- 玻璃高光效果 -->
      <path
        d="M60 8 L103.3 33 V55 L60 80 L16.7 55 V33 Z"
        fill="url(#glassHighlight)"
        class="glass-highlight"
      />
      
      <!-- 辐射连线 - 6条 -->
      <g class="connection-lines" :stroke="colors.lineColor" stroke-width="1.5" stroke-linecap="round">
        <!-- 上 -->
        <line x1="60" y1="60" x2="60" y2="22" />
        <!-- 右上 -->
        <line x1="60" y1="60" x2="92.7" y2="41" />
        <!-- 右下 -->
        <line x1="60" y1="60" x2="92.7" y2="79" />
        <!-- 下 -->
        <line x1="60" y1="60" x2="60" y2="98" />
        <!-- 左下 -->
        <line x1="60" y1="60" x2="27.3" y2="79" />
        <!-- 左上 -->
        <line x1="60" y1="60" x2="27.3" y2="41" />
      </g>
      
      <!-- 外围节点 - 6个 -->
      <g class="outer-nodes">
        <!-- 上 -->
        <circle cx="60" cy="18" r="5" fill="url(#nodeGradient)" />
        <!-- 右上 -->
        <circle cx="94.7" cy="38" r="5" fill="url(#nodeGradient)" />
        <!-- 右下 -->
        <circle cx="94.7" cy="82" r="5" fill="url(#nodeGradient)" />
        <!-- 下 -->
        <circle cx="60" cy="102" r="5" fill="url(#nodeGradient)" />
        <!-- 左下 -->
        <circle cx="25.3" cy="82" r="5" fill="url(#nodeGradient)" />
        <!-- 左上 -->
        <circle cx="25.3" cy="38" r="5" fill="url(#nodeGradient)" />
      </g>
      
      <!-- 中心核心 -->
      <circle
        cx="60"
        cy="60"
        r="14"
        fill="url(#coreGradient)"
        filter="url(#coreGlow)"
        class="core-center"
      />
      
      <!-- 中心高光 -->
      <circle
        cx="56"
        cy="56"
        r="5"
        fill="white"
        fill-opacity="0.3"
        class="core-highlight"
      />
    </svg>
  </div>
</template>

<script>
export default {
  name: 'Logo',
  props: {
    // 模式：dark（用于浅色背景）或 light（用于深色背景）
    mode: {
      type: String,
      default: 'dark',
      validator: (value) => ['dark', 'light'].includes(value)
    },
    // 尺寸：small, medium, large
    size: {
      type: String,
      default: 'medium',
      validator: (value) => ['small', 'medium', 'large'].includes(value)
    },
    // 自定义尺寸（像素），优先级高于size
    customSize: {
      type: Number,
      default: null
    }
  },
  computed: {
    sizeInPx() {
      if (this.customSize) return this.customSize
      const sizeMap = {
        small: 32,
        medium: 40,
        large: 120
      }
      return sizeMap[this.size] || 40
    },
    containerStyle() {
      return {
        width: `${this.sizeInPx}px`,
        height: `${this.sizeInPx}px`
      }
    },
    colors() {
      // 深色模式（用于浅色背景）
      if (this.mode === 'dark') {
        return {
          // 中心核心
          coreCenter: '#60A5FA',
          coreEdge: '#3B82F6',
          // 外框
          frameStart: '#3B82F6',
          frameEnd: '#2563EB',
          frameStroke: '#2563EB',
          // 连线
          lineStart: 'rgba(59, 130, 246, 0.6)',
          lineEnd: 'rgba(37, 99, 235, 0.3)',
          lineColor: '#3B82F6',
          // 外围节点
          nodeCenter: '#60A5FA',
          nodeEdge: '#3B82F6'
        }
      }
      // 浅色模式（用于深色背景）
      return {
        // 中心核心
        coreCenter: '#93C5FD',
        coreEdge: '#60A5FA',
        // 外框
        frameStart: '#60A5FA',
        frameEnd: '#3B82F6',
        frameStroke: '#60A5FA',
        // 连线
        lineStart: 'rgba(96, 165, 250, 0.8)',
        lineEnd: 'rgba(59, 130, 246, 0.4)',
        lineColor: '#60A5FA',
        // 外围节点
        nodeCenter: '#93C5FD',
        nodeEdge: '#60A5FA'
      }
    }
  }
}
</script>

<style scoped>
.logo-container {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.logo-svg {
  display: block;
}

/* 动画效果 */
.hexagon-frame {
  transition: all 0.3s ease;
}

.core-center {
  animation: pulse 3s ease-in-out infinite;
}

.connection-lines line {
  stroke-dasharray: 40;
  stroke-dashoffset: 40;
  animation: drawLine 1.5s ease-out forwards;
}

.connection-lines line:nth-child(1) { animation-delay: 0.1s; }
.connection-lines line:nth-child(2) { animation-delay: 0.2s; }
.connection-lines line:nth-child(3) { animation-delay: 0.3s; }
.connection-lines line:nth-child(4) { animation-delay: 0.4s; }
.connection-lines line:nth-child(5) { animation-delay: 0.5s; }
.connection-lines line:nth-child(6) { animation-delay: 0.6s; }

.outer-nodes circle {
  opacity: 0;
  animation: fadeInNode 0.5s ease-out forwards;
}

.outer-nodes circle:nth-child(1) { animation-delay: 0.7s; }
.outer-nodes circle:nth-child(2) { animation-delay: 0.8s; }
.outer-nodes circle:nth-child(3) { animation-delay: 0.9s; }
.outer-nodes circle:nth-child(4) { animation-delay: 1.0s; }
.outer-nodes circle:nth-child(5) { animation-delay: 1.1s; }
.outer-nodes circle:nth-child(6) { animation-delay: 1.2s; }

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.9;
  }
}

@keyframes drawLine {
  to {
    stroke-dashoffset: 0;
  }
}

@keyframes fadeInNode {
  from {
    opacity: 0;
    transform: scale(0);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 悬停效果 */
.logo-container:hover .hexagon-frame {
  filter: drop-shadow(0 0 8px rgba(59, 130, 246, 0.5));
}

.logo-container:hover .core-center {
  animation: pulse 1.5s ease-in-out infinite;
}
</style>
