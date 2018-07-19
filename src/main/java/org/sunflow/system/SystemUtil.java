package org.sunflow.system;

public class SystemUtil {

    /**
     * This is a quick system test which verifies that the user has launched
     * Java properly.
     */
    public static void runSystemCheck() {
        final long RECOMMENDED_MAX_SIZE = 800;
        long maxMb = Runtime.getRuntime().maxMemory() / 1048576;
        if (maxMb < RECOMMENDED_MAX_SIZE)
            UI.printError(UI.Module.API, "JVM available memory is below %d MB (found %d MB only).\nPlease make sure you launched the program with the -Xmx command line options.", RECOMMENDED_MAX_SIZE, maxMb);
        String compiler = System.getProperty("java.vm.name");
        if (compiler == null || !(compiler.contains("HotSpot") && compiler.contains("Server")))
            UI.printError(UI.Module.API, "You do not appear to be running Sun's server JVM\nPerformance may suffer");
        UI.printDetailed(UI.Module.API, "Java environment settings:");
        UI.printDetailed(UI.Module.API, "  * Max memory available : %d MB", maxMb);
        UI.printDetailed(UI.Module.API, "  * Virtual machine name : %s", compiler == null ? "<unknown" : compiler);
        UI.printDetailed(UI.Module.API, "  * Operating system     : %s", System.getProperty("os.name"));
        UI.printDetailed(UI.Module.API, "  * CPU architecture     : %s", System.getProperty("os.arch"));
    }

}
