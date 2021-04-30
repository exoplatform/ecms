<template>
  <div class="uploadedFiles">
    <div class="attachments-list d-flex align-center">
      <v-subheader class="text-sub-title pl-0 d-flex">{{ $t('attachments.drawer.title') }} ({{ attachments.length }})
      </v-subheader>
      <v-divider></v-divider>
    </div>
    <div v-if="attachments.length === 0" class="no-files-attached d-flex flex-column align-center text-sub-title">
      <div class="d-flex pl-6 not-files-icon">
        <i class="uiIconAttach uiIcon64x64"></i>
        <i class="uiIconCloseCircled uiIcon32x32"></i>
      </div>
      <span>{{ $t('no.attachments') }}</span>
    </div>
    <div v-if="attachments.length > 0" class="destinationFolder d-flex justify-space-between mb-4 ms-5">
      <div v-if="!displayMessageDestinationFolder && schemaFolder.length" class="destinationFolderBreadcrumb d-flex">
        <div :title="schemaFolder[0]" class="drive text-sub-title" rel="tooltip" data-placement="top">
          {{ schemaFolder[0] }}
        </div>
        <div class="folderLocation">
          <div v-if="schemaFolder.length > 1" class="folder">
            <div><span class="uiIconArrowRight colorIcon"></span></div>
            <div :title="schemaFolder[1]" :class="schemaFolder.length === 2 ? 'active' : 'text-sub-title' " class="folderName"
                 rel="tooltip" data-placement="top">{{ schemaFolder[1] }}
            </div>
          </div>
          <div v-if="schemaFolder.length === 3" class="folder">
            <div><span class="uiIconArrowRight colorIcon"></span></div>
            <div :title="schemaFolder[2]"
                 :class="schemaFolder[2] !== 'Activity Stream Documents' ? 'path' : 'active' " class="folderName"
                 rel="tooltip" data-placement="top">{{ schemaFolder[2] }}
            </div>
          </div>
          <div v-if="schemaFolder.length > 3" class="folder">
            <div><span class="uiIconArrowRight colorIcon"></span></div>
            <div :title="schemaFolder[2]" class="folderName" rel="tooltip" data-placement="top">...</div>
          </div>
          <div v-for="folder in schemaFolder.slice(schemaFolder.length-1,schemaFolder.length)"
               v-show="schemaFolder.length > 3" :key="folder" class="folder">
            <div><span class="uiIconArrowRight colorIcon"></span></div>
            <div :title="folder"
                 :class="schemaFolder[schemaFolder.length - 1] === folder && schemaFolder[schemaFolder.length - 1].length < 11 ?'active' : 'path'"
                 class="folderName" rel="tooltip" data-placement="top">{{ folder }}
            </div>
          </div>
        </div>
      </div>
      <div v-if="displayMessageDestinationFolder" class="messageDestination">
        <span
          :title="$t('attachments.drawer.destination.attachment.message')"
          class="text-sub-title"
          rel="tooltip"
          data-placement="top">
          {{ $t('attachments.drawer.destination.attachment.message') }}</span>
      </div>
      <button :disabled="displayMessageDestinationFolder" class="buttonSelect d-flex pl-1 flex-column-reverse"
              @click="openSelectDestinationFolderDrawer()">
        <i
          :title="!displayMessageDestinationFolder ? $t('attachments.drawer.destination.attachment') : $t('attachments.drawer.destination.attachment.access') "
          :class="displayMessageDestinationFolder ? 'disabled not-allowed' : ''" class="uiIconFolder " rel="tooltip"
          data-placement="top"></i>
      </button>
    </div>
    <div class="uploadedFilesItems ml-5">
      <transition-group name="list-complete" tag="div" class="d-flex flex-column">
        <span
          v-for="attachment in attachments"
          :key="attachment"
          class="list-complete-item"
        >
          <attachment-item :file="attachment"></attachment-item>
        </span>
      </transition-group>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    attachments: {
      type: Array,
      default: () => []
    },
    schemaFolder: {
      type: Object,
      default: () => null
    },
  },
  computed: {
    displayMessageDestinationFolder() {
      return !this.attachments.length || !this.attachments.some(val => val.uploadId != null && val.uploadId !== '');
    }
  },
  methods: {
    openSelectDestinationFolderDrawer() {
      this.$root.$emit('open-drive-explorer-drawer');
    },
  }
};
</script>