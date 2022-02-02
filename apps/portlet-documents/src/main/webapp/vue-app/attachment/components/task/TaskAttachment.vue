<template>
  <div class="taskAttachments d-flex">
    <attachment-app
      :entity-id="taskId"
      :space-id="spaceId"
      entity-type="task" />
  </div>
</template>
<script>
export default {
  props: {
    taskId: {
      type: String,
      default: null,
    },
    spaceId: {
      type: String,
      default: null,
    },
  },
};
</script>