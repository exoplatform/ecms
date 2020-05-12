import { shallowMount } from "@vue/test-utils";
import ConnectCloudDrive from "../../main/webapp/vue-app/components/ConnectCloudDrive.vue";
import Vuetify from "vuetify";
import Vue from "vue";

function mountWrapper() {
  return shallowMount(ConnectCloudDrive, {
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
}

function mockSuccessfullRequest(response) {
  const mockJsonPromise = Promise.resolve(response);
  const mockFetchPromise = Promise.resolve({
    ok: true,
    json: () => mockJsonPromise
  });
  global.fetch = jest.fn().mockResolvedValue(mockFetchPromise);
}

describe("ConnectCloudDrive.test.js", () => {

  beforeEach(() => {
    Vue.use(Vuetify);
  });

  it("should get userDrive and display list with providers after component creation", done => {
    mockSuccessfullRequest({
      name: "Personal Documents",
      workspace: "collaboration",
      homePath: "/Personal__Documents"
    });
    const wrapper = mountWrapper();
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

  it("should emit update progress event after connect button click", done => {
    const wrapper = mountWrapper();
    const cmp = wrapper.vm;
    const providers = cloudDrive.getProviders();
    wrapper.setData({ providers: providers });
    const connectToCloudDrive = jest.spyOn(cmp, "connectToCloudDrive");

    process.nextTick(() => {
      expect(wrapper.findAll(".cloudDriveListItem")).toHaveLength(2);
      wrapper.find(".cloudDriveListItem").trigger("click");
      expect(connectToCloudDrive).toHaveBeenCalledWith("gDrive");
      expect(wrapper.emitted().updateProgress).toBeTruthy();
      wrapper.destroy();
      done();
    });
  });

  it("should close drawer after click on close button ", () => {
    const wrapper = mountWrapper();
    const cmp = wrapper.vm;
    wrapper.setData({ showCloudDrawer: true });
    wrapper.find(".backButton").trigger("click");
    expect(cmp.showCloudDrawer).toBeFalsy();
    wrapper.destroy();
  });

  it("should display notification in case of error", done => {
    const mockFetchPromise = Promise.reject(new Error("somethind went wrong"));
    global.fetch = jest.fn().mockResolvedValue(mockFetchPromise);
    const wrapper = mountWrapper();
    const notifyError = jest.spyOn($, "pnotify");

    process.nextTick(() => {
      expect(global.fetch).toHaveBeenCalledTimes(1);
      expect(notifyError).toHaveBeenCalled();
      global.fetch.mockClear();
      wrapper.destroy();
      done();
    });
  });
});
