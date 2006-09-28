package org.sunflow.system;

public interface UserInterface {
    /**
     * Displays some information which would be considered detailed and not
     * needed during typical runs.
     * 
     * @param s string to display
     */
    void printDetailed(String s);

    /**
     * Displays an informative string to the user. It is assumed the string
     * corresponds to one line only.
     * 
     * @param s string to display
     */
    void printInfo(String s);

    /**
     * Displays a warning to the user. It is assumed the string corresponds to
     * one line only.
     * 
     * @param s string to display
     */

    void printWarning(String s);

    /**
     * Displays an error message to the user. It is assumed the string
     * corresponds to one line only.
     * 
     * @param s string to display
     */
    void printError(String s);

    /**
     * Prepare a progress bar representing a lengthy task. The actual progress
     * is first shown by the call to update and closed when update is closed
     * with the max value. It is currently not possible to nest calls to
     * setTask, so only one task needs to be tracked at a time.
     * 
     * @param s desriptive string
     * @param min minimum value of the task
     * @param max maximum value of the task
     */
    void taskStart(String s, int min, int max);

    /**
     * Updates the current progress bar to a value between the current min and
     * max. When min or max are passed the progressed bar is shown or hidden
     * respectively.
     * 
     * @param current current value of the task in progress.
     */
    void taskUpdate(int current);

    /**
     * Closes the current progress bar to indicate the task is over
     */
    void taskStop();
}