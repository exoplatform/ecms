import { shallowMount } from "@vue/test-utils";
import ConnectCloudDrive from "../../main/webapp/vue-app/components/ConnectCloudDrive.vue";
import Vuetify from "vuetify";
import Vue from "vue";

describe("ConnectCloudDrive.test.js", () => {

  beforeEach(() => {
    Vue.use(Vuetify);
  });

  it("should get userDrive and display list with providers after component creation", done => {
    const mockJsonPromise = Promise.resolve({
      name: "Personal Documents",
      workspace: "collaboration",
      homePath: "/Personal__Documents"
    });
    const mockFetchPromise = Promise.resolve({
      ok: true,
      json: () => mockJsonPromise
    });
    global.fetch = jest.fn().mockResolvedValue(mockFetchPromise);

    const wrapper = shallowMount(ConnectCloudDrive, {
      mocks: {
        $t: () => {
          // mocked translate function
        },
      },
      propsData: {
        showCloudDrawer: false,
        currentDrive: { isSelected: true, name: "Personal Documents", title: "Personal Documents" }
      }
    });
    const cmp = wrapper.vm;

    process.nextTick(() => {
      expect(global.fetch).toHaveBeenCalledTimes(1);
      expect(cmp.userDrive).toEqual({ name: "Personal Documents", title: "Personal Documents", isSelected: false });
      expect(wrapper.findAll(".cloudDriveListItem")).toHaveLength(2);
      global.fetch.mockClear();
      wrapper.destroy();
      done();
    });
  });
});
