<template>
  <div id="connectCloudApp">
    <div :class="{ open: showDrawer }" class="connect-cloud drawer ignore-vuetify-classes">
      <div style="height: 600px">Cloud Drive Drawer</div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      showDrawer: false
    };
  }
};
</script>
