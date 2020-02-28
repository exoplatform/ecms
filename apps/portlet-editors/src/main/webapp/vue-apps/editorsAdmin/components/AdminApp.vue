<template>
  <v-app
    id="editors-admin"
    class="VuetifyApp"
    flat>
    <v-container pa-0>
      <v-layout
        row
        mx-0
        class="white">
        <v-flex
          d-flex
          xs12
          px-3>
          <v-layout
            mx-0
            align-center>
            <v-flex
              d-flex
              xs12>
              <v-card
                flat
                color="transparent">
                <a @click="navigateTo('tasks/my-task')"><v-card-text class="body-1 text-uppercase color-title px-0">
                  {{ this.$t('editors.admin.title') }}
                </v-card-text></a>
              </v-card>
            </v-flex>
          </v-layout>
        </v-flex>
      </v-layout>
    </v-container>
  </v-app>
</template>

<script>
  //import TaskDrawer from "./TaskDrawer.vue";
  //import {getMyAllTasks, getMyIncomingTasks, getMyOverdueTasks} from '../TasksAPI'

  export default {

    data() {
      // TODO Initial data?
      return {
        my_data: "the value",
        another_data: "another value"
      }
    },
    created(){
      // TODO initialization here?
      //this.getMyAllTasks();
      //this.getMyIncomingTasksSize();
      //this.getMyOverdueTasksSize();
    },
    methods: {
      getEditors() {
        return Promise.resolve("Success");
      },
      saveEditor() {
        return Promise.resolve("Success");
      }
    }
  }
</script>