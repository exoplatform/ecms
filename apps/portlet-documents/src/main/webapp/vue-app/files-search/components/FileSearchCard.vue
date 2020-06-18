<template>
  <v-card class="searchApplicationCard d-flex flex-column" outlined>
    <div class="image mx-auto">
      <a :target="targetUrl" :href="applicationUrl">
        <v-img
          :src="imageUrl"
          class="appImage"
          width="148"
          height="148"/>
      </a>
    </div>
    <div class="mx-auto">
      <a
        :title="result.title"
        :target="targetUrl"
        :href="applicationUrl"
        class="headline">
        {{ result.title }}
      </a>
    </div>
    <div
      :title="result.description"
      class="mx-auto text-sub-title pt-2 pb-4">
      {{ result.description }}
    </div>
  </v-card>
</template>

<script>
export default {
  name: 'ApplicationSearchCard',
  props: {
    term: {
      type: String,
      default: null,
    },
    result: {
      type: Object,
      default: null,
    },
  },
  computed: {
    imageUrl() {
      return `${eXo.env.portal.context}/${eXo.env.portal.rest}/app-center/applications/illustration/${this.result.id}`;
    },
    applicationUrl() {
      const computedUrl = this.result.url.replace(/^\.\//, `${eXo.env.portal.context}/${eXo.env.portal.portalName}/`);
      return computedUrl.replace('@user@', eXo.env.portal.userName);
    },
    targetUrl() {
      return this.applicationUrl.indexOf('/') === 0 ? '_self' : '_blank';
    },
  },
};
</script>
