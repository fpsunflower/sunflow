package org.sunflow.system;

import org.sunflow.system.ui.ConsoleInterface;
import org.sunflow.system.ui.SilentInterface;

/**
 * Static singleton interface to a UserInterface object. This is set to a text
 * console by default.
 */
public final class UI {
    private static UserInterface ui = new ConsoleInterface();
    private static boolean canceled = false;
    private static int verbosity = 3;

    private UI() {
    }

    /**
     * Sets the active user interface implementation. Passing <code>null</code>
     * silences printing completely (by using the
     * 
     * @see SilentInterface).
     * @param ui
     */
    public final static void set(UserInterface ui) {
        if (ui == null)
            ui = new SilentInterface();
        UI.ui = ui;
    }

    public final static void verbosity(int verbosity) {
        UI.verbosity = verbosity;
    }

    public final static synchronized void printDetailed(String s, Object... args) {
        if (verbosity > 3)
            ui.printDetailed(String.format(s, args));
    }

    public final static synchronized void printInfo(String s, Object... args) {
        if (verbosity > 2)
            ui.printInfo(String.format(s, args));
    }

    public final static synchronized void printWarning(String s, Object... args) {
        if (verbosity > 1)
            ui.printWarning(String.format(s, args));
    }

    public final static synchronized void printError(String s, Object... args) {
        if (verbosity > 0)
            ui.printError(String.format(s, args));
    }

    public final static synchronized void taskStart(String s, int min, int max) {
        ui.taskStart(s, min, max);
    }

    public final static synchronized void taskUpdate(int current) {
        ui.taskUpdate(current);
    }

    public final static synchronized void taskStop() {
        ui.taskStop();
        // reset canceled status ~ assume the parent application will deal with
        // it immediately
        canceled = false;
    }

    /**
     * Cancel the currently active task. This forces the application to abort as
     * soon as possible.
     */
    public final static synchronized void taskCancel() {
        printInfo("[GUI] Abort requested by the user ...");
        canceled = true;
    }

    /**
     * Check to see if the current task should be aborted.
     * 
     * @return <code>true</code> if the current task should be stopped,
     *         <code>false</code> otherwise
     */
    public final static synchronized boolean taskCanceled() {
        if (canceled)
            printInfo("[GUI] Abort request noticed by the current task");
        return canceled;
    }
}