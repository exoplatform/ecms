<template>
  <card-carousel parent-class="activity-files-parent">
    <activity-attachment
      v-for="(attachment, index) in attachments"
      :key="attachment.id"
      :activity="activity"
      :index="index"
      :count="attachmentsCount"
      :attachment="attachment"
      :preview-width="previewWidth"
      :preview-height="previewHeight"
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
    previewHeight: {
      type: String,
      default: () => '152px',
    },
    previewWidth: {
      type: String,
      default: () => '250px',
    },
  },
  computed: {
    attachments() {
      if (!this.activity.files) {
        return [];
      }

      const attachments = [];
      this.activity.files.forEach(attachment => {
        const mimeType = attachment.mimeType;
        const icon = mimeType && `primary--text uiIconFileType${mimeType.replaceAll(/[/.\\]/g, '')}` || '';
        let name = attachment.name;
        try {
          name = decodeURIComponent(name.replace(/%25/g, '%').replace(/%([^2][^5])/g, '%25$1'));
        } catch (e) {
          // could happen, but ignore it
        }
        const repository = attachment.repository;
        const workspace = attachment.workspace;
        const imageURL = mimeType.includes('image/') && `${eXo.env.portal.context}/${eXo.env.portal.rest}/thumbnailImage/custom/250x250/${workspace}/${attachment.id}` || null;

        attachments.push({
          id: attachment.id,
          image: imageURL,
          path: attachment.docPath,
          name,
          repository,
          workspace,
          mimeType,
          icon: `uiIconPLFFont ${icon} uiIconFileTypeDefault`,
        });
      });
      return attachments;
    },
    attachmentsCount() {
      return this.attachments.length;
    },
  },
};
</script>