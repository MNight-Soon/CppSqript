package com.mnight.cppsqript;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Cppsqript.MOD_ID)
public class Cppsqript {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "cppsqript";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "cppsqript" namespace
    private final CppBridge cppBridge;
    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Cppsqript(IEventBus modEventBus) {
        LOGGER.info("Initializing CppScript library");

        this.cppBridge = new CppBridge();

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        String osName = System.getProperty("os.name").toLowerCase();
        String libName = osName.contains("win") ? "script.dll" : "libsqript.so";

        LOGGER.info("Loading C++ library: {}", libName);

        if (cppBridge.loadLibrary(libName)) {
            cppBridge.executeServerInit();
        }
    }
}
