/**
 * @author Thorin Tabor
 *
 * Library for interfacing with GenePattern REST API from JavaScript.
 */
"use strict";

/**
 * Declaration of top gp library namespace
 *
 * @required - jQuery 1.5+ library
 */
var gp = gp || {};
gp._server = null;
gp._tasks = null;
gp._jobs = null;


/**
 * Sets the URL to the GP server
 * Example: http://genepattern.broadinstitute.org/gp
 *
 * @param url - URL to server including the /gp (or similar)
 */
gp.setServer = function(url) {
    gp._server = url;
};


/**
 * Easily determine if the URL to the GenePattern server has been set or not.
 *
 * @returns {boolean} - true if the server has been set, else false
 */
gp.isServerSet = function() {
    return gp._server ? true : false;
};


/**
 * Returns the server at which this library is pointed
 * @returns {string|null}
 */
gp.server = function() {
    return gp._server;
};


/**
 * Queries for a list of all tasks available to the user. If this data is not yet cached, it will
 * make an AJAX request to get the info, and make a callback once available. If this data is already
 * cached, it will immediately make the callback with the cached data.
 *
 * This does not retrieve or cache parameter information. For that use Task.load()
 *
 * @param pObj - An object that can set the following options:
 *                  force: force an AJAX call, regardless of whether data is cached
 *                  hidden: include hidden modules? default: false
 *                  success: callback function for a done() event,
 *                          expects response and list of Task objects as arguments
 *                  error: callback function for an fail() event, expects exception as argument
 *
 * @returns {jQuery.Deferred} - Returns a jQuery Deferred object for event chaining.
 *      See http://api.jquery.com/jquery.deferred/ for details.
 */
gp.tasks = function(pObj) {
    var forceRefresh = pObj && ((typeof pObj.force === 'boolean' && pObj.force) ||
        (typeof pObj.force === 'string' && pObj.force.toLowerCase() === 'true'));
    var useCache = gp._tasks && !forceRefresh;

    if (useCache) {
        return new $.Deferred()
            .done(function() {
                if (pObj && pObj.success) {
                    pObj.success("cached", gp._tasks);
                }
            })
            .resolve();
    }
    else {
        var REST_ENDPOINT = "/rest/v1/tasks/all.json";
        var includeHidden = pObj && pObj.hidden && pObj.hidden.toLowerCase() === 'true' ? '?includeHidden=true' : '';

        return $.ajax({
            url: gp.server() + REST_ENDPOINT + includeHidden,
            type: 'GET',
            dataType: 'json',
            xhrFields: {
                withCredentials: true
            }
        })
            .done(function(response) {
                // Create the new _tasks list and iterate over returned JSON list, creating Task objects
                gp._tasks = [];
                var modules = response['all_modules'];
                if (modules) {
                    for (var i = 0; i < modules.length; i++) {
                        var json = modules[i];
                        gp._tasks.push(new gp.Task(json));
                    }
                }

                if (pObj && pObj.success) {
                    pObj.success(response, gp._tasks);
                }
            })
            .fail(function(exception) {
                if (pObj && pObj.error) {
                    pObj.error(exception);
                }
            });
    }
};


/**
 * Returns a cached Task() object matching the provided LSID or module name
 *
 * @param pObj - An object specifying of one these two properties:
 *                  lsid: the LSID of the task to load from the server
 *                  name: the name of the task to load from the server
 *                      Alternately a string can be passed in containing LSID or name
 *                      If nothing is defined, an error will be thrown.
 *
 * @returns {gp.Task|null} - The Task object from the cache
 */
gp.task = function(pObj) {
    // Ensure either lsid or name is defined
    if (!pObj) throw "gp.task() parameter either null or undefined";
    if (typeof pObj === 'object' && !pObj.lsid && !pObj.name) throw "gp.task() parameter does not contain lsid or name";
    if (typeof pObj !== 'string' && typeof pObj !== 'object') throw "gp.task() parameter must be either object or string";
    if (gp._tasks === null) throw "gp task list has not been initialized";

    var identifier = typeof pObj === 'string'? pObj : null;

    for (var i = 0; i < gp._tasks.length; i++) {
        var task = gp._tasks[i];
        if (task.lsid() === pObj.lsid || task.lsid() === identifier) return task;
        if (task.name() === pObj.name || task.name() === identifier) return task;
    }

    return null;
};


/**
 * Returns a list of jobs on the server and caches those jobs.
 * To begin a new search include the force parameter
 *
 * @param pObj - An object specifying which jobs to select:
 *                  force: do not use cache, force a new search
 *                  success: callback function for a done() event,
 *                          expects response and list of Job objects as arguments
 *                  error: callback function for an fail() event, expects exception as argument
 *                  userId: select by user (default is all users)
 *                  groupId: the ID of the group to select for
 *                  batchId: the ID of the batch
 *                  pageSize: the maximum number of jobs to select (default is 10)
 *                  page: page of jobs to select (default is 1)
 *                  includeChildren: include child jobs? (default is true)
 *                  includeOutputFiles: include the output files? (default is true)
 *                  includePermissions: include job permissions? (default is true)
 *
 * @returns {jQuery.Deferred} - Returns a jQuery Deferred object for event chaining.
 *      See http://api.jquery.com/jquery.deferred/ for details.
 */
gp.jobs = function(pObj) {
    var forceRefresh = pObj && pObj.force && pObj.force.toLowerCase() === 'true';
    var useCache = gp._jobs && !forceRefresh;

    if (useCache) {
        return new $.Deferred()
            .done(function() {
                if (pObj && pObj.success) {
                    pObj.success("cached", gp._jobs);
                }
            })
            .resolve();
    }
    else {
        var REST_ENDPOINT = "/rest/v1/jobs/?";

        var userId = pObj && pObj['userId'] ? pObj['userId'] : null;
        var groupId = pObj && pObj['groupId'] ? pObj['groupId'] : null;
        var batchId = pObj && pObj['batchId'] ? pObj['batchId'] : null;
        var pageSize = pObj && pObj['pageSize'] ? pObj['pageSize'] : null;
        var page = pObj && pObj['page'] ? pObj['page'] : null;
        var includeChildren = pObj && pObj['includeChildren'] ? pObj['includeChildren'] : null;
        var includeOutputFiles = pObj && pObj['includeOutputFiles'] ? pObj['includeOutputFiles'] : null;
        var includePermissions = pObj && pObj['includePermissions'] ? pObj['includePermissions'] : null;

        if (userId) REST_ENDPOINT += "&userId=" + encodeURIComponent(userId);
        if (groupId) REST_ENDPOINT += "&groupId=" + encodeURIComponent(groupId);
        if (batchId) REST_ENDPOINT += "&batchId=" + encodeURIComponent(batchId);
        if (pageSize) REST_ENDPOINT += "&pageSize=" + encodeURIComponent(pageSize);
        if (page) REST_ENDPOINT += "&page=" + encodeURIComponent(page);
        if (includeChildren) REST_ENDPOINT += "&includeChildren=" + encodeURIComponent(includeChildren);
        if (includeOutputFiles) REST_ENDPOINT += "&includeOutputFiles=" + encodeURIComponent(includeOutputFiles);
        if (includePermissions) REST_ENDPOINT += "&includePermissions=" + encodeURIComponent(includePermissions);

        return $.ajax({
            url: gp.server() + REST_ENDPOINT,
            type: 'GET',
            dataType: 'json',
            xhrFields: {
                withCredentials: true
            }
        })
            .done(function(response) {
                // Create the new _jobs list and iterate over returned JSON list, creating Job objects
                gp._jobs = [];
                var jobs = response['items'];
                if (jobs) {
                    for (var i = 0; i < jobs.length; i++) {
                        var json = jobs[i];
                        gp._jobs.push(new gp.Job(json));
                    }
                }

                if (pObj && pObj.success) {
                    pObj.success(response, gp._tasks);
                }
            })
            .fail(function(exception) {
                if (pObj && pObj.error) {
                    pObj.error(exception);
                }
            });
    }
};


/**
 * Returns a Job object either from the cache or from a server query
 *
 * @param pObj - An object specifying this property:
 *                  jobNumber: the job number of the job
 *                  force: do not use cache, force a new query
 *                  permissions: whether to include permissions info (default: false)
 *                  success: callback function for a done() event,
 *                          expects response and a Job object as arguments
 *                  error: callback function for an fail() event, expects exception as argument
 *
 * @returns {jQuery.Deferred} - Returns a jQuery Deferred object for event chaining.
 *      See http://api.jquery.com/jquery.deferred/ for details.
 */
gp.job = function(pObj) {
    var forceRefresh = pObj && ((typeof pObj.force === 'boolean' && pObj.force) ||
        (typeof pObj.force === 'string' && pObj.force.toLowerCase() === 'true'));
    var getPermissions = pObj && ((typeof pObj.permissions === 'boolean' && pObj.permissions) ||
        (typeof pObj.permissions === 'string' && pObj.permissions.toLowerCase() === 'true'));
    var jobNumber = pObj.jobNumber;

    // Try to find the job in the cache
    if (!forceRefresh && gp._jobs) {
        for (var i = 0; i < gp._jobs.length; i++) {
            var job = gp._jobs[i];
            if (job.jobNumber() === jobNumber) {
                return new $.Deferred()
                    .done(function() {
                        if (pObj && pObj.success) {
                            pObj.success("Job cached", job);
                        }
                    })
                    .resolve();
            }
        }
    }

    // Otherwise, if not cached or refreshed forced
    var permissionsParam = getPermissions ? "?includePermissions=true" : "";
    var REST_ENDPOINT = "/rest/v1/jobs/";
    return $.ajax({
        url: gp.server() + REST_ENDPOINT + jobNumber + permissionsParam,
        type: 'GET',
        dataType: 'json',
        xhrFields: {
            withCredentials: true
        }
    })
        .done(function(response) {
            // Create the new _jobs list and iterate over returned JSON list, creating Job objects
            var loadedJob = new gp.Job(response);

            if (pObj && pObj.success) {
                pObj.success(response, loadedJob);
            }
        })
        .fail(function(exception) {
            if (pObj && pObj.error) {
                pObj.error(exception);
            }
        });
};


/**
 * Uploads a file for running a job
 *
 * @param pObj - An object specifying this property:
 *                  file: This is a File object for the file to upload
 *                          (See the HTML5 File API)
 *                  success: This a callback for after the upload.
 *                          Expects a response and URL to the file resource
 *                  error: Callback for an error. Expects an exception
 */
gp.upload = function(pObj) {
    // Ensure the file is specified
    if (!pObj) throw "gp.upload() parameter either null or undefined";
    if (typeof pObj === 'object' && typeof pObj.file !== 'object') throw "gp.upload() parameter does not contain a File object";

    var REST_ENDPOINT = "/rest/v1/data/upload/job_input";
    var nameParam = "?name=" + pObj.file.name;

    return $.ajax({
        url: gp.server() + REST_ENDPOINT + nameParam,
        type: 'POST',
        dataType: "text",
        processData: false,
        data: pObj.file,
        xhrFields: {
            withCredentials: true
        },
        headers: {
            "Content-Length": pObj.file.size
        },
        success: function(data, textStatus){
            if (pObj && pObj.success) {
                pObj.success(textStatus, data);
            }
        }
    })
        .fail(function(exception) {
            if (pObj && pObj.error) {
                pObj.error(exception);
            }
        });
};


/**
 * Declaration of Task class
 * @constructor
 */
gp.Task = function(taskJson) {
    // Define class members
    this._tags = null;
    this._description = null;
    this._name = null;
    this._documentation = null;
    this._categories = null;
    this._suites = null;
    this._version = null;
    this._lsid = null;
    this._params = null;

    /**
     * Constructor-like initialization for the Task class
     *
     * @private
     */
    this._init_ = function() {
        if (taskJson) {
            this._tags = taskJson.tags;
            this._description = taskJson.description;
            this._name = taskJson.name;
            this._documentation = taskJson.documentation;
            this._categories = taskJson.categories;
            this._suites = taskJson.suites;
            this._version = taskJson.version;
            this._lsid = taskJson.lsid;
        }
    };
    this._init_();

    /**
     * Returns a JobInput object for submitting a job for this task
     * @returns {gp.JobInput}
     */
    this.jobInput = function() {
        return new gp.JobInput(this);
    };

    /**
     * Loads a Task's parameters from REST call, or retrieves them from the cache
     *
     * @param pObj - The following parameters may be set
     *                  force: Do not use cache, if available. Always make AJAX call.
     *                  success: callback function for a done() event,
     *                          expects response and a list of Param objects as arguments
     *                  error: callback function for an fail() event, expects exception as argument
     *
     * @returns {jQuery.Deferred} - Returns a jQuery Deferred object for event chaining.
     *      See http://api.jquery.com/jquery.deferred/ for details.
     */
    this.params = function(pObj) {
        var task = this;
        var forceRefresh = pObj && ((typeof pObj.force === 'boolean' && pObj.force) ||
            (typeof pObj.force === 'string' && pObj.force.toLowerCase() === 'true'));
        var inCache = forceRefresh ? false : task._params !== null;

        if (inCache) {
            return new $.Deferred()
                .done(function() {
                    if (pObj && pObj.success) {
                        pObj.success("cached", task._params);
                    }
                })
                .resolve();
        }
        else {
            var REST_ENDPOINT = "/rest/v1/tasks/";

            return $.ajax({
                url: gp.server() + REST_ENDPOINT + encodeURIComponent(task.lsid()),
                type: 'GET',
                dataType: 'json',
                xhrFields: {
                    withCredentials: true
                }
            })
                .done(function(response) {
                    // Add params to Task object
                    var params = response['params'];
                    if (params) {
                        task._params = [];
                        for (var i = 0; i < params.length; i++) {
                            var param = params[i];
                            task._params.push(new gp.Param(param));
                        }
                    }

                    if (pObj && pObj.success) {
                        pObj.success(response, task._params);
                    }
                })
                .fail(function(exception) {
                    if (pObj && pObj.error) {
                        pObj.error(exception);
                    }
                });
        }
    };

    /**
     * Getter for Task tags
     *
     * @returns {null|Array}
     */
    this.tags = function() {
        return this._tags;
    };

    /**
     * Getter for Task description
     *
     * @returns {null|string}
     */
    this.description = function() {
        return this._description;
    };

    /**
     * Getter for Task name
     *
     * @returns {null|string}
     */
    this.name = function() {
        return this._name;
    };

    /**
     * Getter for URL to Task documentation
     *
     * @returns {null|string}
     */
    this.documentation = function() {
        return this._documentation;
    };

    /**
     * Getter for list of Task categories
     *
     * @returns {null|Array}
     */
    this.categories = function() {
        return this._categories;
    };

    /**
     * Getter for list of Task suites
     *
     * @returns {null|Array}
     */
    this.suites = function() {
        return this._suites;
    };

    /**
     * Getter for Task version
     *
     * @returns {null|number}
     */
    this.version = function() {
        return this._version;
    };

    /**
     * Getter for Task LSID
     *
     * @returns {null|string}
     */
    this.lsid = function() {
        return this._lsid;
    };
};


/**
 * Declaration of Job class
 * @constructor
 */
gp.Job = function(jobJson) {
    this._task = null;
    this._taskName = null;
    this._taskLsid = null;
    this._userId = null;
    this._permissions = null;
    this._jobNumber = null;
    this._status = null;
    this._dateSubmitted = null;
    this._logFiles = null;
    this._outputFiles = null;
    this._numOutputFiles = null;

    /**
     * Constructor-like initialization for the Job class
     *
     * @private
     */
    this._init_ = function() {
        if (jobJson) {
            this._taskName = jobJson.taskName;
            this._taskLsid = jobJson.taskLsid;
            this._userId = jobJson.userId;
            this._permissions = jobJson.permissions;
            this._jobNumber = typeof jobJson['jobId'] === 'string' ? parseInt(jobJson['jobId']) : jobJson['jobId'];
            this._status = jobJson.status;
            this._dateSubmitted = jobJson.dateSubmitted;
            this._logFiles = jobJson.logFiles;
            this._outputFiles = jobJson.outputFiles;
            this._numOutputFiles = typeof jobJson.numOutputFiles === 'string' ? parseInt(jobJson.numOutputFiles) : jobJson.numOutputFiles;
            this._task = gp.task(this._taskLsid);
        }
    };
    this._init_();

    /**
     * Queries the server for the job's status, updates the Job object and returns
     *
     * @param pObj - The following parameters may be set
     *                  success: callback function for a done() event,
     *                          expects response and a status object as arguments
     *                  error: callback function for an fail() event, expects exception as argument
     *
     * @returns {jQuery.Deferred} - Returns a jQuery Deferred object for event chaining.
     *      See http://api.jquery.com/jquery.deferred/ for details.
     */
    this.update = function(pObj) {
        var REST_ENDPOINT = "/rest/v1/jobs/" + this.jobNumber() + "/status.json";
        var job = this;

        return $.ajax({
            url: gp.server() + REST_ENDPOINT,
            type: 'GET',
            dataType: 'json',
            xhrFields: {
                withCredentials: true
            }
        })
            .done(function(response) {
                // Add params to Job object
                var status = response;
                if (status) {
                    job._status = status;
                }

                if (pObj && pObj.success) {
                    pObj.success(response, status);
                }
            })
            .fail(function(exception) {
                if (pObj && pObj.error) {
                    pObj.error(exception);
                }
            });
    };

    /**
     * Returns API code for getting the status of this job
     *
     * @param pObj - The following parameters may be set
     *                  language: The language to get the code in, default to Python.
     *								Options: R, Java, MATLAB, Python
     *                  error: callback function for an fail() event, expects exception as argument
     *
     * @returns {jQuery.Deferred} - Returns a jQuery Deferred object for event chaining.
     *      See http://api.jquery.com/jquery.deferred/ for details.
     */
    this.code = function(pObj) {
        // Validate language
        var language = null;

        if (typeof pObj === "string") { language = pObj; }
        else { language = pObj.language; }

        if (language !== "Python" && language !== "R" && language !== "Java" && language !== "MATLAB") {
            console.log("Unknown language, defaulting to Python: " + language);
        }

        var REST_ENDPOINT = "/rest/v1/jobs/" + this.jobNumber() + "/code?language=" + language;

        return $.ajax({
                url: gp.server() + REST_ENDPOINT,
                type: 'GET',
                dataType: 'text',
                xhrFields: {
                    withCredentials: true
                }
            })
            .fail(function(exception) {
                if (pObj && pObj.error) {
                    pObj.error(exception);
                }
            });
    };

    /**
     * Returns the Task object associated with the job
     *
     * @returns {null|gp.Task}
     */
    this.task = function() {
        return this._task;
    };

    /**
     * Returns the name of the job's associated task
     * @returns {string}
     */
    this.taskName = function() {
        return this._taskName;
    };

    /**
     * Returns the LSID of the job's associated task
     *
     * @returns {string}
     */
    this.taskLsid = function() {
        return this._taskLsid;
    };

    /**
     * Returns the user ID of the job's owner
     *
     * @returns {string}
     */
    this.userId = function() {
        return this._userId;
    };

    this.permissions = function() {
        return this._permissions;
    };

    /**
     * Returns the job number
     *
     * @returns {number}
     */
    this.jobNumber = function() {
        return this._jobNumber;
    };

    /**
     * Returns a job permissions object
     *
     * @returns {null|object}
     */
    this.permissions = function() {
        return this._permissions;
    };

    /**
     * Returns a job status object
     *
     * @returns {null|object}
     */
    this.status = function() {
        return this._status;
    };

    /**
     * Returns the date the job was submitted
     * @returns {null|string|Date}
     */
    this.dateSubmitted = function() {
        return this._dateSubmitted;
    };

    /**
     * Returns an array of log files associated with the job
     * @returns {Array}
     */
    this.logFiles = function() {
        return this._logFiles;
    };

    /**
     * Returns an array of the output files possessed by the job
     *
     * @returns {Array}
     */
    this.outputFiles = function() {
        return this._outputFiles;
    };

    /**
     * Returns the number of output files the job has currently output
     *
     * @returns {null|number}
     */
    this.numOutputFiles = function() {
        return this._numOutputFiles;
    };
};


/**
 * Declaration of Job Input class
 * @constructor
 */
gp.JobInput = function(task) {
    // Define class members
    this._lsid = null;
    this._params = null;

    // Ensure that Task object has its params initialized
    if (task._params === null) throw "Cannot create JonInput from Task with null params. First call Task.params()";

    /**
     * Constructor-like initialization for the JobInput class
     *
     * @private
     */
    this._init_ = function() {
        if (task) {
            this._lsid = task.lsid();
            this._params = [];
            for (var i = 0; i < task._params.length; i++) {
                var param = task._params[i];
                this._params.push(param.clone());
            }
        }
    };
    this._init_();

    /**
     * Getter for Task LSID
     *
     * @returns {string}
     */
    this.lsid = function() {
        return this._lsid;
    };

    /**
     * Getter for the params list
     *
     * @returns {Array}
     */
    this.params = function() {
        return this._params;
    };

    /**
     * Returns a Parameter after looking it up by name
     *      Returns null if the param was not found.
     *
     * @param name - The name of the parameter
     * @returns {gp.Param|null} - The matching Param object
     */
    this.param = function(name) {
        for (var i = 0; i < this._params.length; i++) {
            var param = this._params[i];
            if (param.name() == name) return param;
        }
        return null;
    };

    /**
     * Returns a JSON structure for this Job Input designed to be consumed by a submit() call
     * @returns {object}
     *
     * @private
     */
    this._submitJson_ = function() {
        var lsid = this.lsid();
        var params = [];
        for (var i = 0; i < this.params().length; i++) {
            var param = this.params()[i];
            params.push({
                name: param.name(),
                values: param.values() === null ? (param.defaultValue() ? [param.defaultValue()] : []) : param.values(),
                batchParam: param.batchParam() === null ? false : param.batchParam(),
                groupId: param.groupId() === null ? "" : param.groupId()
            });
        }
        return {
            lsid: lsid,
            params: params
        };
    };

    /**
     * Submits the task and parameter values to the server as a Job
     *
     * @param pObj - The following parameters may be set
     *                  success: callback function for a done() event,
     *                          expects response and a Job Number as arguments
     *                  error: callback function for an fail() event, expects exception as argument
     *
     * @returns {jQuery.Deferred} - Returns a jQuery Deferred object for event chaining.
     *      See http://api.jquery.com/jquery.deferred/ for details.
     */
    this.submit = function(pObj) {
        var REST_ENDPOINT = "/rest/v1/jobs/";

        return $.ajax({
            url: gp.server() + REST_ENDPOINT,
            type: 'POST',
            data: JSON.stringify(this._submitJson_()),
            dataType: 'json',
            contentType: "application/json",
            xhrFields: {
                withCredentials: true
            }
        })
            .done(function(response) {
                // Create Job object from JSON response
                var jobNumber = response['jobId'];

                if (pObj && pObj.success) {
                    pObj.success(response, jobNumber);
                }
            })
            .fail(function(exception) {
                if (pObj && pObj.error) {
                    pObj.error(exception);
                }
            });
    };
};

/**
 * Declaration of Param class
 * @constructor
 */
gp.Param = function(paramJson) {
    // Define class members
    this._name = null;
    this._description = null;
    this._choices = null;
    this._values = null;
    this._batchParam = null;
    this._groupId = null;
    this._defaultValue = null;
    this._optional = null;
    this._prefixWhenSpecified = null;
    this._type = null;

    /**
     * Constructor-like initialization for the Param class
     *
     * @private
     */
    this._init_ = function() {
        if (paramJson) {
            if (paramJson) {
                this._name = Object.keys(paramJson)[0];
                this._description = paramJson[this._name]['description'];
                this._choices = paramJson[this._name]['choiceInfo'] ? this._parseChoices(paramJson[this._name]['choiceInfo']) : null;
                this._values = null;
                this._batchParam = false;
                this._groupId = null;
                this._defaultValue = paramJson[this._name]['attributes']['default_value'];
                this._optional = paramJson[this._name]['attributes']['optional'] === 'on';
                this._prefixWhenSpecified = paramJson[this._name]['attributes']['prefix_when_specified'];
                this._type = paramJson[this._name]['attributes']['type'];
            }
        }
    };

    /**
     * Parses the choice info JSON returned by the server into the expected format
     *
     * @param choiceInfo - The choice info JSON
     * @returns {*}
     * @private
     */
    this._parseChoices = function(choiceInfo) {
        if (choiceInfo['choices']) {
            var choices = {};
            for (var i = 0; i < choiceInfo['choices'].length; i++) {
                var choice = choiceInfo['choices'][i];
                choices[choice['label']] = choice['value'];
            }
            return choices;
        }
        else {
            console.log("No choices in choice info. Dynamic choices not yet supported.");
            return null;
        }
    };

    /**
     * Return a clone of this param
     *
     * @returns {gp.Param}
     */
    this.clone = function() {
        var param = new gp.Param();
        param.name(this.name());
        param.values(this.values());
        param.defaultValue(this.defaultValue());
        param.optional(this.optional());
        param.prefixWhenSpecified(this.prefixWhenSpecified());
        param.type(this.type());

        return param;
    };

    /**
     * Returns or sets the value of the parameter
     *
     * @param [value=optional] - The set value of the parameter
     * @returns {null|Array}
     */
    this.values = function(value) {
        if (value !== undefined) {
            this._values = value;
        }
        else {
            return this._values;
        }
    };

    /**
     * Returns or sets whether this parameter is a batch (default is false)
     *
     * @param [batchParam=optional] - Is this parameter a batch?
     * @returns {null|boolean}
     */
    this.batchParam = function(batchParam) {
        if (batchParam !== undefined) {
            this._batchParam = batchParam;
        }
        else {
            return this._batchParam;
        }
    };

    /**
     * Returns or sets the group ID
     *
     * @param [groupId=optional] - the group ID of the parameter
     * @returns {null|string}
     */
    this.groupId = function(groupId) {
        if (groupId !== undefined) {
            this._groupId = groupId;
        }
        else {
            return this._groupId;
        }
    };

    /**
     * Returns or sets the name of the parameter
     *
     * @param [name=optional] - The name of the parameter
     * @returns {string}
     */
    this.name = function(name) {
        if (name !== undefined) {
            this._name = name;
        }
        else {
            return this._name;
        }
    };

    /**
     * Returns or sets the description of the parameter
     *
     * @param [description=optional] - The description of the parameter
     * @returns {string}
     */
    this.description = function(description) {
        if (description !== undefined) {
            this._description = description;
        }
        else {
            return this._description;
        }
    };

    /**
     * Returns or sets the choices for the parameter
     *
     * @param [choices=optional] - The choices for the parameter.
     *              Assumes a object of key : value pairings.
     * @returns {string}
     */
    this.choices = function(choices) {
        if (choices !== undefined) {
            this._choices = choices;
        }
        else {
            return this._choices;
        }
    };

    /**
     * Returns or sets the default value of the parameter
     *
     * @param [defaultValue=optional] - The default value for the parameter
     * @returns {string}
     */
    this.defaultValue = function(defaultValue) {
        if (defaultValue !== undefined) {
            this._defaultValue = defaultValue;
        }
        else {
            return this._defaultValue;
        }
    };

    /**
     * Returns or sets whether the parameter is optional or not
     *
     * @param [optional=optional] - Is this parameter optional?
     * @returns {boolean}
     */
    this.optional = function(optional) {
        if (optional !== undefined) {
            this._optional = optional;
        }
        else {
            return this._optional;
        }
    };

    /**
     * Returns or sets the prefix when specified value
     *
     * @param [prefixWhenSpecified=optional] - What is the prefix?
     * @returns {string}
     */
    this.prefixWhenSpecified = function(prefixWhenSpecified) {
        if (prefixWhenSpecified !== undefined) {
            this._prefixWhenSpecified = prefixWhenSpecified;
        }
        else {
            return this._prefixWhenSpecified;
        }
    };

    /**
     * Returns or sets the type of the parameter
     *
     * @param [type=optional] - The type of this parameter
     * @returns {string}
     */
    this.type = function(type) {
        if (type !== undefined) {
            this._type = type;
        }
        else {
            return this._type;
        }
    };

    // Init the object
    this._init_();
};
