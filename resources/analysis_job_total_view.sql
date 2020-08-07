CREATE 
    ALGORITHM = UNDEFINED 
    DEFINER = `gpbeta`@`%` 
    SQL SECURITY DEFINER
VIEW `analysis_job_total` AS
    SELECT 
        `analysis_job`.`JOB_NO` AS `JOB_NO`,
        `analysis_job`.`TASK_ID` AS `TASK_ID`,
        `analysis_job`.`STATUS_ID` AS `STATUS_ID`,
        `analysis_job`.`DATE_SUBMITTED` AS `DATE_SUBMITTED`,
        `analysis_job`.`DATE_COMPLETED` AS `DATE_COMPLETED`,
        `analysis_job`.`USER_ID` AS `USER_ID`,
        `analysis_job`.`ISINDEXED` AS `ISINDEXED`,
        `analysis_job`.`ACCESS_ID` AS `ACCESS_ID`,
        `analysis_job`.`JOB_NAME` AS `JOB_NAME`,
        `analysis_job`.`LSID` AS `LSID`,
        `analysis_job`.`TASK_LSID` AS `TASK_LSID`,
        `analysis_job`.`TASK_NAME` AS `TASK_NAME`,
        `analysis_job`.`PARENT` AS `PARENT`,
        `analysis_job`.`DELETED` AS `DELETED`
    FROM
        `analysis_job` 
    UNION SELECT 
        `analysis_job_archive`.`JOB_NO` AS `JOB_NO`,
        `analysis_job_archive`.`TASK_ID` AS `TASK_ID`,
        `analysis_job_archive`.`STATUS_ID` AS `STATUS_ID`,
        `analysis_job_archive`.`DATE_SUBMITTED` AS `DATE_SUBMITTED`,
        `analysis_job_archive`.`DATE_COMPLETED` AS `DATE_COMPLETED`,
        `analysis_job_archive`.`USER_ID` AS `USER_ID`,
        `analysis_job_archive`.`ISINDEXED` AS `ISINDEXED`,
        `analysis_job_archive`.`ACCESS_ID` AS `ACCESS_ID`,
        `analysis_job_archive`.`JOB_NAME` AS `JOB_NAME`,
        `analysis_job_archive`.`LSID` AS `LSID`,
        `analysis_job_archive`.`TASK_LSID` AS `TASK_LSID`,
        `analysis_job_archive`.`TASK_NAME` AS `TASK_NAME`,
        `analysis_job_archive`.`PARENT` AS `PARENT`,
        `analysis_job_archive`.`DELETED` AS `DELETED`
    FROM
        `analysis_job_archive`