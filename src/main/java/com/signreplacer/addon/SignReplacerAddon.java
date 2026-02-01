package com.signreplacer.addon;

import com.signreplacer.addon.modules.SignReplacer;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignReplacerAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger(SignReplacerAddon.class);
    public static final Category CATEGORY = new Category("Oxygen Mod");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Oxygen Mod");

        // Register modules
        Modules.get().add(new SignReplacer());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.signreplacer.addon";
    }

    @Override
    public String getWebsite() {
        return "https://github.com/QuiveringMudflap/sign-replacer-addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("QuiveringMudflap", "sign-replacer-addon");
    }
}
