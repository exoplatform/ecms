import { shallowMount } from "@vue/test-utils";
import AdminApp from "../../main/webapp/vue-apps/editorsAdmin/components/AdminApp.vue";
import Vuetify from "vuetify";
import Vue from "vue";

describe("AdminApp.test.js", () => {
  let cmp;
  const data = {
    editors: [
      {
        permissions: [
          {
            id: "/platform",
            displayName: "Platform",
            avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
          }
        ],
        provider: "onlyoffice",
        active: true,
        links: {
          self: {
            href: "/rest/documents/editors/onlyoffice"
          }
        }
      }
    ]
  };

  beforeEach(() => {
    Vue.use(Vuetify);

    cmp = shallowMount(AdminApp, {
      mocks: {
        $t: () => {
          // mocked translate function
        }
      },
      propsData: {
        services: { providers: "providers", identities: "identity/search" },
        i18n: { te: () => true, mergeLocaleMessage: () => {
          // mocked i18n merge function
        } },
        language: "en",
        resourceBundleName: "localizationBundle"
      },
      stubs: ["edit-dialog"],
      data: function() {
        return {
          providers: data.editors
        }
      }
    });
  });

  it("should be a Vue instance", () => {
    expect(cmp.isVueInstance).toBeTruthy();
  });

  it("should display providers table", () => {
    const providersTable = cmp.findAll(".providersTable");
    expect(providersTable).toHaveLength(1);
  });

  it("should change providers active status to false", (done) => {
    const selectedProvider = cmp.vm.providers.find(({ provider }) => provider === "onlyoffice")
    expect(selectedProvider.active).toBeTruthy();
    const mockJsonPromise = Promise.resolve({});
    const mockFetchPromise = Promise.resolve({
      ok: true,
      json: () => mockJsonPromise
    });
    global.fetch = jest.fn().mockResolvedValue(mockFetchPromise);
    cmp.vm.changeActive(selectedProvider);
    process.nextTick(() => {
      expect(global.fetch).toHaveBeenCalled();
      expect(global.fetch).toHaveBeenCalledWith("/rest/documents/editors/onlyoffice", {
        body: JSON.stringify({ active: false }),
        headers: { "Content-Type": "application/json" },
        method: "POST"
      });
      global.fetch.mockClear();
      done();
    });
  });
});
