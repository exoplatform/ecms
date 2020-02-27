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
            row
            mx-0
            align-center>
            <v-flex
              d-flex
              xs12>
              <v-card
                flat
                color="transparent">
                <a @click="navigateTo('tasks/my-task')"><v-card-text class="body-1 text-uppercase color-title px-0">
                  {{ this.$t('homepage.tasks.header') }}
                </v-card-text></a>
              </v-card>
            </v-flex>
          </v-layout>
        </v-flex>
        <v-flex
          xs12
          px-3>
          <v-layout
            row
            mx-0
            align-center>
            <v-flex
              d-flex
              xs12>
              <v-layout mx-0>
                <v-flex xs12>
                  <v-card
                    class="mx-auto"
                    flat
                    color="#F7FAFD">
                    <v-layout>
                      <v-flex>
                        <v-layout
                          row
                          mx-0
                          justify-center>
                          <v-flex
                            xs12
                            d-flex>
                            <v-layout mx-0 pl-2>
                              <v-flex
                                d-flex
                                xs4>
                                <v-layout
                                  mx-0
                                  justify-center
                                  class="py-2">
                                  <template>
                                    <div class="text-center">
                                      <v-avatar tile color="#F7FAFD">
                                        <i class="uiIconEcmsViewTimeline iconlarge"></i>
                                      </v-avatar>
                                    </div>
                                  </template>
                                </v-layout>
                              </v-flex>
                              <v-flex
                                d-flex
                                xs8>
                                <v-card
                                  class="py-1"
                                  flat
                                  color="#F7FAFD"
                                  @click="navigateTo('tasks')">
                                  <div class="title">{{ incomingTasksSize }} {{ this.$t('homepage.tasks') }}</div>
                                  <div class="caption color-title">{{ this.$t('homepage.tasks.incoming') }}</div>
                                </v-card>
                              </v-flex>
                            </v-layout>
                          </v-flex>
                        </v-layout>
                      </v-flex>
                    </v-layout>
                  </v-card>
                </v-flex>
                <v-flex xs12>
                  <v-card
                    class="mx-auto"
                    flat
                    color="#F7FAFD">
                    <v-layout>
                      <v-flex>
                        <v-layout
                          row
                          mx-0
                          justify-center>
                          <v-flex
                            xs12
                            d-flex>
                            <v-layout mx-0>
                              <v-flex
                                d-flex
                                xs5>
                                <v-layout
                                  mx-0
                                  justify-center
                                  class="pa-2">
                                  <template>
                                    <div class="text-center">
                                      <v-avatar tile color="#F7FAFD">
                                        <v-icon
                                          size="50"
                                          color="#D07B7B">mdi-alarm</v-icon>
                                      </v-avatar>
                                    </div>
                                  </template>
                                </v-layout>
                              </v-flex>
                              <v-flex
                                d-flex
                                xs7>
                                <v-card
                                  class="py-1"
                                  flat
                                  color="#F7FAFD"
                                  @click="navigateTo('tasks/my-task/overdue')">
                                  <div class="title">{{ overdueTasksSize }} {{ this.$t('homepage.tasks') }}</div>
                                  <div class="caption color-title">{{ this.$t('homepage.tasks.late') }}</div>
                                </v-card>
                              </v-flex>
                            </v-layout>
                          </v-flex>
                        </v-layout>
                      </v-flex>
                    </v-layout>
                  </v-card>
                </v-flex>
              </v-layout>
          </v-flex></v-layout>
        </v-flex>
        <v-flex
          d-flex
          xs12
          pl-2
          pt-4>
          <v-layout
            row
            mx-0>
            <v-flex
              xs12>
              <v-list>
                <v-list-item
                  v-for="task in tasks"
                  :key="task.title"
                  class="px-0">
                  <task-details :task="task"/>
                </v-list-item>
              </v-list>
        </v-flex></v-layout></v-flex>
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
        return Promise.resolve('Success');
      },
      saveEditor() {
        return Promise.resolve('Success');
      }
    }
  }
</script>