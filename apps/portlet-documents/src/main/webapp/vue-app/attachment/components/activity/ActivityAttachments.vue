<template>
  <v-list dense class="pa-0">
    <activity-attachment
      v-for="(attachment, index) in attachments"
      :key="attachment.id"
      :activity="activity"
      :index="index"
      :count="attachmentsCount"
      :attachment="attachment"
      @delete-invald-attachment="deleteAttachment(attachment)" />
  </v-list>
</template>

<script>
export default {
  props: {
    activity: {
      type: Object,
      default: null,
    },
  },
  computed: {
    attachments() {
      if (!this.activity) {
        return [];
      }
      const repositories = this.splitParam('REPOSITORY');
      const workspaces = this.splitParam('WORKSPACE');
      const docPaths = this.splitParam('DOCPATH');
      const ids = this.splitParam('id');
      const mimeTypes = this.splitParam('mimeType');

      const attachments = [];
      docPaths.forEach((docPath, index) => {
        const name = docPath.replaceAll(/(.*)\//g, '');
        const mimeType = this.getParamValue(mimeTypes, index);
        const icon = mimeType && `primary--text uiIconFileType${mimeType.replaceAll(/[/.\\]/g, '')}` || '';

        attachments.push({
          id: this.getParamValue(ids, index),
          name,
          path: docPath,
          icon: `uiIconPLFFont ${icon} uiIconFileTypeDefault`,
          repository: this.getParamValue(repositories, index),
          workspace: this.getParamValue(workspaces, index),
          mimeType: this.getParamValue(mimeTypes, index),
        });
      });
      return attachments;
    },
    attachmentsCount() {
      return this.attachments.length;
    },
  },
  methods: {
    splitParam(value) {
      if (this.activity && this.activity.templateParams && this.activity.templateParams[value]) {
        return this.activity.templateParams[value].split('|@|');
      }
      return [];
    },
    getParamValue(arr, index) {
      if (arr.length > index) {
        return arr[index];
      }
      return null;
    },
    deleteAttachment(attachment) {
      const index = this.attachments.findIndex(att => att === attachment);
      if (index >= 0) {
        this.attachments.splice(index, 1);
        this.$forceUpdate();
      }
    },
  },
};
</script>