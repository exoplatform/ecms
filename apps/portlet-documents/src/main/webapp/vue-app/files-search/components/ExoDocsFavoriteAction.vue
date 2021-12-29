<template>
  <favorite-button
    :id="favoriteId"
    :favorite="isFavorite"
    :absolute="absolute"
    :top="top"
    :right="right"
    :space-id="spaceId"
    type="file"
    :template-params="templateParams"
    :small="false"
    @removed="removed"
    @remove-error="removeError"
    @added="added"
    @add-error="addError" />
</template>

<script>
export default {
  props: {
    document: {
      type: Object,
      default: null,
    },
    absolute: {
      type: Boolean,
      default: false,
    },
    top: {
      type: Number,
      default: () => 0,
    },
    right: {
      type: Number,
      default: () => 0,
    },
  },
  data: () => ({
    spaceId: null,
    templateParams: {},
  }),
  computed: {
    isFavorite() {
      return this.document.metadatas && this.document.metadatas.favorites && this.document.metadatas.favorites.length;
    },
    favoriteId() {
      return this.document.activityId ? this.document.activityId : this.document.id;
    }
  },
  watch: {
    document() {
      if (this.document) {
        this.templateParams.page_id = this.document.id;
      }
    }
  },
  methods: {
    removed() {
      this.displayAlert(this.$t('Favorite.tooltip.SuccessfullyDeletedFavorite'));
      this.$favoriteService.removeFavorite('file', this.favoriteId)
        .then(() => {
          this.isFavorite = false;
          this.$emit('removed');
        })
        .catch(() => this.$emit('remove-error'));
    },
    removeError() {
      this.displayAlert(this.$t('Favorite.tooltip.ErrorDeletingFavorite', 'document'), 'error');
    },
    added() {
      this.displayAlert(this.$t('Favorite.tooltip.SuccessfullyAddedAsFavorite'));
      this.$favoriteService.addFavorite('file', this.favoriteId)
        .then(() => {
          this.isFavorite = true;
          this.$emit('added');
        })
        .catch(() => this.$emit('add-error'));
    },
    addError() {
      this.displayAlert(this.$t('Favorite.tooltip.ErrorAddingAsFavorite', 'file'), 'error');
    },
    displayAlert(message, type) {
      this.$root.$emit('notes-notification-alert', {
        message,
        type: type || 'success',
      });
    },
  },
};
</script>