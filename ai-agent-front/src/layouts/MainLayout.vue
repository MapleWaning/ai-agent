<script setup lang="ts">
defineProps<{
  blurred?: boolean
}>()
</script>

<template>
  <div class="app-shell">
    <div class="main-layout" :class="{ blurred }">
      <slot name="header" />
      <div class="main-layout__body">
        <div class="main-layout__sidebar">
          <slot name="sidebar" />
        </div>
        <div class="main-layout__main">
          <slot name="main" />
        </div>
        <div class="main-layout__aside">
          <slot name="aside" />
        </div>
      </div>
    </div>
    <slot name="overlay" />
  </div>
</template>

<style scoped>
.app-shell {
  height: 100%;
}
.main-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: filter 0.2s;
}
.main-layout.blurred {
  filter: blur(4px);
  pointer-events: none;
  user-select: none;
}
.main-layout__body {
  flex: 1;
  display: flex;
  min-height: 0;
}
.main-layout__sidebar {
  flex: 0 0 var(--sidebar-width);
  width: var(--sidebar-width);
  min-height: 0;
}
.main-layout__main {
  flex: 1;
  min-width: 0;
  min-height: 0;
}
.main-layout__aside {
  flex: 0 0 var(--aside-width);
  width: var(--aside-width);
  min-height: 0;
}
</style>
