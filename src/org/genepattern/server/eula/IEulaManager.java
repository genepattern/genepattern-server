package org.genepattern.server.eula;

import java.util.List;

import org.genepattern.server.config.ServerConfiguration.Context;

/**
 * Methods for managing End-user license agreements (EULA) for GenePattern modules and pipelines.
 * 
 * @author pcarr
 */
public interface IEulaManager {

    /**
     * Optionally set the strategy for getting the list (if any) of EULA
     * which are required for a particular module or pipeline.
     * 
     * @param impl, an object which implements the GetEulaFromTask interface, can be null.
     */
    void setGetEulaFromTask(GetEulaFromTask impl);

    /**
     * Optionally set the strategy for initializing a TaskInfo from a task lsid.
     * 
     * @param impl, an object which implements this interface, can be null.
     */
    void setGetTaskStrategy(GetTaskStrategy impl);

    /**
     * Optionally set the strategy for recording user agreement to the local database.
     * 
     * @param impl, an object which implements the RecordEula interface, can be null.
     */
    void setRecordEulaStrategy(final RecordEula impl);

    /**
     * Implement a run-time check, before starting a job, verify that there are
     * no EULA which the current user has not yet agreed to.
     * 
     * Returns true, if,
     *     1) the module requires no EULAs, or
     *     2) the current user has agreed to all EULAs for the module.
     * 
     * @param taskContext, must have a valid user and taskInfo
     * @return true if there is no record of EULA for the current user.
     */
    boolean requiresEula(Context taskContext);

    /**
     * Get the list of all End-user license agreements for the given module or pipeline.
     * This list includes all EULA, even if the current user has already agreed to them.
     * 
     * @param taskContext, must have a valid taskInfo object
     * @return
     */
    List<EulaInfo> getAllEulaForModule(final Context taskContext);

    /**
     * Get the list of End-user license agreements for the given module or pipeline, for
     * which to prompt for agreement from the current user.
     * 
     * Use this list as the basis for prompting the current user for agreement before
     * going to the job submit form for the task.
     * 
     * @param taskContext
     * @return
     */
    List<EulaInfo> getPendingEulaForModule(final Context taskContext);

    /**
     * In response to user acceptance by clicking the 'Ok' button in the GUI,
     * store a local record that the user has agreed.
     * 
     * When the taskInfo is a pipeline, accept all agreements.
     * 
     * This also, optionally, schedules remote recording of the eula.
     * 
     * @param taskContext, must not be null, and
     *     must have a non-null and valid taskInfo, and
     *     must have a non-null and valid userId
     *     
     * @throws IllegalArgumentException if the taskContext is not initialized properly.
     */
    void recordEula(final Context taskContext) throws IllegalArgumentException;

}