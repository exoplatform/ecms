<template>
  <div class="documentSize">
    <template v-if="documentSize">
      {{ documentSize }} {{ $t(`analytics.${unit}`) }}
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
  },
  data: () => ({
    BYTES_IN_KB: 1024,
    BYTES_IN_MB: 1048576,
    BYTES_IN_GB: 1073741824,
    MIN_GB_UNIT_DISPLAY_IN_MB: 500,
    MIN_MB_UNIT_DISPLAY_IN_KB: 500,
    MIN_KB_UNIT_DISPLAY_IN_BYTES: 500,
    formattedSizePrecision: 2,
    documentSize: 0,
    unit: 'bytes',
  }),
  watch: {
    value: {
      immediate: true,
      handler(value) {
        if (value) {
          this.computeFileSize();
        }
      },
    },
  },
  methods: {
    computeFileSize() {
      const minGb = this.MIN_GB_UNIT_DISPLAY_IN_MB * this.BYTES_IN_MB; // equals 0.01 GB
      const minMb = this.MIN_MB_UNIT_DISPLAY_IN_KB * this.BYTES_IN_KB; // equals 0.01 MB, which is the smallest number with precision `formattedSizePrecision`
      const minKb = this.MIN_KB_UNIT_DISPLAY_IN_BYTES; // equals 0.01 KB, which is the smallest number with precision `formattedSizePrecision`
      let size = this.value;
      if (size < minKb) {
        this.unit = 'bytes';
      } else if (size < minMb) {
        size = size / this.BYTES_IN_KB;
        this.unit = 'kilo';
      } else if (size < minGb) {
        size = size / this.BYTES_IN_MB;
        this.unit = 'mega';
      } else {
        size = size / this.BYTES_IN_GB;
        this.unit = 'giga';
      }
      this.documentSize = (+size).toFixed(this.formattedSizePrecision);
    },
  }
};
</script>