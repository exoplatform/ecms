<template>
  <favorite-button
    :id="favoriteId"
    :favorite="isFavorite"
    :absolute="absolute"
    :top="top"
    :right="right"
    :space-id="spaceId"
    :template-params="templateParams"
    :small="false"
    type="file"
    type-label="Documents"
    @removed="removed"
    @remove-error="removeError"
    @added="added"
    @add-error="addError" />
</template>

<script>
export default {
  props: {
    file: {
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
    templateParams: {},
  }),
  computed: {
    isFavorite() {
      return this.file && this.file.metadatas && this.file.metadatas.favorites && this.file.metadatas.favorites.length;
    },
    favoriteId() {
      return  this.file && this.file.id;
    },
    spaceId() {
      return this.file?.space?.id;
    }
  },
  watch: {
    file() {
      if (this.file) {
        this.templateParams.page_id = this.file.id;
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
      document.dispatchEvent(new CustomEvent('notification-alert', {detail: {
        message,
        type: type || 'success',
      }}));
    },
  },
};
</script>