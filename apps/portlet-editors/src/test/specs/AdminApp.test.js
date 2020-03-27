import { shallowMount } from "@vue/test-utils";
import AdminApp from "../../main/webapp/vue-apps/editorsAdmin/components/AdminApp.vue";
import Vuetify from "vuetify";
import Vue from "vue";

describe("AdminApp.test.js", () => {
  let cmp;
  const data = {
    permissions: [
      {
        id: "/platform",
        displayName: "Platform",
        avatarUrl: "/eXoSkin/skin/images/system/SpaceAvtDefault.png"
      }
    ],
    provider: "onlyoffice",
    active: true,
    links: [
      {
        rel: "self",
        href: "/rest/documents/editors/onlyoffice"
      },
      {
        rel: "update",
        href: "/rest/documents/editors/onlyoffice"
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
        i18n: { te: () => true },
        language: "en",
        resourceBundleName: "localizationBundle"
      },
      stubs: ["edit-dialog"]
    });
  });

  it("should be a Vue instance", () => {
    expect(cmp.isVueInstance).toBeTruthy();
  });
  
  it("should display providers table", () => {
    cmp.vm.providers = data.editors;

    const providersTable = cmp.findAll(".providersTable");
    expect(providersTable).toHaveLength(1);
  });
});
