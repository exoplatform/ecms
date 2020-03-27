import { shallowMount } from "@vue/test-utils";
import EditDialog from "../../main/webapp/vue-apps/editorsAdmin/components/EditDialog.vue";
import Vuetify from "vuetify";
import Vue from "vue";

describe("EditDialog.test.js", () => {
  let cmp;
  const data = {
    permissions: [
      {
        id: "/platform",
        displayName: "Platform",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      }
    ]
  };

  beforeEach(() => {
    Vue.use(Vuetify);

    cmp = shallowMount(EditDialog, {
      mocks: {
        $t: () => {
          // mocked translate function
        }
      },
      propsData: {
        providerLink: "/rest/documents/editors/onlyoffice",
        providerName: "onlyoffice",
        searchUrl: "portal/rest/identity/search",
        i18n: { te: () => true }
      }
    });
  });

  afterEach(() => cmp.destroy());

  it("should be a Vue instance", () => {
    expect(cmp.isVueInstance).toBeTruthy();
  });

  it("should change component selected permissions after selection changes", () => {
    expect(cmp.contains(".searchPermissions")).toBe(true);
    const autoComplete = cmp.find(".searchPermissions");
    autoComplete.vm.$emit("input", [
      {
        type: "group",
        id: "/platform/web-contributors",
        displayName: "Content Management",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      }
    ]);
    expect(autoComplete.emitted().input).toBeTruthy();
    expect(cmp.vm.selectedItems).toEqual([
      {
        type: "group",
        id: "/platform/web-contributors",
        displayName: "Content Management",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      }
    ]);
  });

  it("should fetch searched permissions by value in search property", async () => {
    expect(cmp.contains(".searchPermissions")).toBeTruthy();
    const mockJsonPromise = Promise.resolve({
      identities: [
        {
          type: "group",
          id: "/platform/web-contributors",
          displayName: "Content Management",
          avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
        },
        {
          type: "group",
          id: "/organization",
          displayName: "Organization",
          avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
        }
      ]
    });
    const mockFetchPromise = Promise.resolve({
      json: () => mockJsonPromise
    });
    global.fetch = jest.fn().mockImplementation(() => mockFetchPromise);

    const querySelections = jest.spyOn(cmp.vm, "querySelections");
    cmp.vm.search = "pla";
    await Vue.nextTick();
    expect(querySelections).toHaveBeenCalledWith("pla");
    expect(global.fetch).toHaveBeenCalledTimes(1);
    expect(global.fetch).toHaveBeenCalledWith("portal/rest/identity/search/pla", {
      headers: { "Content-Type": "application/json" },
      method: "GET"
    });
    global.fetch.mockClear();
  });
});
