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
  <exo-attachment-item
    :file="attachment"
    class="newsAttachment text-truncate"/>
</template>

<script>
export default {
  data() {
    return {
      attachment: null
    };
  },
  props: {
    attachmentId: {
      type: String,
      default: null
    }
  },
  created() {
    this.getAttachmentById(this.attachmentId);
  },
  methods: {
    getAttachmentById() {
      return Vue.prototype.$attachmentService.getAttachmentById(this.attachmentId).then(attachment => {
        this.attachment = attachment;
      });
    }
  }
};
</script>
