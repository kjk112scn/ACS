<template>
  <q-card class="mode-card control-section" :class="cardClass">
    <q-card-section class="mode-card__section" :class="sectionClass">
      <!-- Title (optional) -->
      <div v-if="title" class="mode-card__title text-subtitle1 text-weight-bold text-primary">
        {{ title }}
      </div>

      <!-- Content -->
      <div class="mode-card__content">
        <slot></slot>
      </div>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title?: string
  variant?: 'default' | 'compact' | 'centered'
  minHeight?: string
  maxHeight?: string
}

const props = withDefaults(defineProps<Props>(), {
  variant: 'default',
})

const cardClass = computed(() => ({
  'mode-card--compact': props.variant === 'compact',
  'mode-card--centered': props.variant === 'centered',
}))

const sectionClass = computed(() => ({
  'mode-card__section--centered': props.variant === 'centered',
}))
</script>

<style scoped>
.mode-card {
  background-color: var(--theme-card-background);
  border: 1px solid var(--theme-border, rgba(255, 255, 255, 0.12));
  border-radius: var(--theme-border-radius, 10px);
  box-shadow: 0 24px 40px var(--theme-shadow, rgba(0, 0, 0, 0.35));
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
}

.mode-card__section {
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: visible;
}

.mode-card__section--centered {
  justify-content: center;
  align-items: center;
}

.mode-card__title {
  margin-bottom: 1rem;
}

.mode-card__content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* Compact variant */
.mode-card--compact .mode-card__section {
  padding: 12px;
}

/* Centered variant */
.mode-card--centered .mode-card__content {
  align-items: center;
  justify-content: center;
}
</style>
