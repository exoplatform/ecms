<template>
  <card-carousel parent-class="activity-files-parent">
    <activity-attachment
      v-for="(attachment, index) in attachments"
      :key="attachment.id"
      :activity="activity"
      :index="index"
      :count="attachmentsCount"
      :attachment="attachment"
      class="activity-file-item" />
  </card-carousel>
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
    templateParams() {
      if (this.activity && this.activity.parentActivity) {
        const templateParams = this.activity.parentActivity
                               && this.activity.parentActivity.templateParams;
        if (templateParams) {
          const sharedTemplateParams = {};
          Object.keys(templateParams).forEach(param => {
            sharedTemplateParams[param.replace('Shared_', '')] = templateParams[param];
          });
          return sharedTemplateParams;
        }
      }
      return this.activity && this.activity.templateParams;
    },
    attachments() {
      if (!this.templateParams) {
        return [];
      }
      const repositories = this.templateParams.REPOSITORY && this.splitParam('REPOSITORY')
                        || (this.templateParams.repository && [this.templateParams.repository]);
      const workspaces = this.templateParams.WORKSPACE && this.splitParam('WORKSPACE')
                        || (this.templateParams.workspace && [this.templateParams.workspace]);
      const docPaths = this.templateParams.DOCPATH && this.splitParam('DOCPATH')
                        || (this.templateParams.nodePath && [this.templateParams.nodePath]);
      const docTitles = this.templateParams.docTitle && this.splitParam('docTitle');
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
        const imageURL = mimeType.includes('image/') && `${eXo.env.portal.context}/${eXo.env.portal.rest}/jcr/${repository}/${workspace}${path.replace(/\[/g, '%5b').replace(/\]/g, '%5d').replace(/\+/g, '%2b')}` || null;

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
      if (this.activity && this.templateParams && this.templateParams[value]) {
        return this.templateParams[value].split('|@|');
      }
      return [];
    },
    getParamValue(arr, index) {
      if (arr.length > index) {
        return arr[index];
      }
      return null;
    },
  },
};
</script>