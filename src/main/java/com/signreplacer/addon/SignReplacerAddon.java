package com.signreplacer.addon;

import com.signreplacer.addon.modules.SignReplacer;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SignReplacerAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger(SignReplacerAddon.class);
    public static final Category CATEGORY = new Category("Sign Replacer");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Sign Replacer Addon");

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
}
