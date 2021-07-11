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
      if (!this.activity || !this.activity.templateParams) {
        return [];
      }
      const repositories = this.activity.templateParams.REPOSITORY && this.splitParam('REPOSITORY')
                        || (this.activity.templateParams.repository && [this.activity.templateParams.repository]);
      const workspaces = this.activity.templateParams.WORKSPACE && this.splitParam('WORKSPACE')
                        || (this.activity.templateParams.workspace && [this.activity.templateParams.workspace]);
      const docPaths = this.activity.templateParams.DOCPATH && this.splitParam('DOCPATH')
                        || (this.activity.templateParams.nodePath && [this.activity.templateParams.nodePath]);
      const docTitles = this.activity.templateParams.docTitle && this.splitParam('docTitle');
      const ids = this.splitParam('id');
      const mimeTypes = this.splitParam('mimeType');

      const attachments = [];
      docPaths.forEach((docPath, index) => {
        const mimeType = this.getParamValue(mimeTypes, index);
        const icon = mimeType && `primary--text uiIconFileType${mimeType.replaceAll(/[/.\\]/g, '')}` || '';
        let name = docTitles && docTitles.length > index && docTitles[index] || docPath.replaceAll(/(.*)\//g, '');
        try {
          name = decodeURIComponent(name.replace(/%25/g, '%').replace(/%([^2][^5])/g, '%25$1'));
        } catch (e) {
          // could happen, but ignore it
        }

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