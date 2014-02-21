package org.genepattern.server.eula;

import java.util.List;

import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.TaskInfo;

/**
 * Methods for managing End-user license agreements (EULA) for GenePattern modules and pipelines.
 * 
 * @author pcarr
 */
public interface IEulaManager {
    /**
     * Implement a run-time check, before starting a job, verify that there are
     * no EULA which the current user has not yet agreed to.
     * 
     * Returns true, if,
     *     1) the module requires no EULAs, or
     *     2) the current user has agreed to all EULAs for the module.
     * 
     * @param taskContext, must not be null, and
     *     must have a non-null and valid taskInfo, and
     *     must have a non-null and valid userId
     * 
     * @return true if there is no record of EULA for the current user.
     */
    boolean requiresEula(GpContext taskContext);

    /**
     * For an individual module or task, get the list of zero or more End-user license agreements (EULA)
     * attached to the task.
     * 
     * This returns the default license for the module or pipeline. When there is no license, it will return an empty list.
     * 
     * Note: Even when the license manager has been disabled, this method will return the default license.
     * In other words, disabling the license manager will have no effect on the return value for this method.
     * The license manager can be disabled in several ways,
     *     1) globally, presumably for debugging
     *     2) site license,
     *     3) group license 
     *     
     * Don't use this method from the EulaTaskBean, because it will not have the desired result when the license is disabled.
     * Instead use getAllEulaForModule or getPendingEulaForModule.
     * 
     * @param taskInfo
     * @return the list of EulaInfo, it will be empty if the task has not attached EULA.
     */
    List<EulaInfo> getEulas(TaskInfo taskInfo);
    
    /**
     * For an individual module or task, set the End-user license agreement (EULA) for the task.
     * Can be null, which means, 'this module (or pipeline) has no EULA'.
     * 
     * This method may make changes to the TaskInfo arg. Make sure to persist those changes to the DB
     * after you call this method.

     * @param eula
     * @param taskInfo
     */
    void setEula(EulaInfo eula, TaskInfo taskInfo);

    /**
     * For an individual module or task, set the list of zero or more End-user license agreements (EULA)
     * attached to the task.
     * 
     * This method may make changes to the TaskInfo arg. Make sure to persist those changes to the DB
     * after you call this method.
     * 
     * @param eulas
     * @param taskInfo
     */
    void setEulas(List<EulaInfo> eulas, TaskInfo taskInfo);
    

    /**
     * Get the list of all End-user license agreements for the given module or pipeline.
     * This list includes all EULA, even if the current user has already agreed to them.
     * 
     * @param taskContext, must not be null, and
     *     must have a non-null and valid taskInfo, and
     *     must have a non-null and valid userId
     * 
     * @return
     */
    List<EulaInfo> getAllEulaForModule(GpContext taskContext);

    /**
     * Get the list of End-user license agreements for the given module or pipeline, for
     * which to prompt for agreement from the current user.
     * 
     * Use this list as the basis for prompting the current user for agreement before
     * going to the job submit form for the task.
     * 
     * @param taskContext, must not be null, and
     *     must have a non-null and valid taskInfo, and
     *     must have a non-null and valid userId
     * 
     * @return
     */
    List<EulaInfo> getPendingEulaForModule(GpContext taskContext);

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
    void recordEula(final GpContext taskContext) throws IllegalArgumentException;

}