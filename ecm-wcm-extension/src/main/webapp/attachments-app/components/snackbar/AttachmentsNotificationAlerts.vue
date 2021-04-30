<template>
  <v-snackbar
    :value="displayAlerts"
    color="transparent"
    elevation="0"
    absolute
    bottom
    left>
    <attachments-notification-alert
      v-for="alert in alerts"
      :key="alert.message"
      :alert="alert"
      @dismissed="deleteAlert(alert)" />
  </v-snackbar>
</template>

<script>
export default {
  props: {
    name: {
      type: String,
      default: null
    },
  },
  data: () => ({
    alerts: [],

  }),
  computed: {
    displayAlerts() {
      return this.alerts && this.alerts.length;
    },
  },
  created() {
    this.$root.$on('attachments-notification-alert', this.addAlert);
  },
  methods: {
    addAlert(alert) {
      if (alert) {
        this.alerts.push(alert);
        // eslint-disable-next-line no-magic-numbers
        window.setTimeout(() => this.deleteAlert(alert), 5000);
      }
    },
    deleteAlert(alert) {
      const index = this.alerts.indexOf(alert);
      this.alerts.splice(index, 1);
      this.$forceUpdate();
    },
  },
};
</script>
