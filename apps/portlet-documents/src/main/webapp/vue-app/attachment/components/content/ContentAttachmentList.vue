<!--
 * Copyright (C) 2024 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
-->

<template>
  <div>
    <div
      v-show="attachmentsIds?.length"
      class="newsAttachmentsTitle mx-13"
      :class="isMobile && 'd-flex flex-row pa-4 subtitle-2'">
      {{ $t('attachments.list') }}
      ({{ attachmentsIds ? attachmentsIds.length : 0 }})
    </div>
    <div class="newsAttachments">
      <content-attachment-item
        v-for="attachmentId in attachmentsIds"
        :key="attachmentId"
        :attachment-id="attachmentId" />
    </div>
  </div>
</template>

<script>
export default {
  props: {
    attachmentsIds: {
      type: String,
      default: null,
    }
  },
  computed: {
    isMobile() {
      return this.$vuetify?.breakpoint?.smAndDown;
    }
  }
};
</script>
