<template>
  <v-carousel
    :continuous="false"
    height="250px"
    light
    hide-delimiters>
    <v-carousel-item
      v-for="(attachment, index) in attachments"
      :key="attachment.id"
      class="my-auto">
      <activity-attachment
        :activity="activity"
        :index="index"
        :count="attachmentsCount"
        :attachment="attachment"
        @delete-invald-attachment="deleteAttachment(attachment)" />
    </v-carousel-item>
  </v-carousel>
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
      docPaths.forEach((path, index) => {
        const mimeType = this.getParamValue(mimeTypes, index);
        const icon = mimeType && `primary--text uiIconFileType${mimeType.replaceAll(/[/.\\]/g, '')}` || '';
        let name = docTitles && docTitles.length > index && docTitles[index] || path.replaceAll(/(.*)\//g, '');
        try {
          name = decodeURIComponent(name.replace(/%25/g, '%').replace(/%([^2][^5])/g, '%25$1'));
        } catch (e) {
          // could happen, but ignore it
        }
        const repository = this.getParamValue(repositories, index);
        const workspace = this.getParamValue(workspaces, index);
        const imageURL = mimeType.includes('image/') && `${eXo.env.portal.context}/${eXo.env.portal.rest}/jcr/${repository}/${workspace}${path}` || null;

        attachments.push({
          id: this.getParamValue(ids, index),
          name,
          mimeType,
          image: imageURL,
          path,
          repository,
          workspace,
          icon: `uiIconPLFFont ${icon} uiIconFileTypeDefault`,
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