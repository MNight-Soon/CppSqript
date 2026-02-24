package com.mnight.cppsqript;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CppBridge {
    private static final Logger LOGGER = LoggerFactory.getLogger(CppBridge.class);

    public interface CppScriptLibrary extends Library {
        void onServerInit();
    }

    private CppScriptLibrary cppLibrary;

    public boolean loadLibrary(String libraryPath) {
        File libfile = new File(libraryPath);
        if (!libfile.exists()) {
            LOGGER.warn("Library file does not exist: " + libraryPath);
            return false;
        }

        try {
            // โหลดไฟล์เข้าสู่ระบบ JNA
            cppLibrary = Native.load(libraryPath, CppScriptLibrary.class);
            LOGGER.info("Load C++ success: {}", libraryPath);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to load C++: ", e);
            return false;
        }
    }

    public void executeServerInit() {
        if (cppLibrary != null) {
            LOGGER.info("running onServerInit() from C++...");
            cppLibrary.onServerInit();
        } else  {
            LOGGER.info("The C++ function cannot be called because the library has not been loaded.");
        }
    }
}
