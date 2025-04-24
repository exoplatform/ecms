<template>
  <div class="documentOrigin">
    <template v-if="value">
      {{ $t(`analytics.origin.${value}`) }}
    </template>
    <template v-else>
      -
    </template>
  </div>
</template>

<script>
export default {
  props: {
    value: {
      type: Object,
      default: () => null,
    }
  }
};
</script>