/**
 * Get a cookie by name
 *
 * @param name
 * @returns {string}
 */
export function cookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
}

/**
 * Get the authentication hash, whether from the URL hash or from the GenePatternAccess cookie.
 * The cookie is accessible only from the genepattern.org domain, the hash must be used elsewhere (e.g. notebook)
 *
 * @returns {string}
 */
export function auth_token() {
    return window.location.hash && window.location.hash.length > 0 ? window.location.hash.slice(1) : cookie('GenePatternAccess');
}

/**
 * Returns the __original__ parameters passed to the visualizer, null if unavailable
 *
 * @returns {null|*}
 */
export function original_parameters() {
    const params = new URLSearchParams(new URL(window.location).search);
    if (params.has('__original__')) {
        try { return JSON.parse(params.get('__original__')) }
        catch (e) { console.log('Error parsing __original__'); return null; }
    }
    else return null;
}

/**
 * Get the value of the original parameter, obtained from __original__, null if unavailable
 *
 * @param name
 * @returns {null|*}
 */
export function original_param(name) {
    const og = original_parameters()
    if (og) {
        try {
            if (name in og)
                if (Array.isArray(og[name] && og[name].length === 1)) return og[name][0];
                else return og[name];
        } catch (e) {}
    }
    return null;
}

/**
 * Get the value of the specified task parameter, passed in as input to this visualizer
 *
 * @returns {string}
 */
export function param(name, use_original=false) {
    if (use_original) {
        const original = original_param(name);
        if (original !== null && original !== undefined) return original;
    }

    const params = new URLSearchParams(new URL(window.location).search);
    return params.get(name);
}

/**
 * Gets the URL of the local GenePattern server
 *
 * @returns {string}
 */
export function server_url() {
    return `${window.location.origin}/gp`;
}

/**
 * Determine whether the provided identifier is a module name or lsid
 *
 * @param name_or_lsid
 * @returns {boolean}
 */
export function is_lsid(name_or_lsid) {
    return name_or_lsid.startsWith('urn:lsid:')
}

/**
 * Get the full JSON description of the given module
 *
 * @param name_or_lsid
 * @returns {Promise<any>}
 */
export async function task(name_or_lsid) {
    // Ensure we have the full lsid (required by /tasks endpoint)
    if (!is_lsid(name_or_lsid)) name_or_lsid = await lsid_from_name(name_or_lsid);

    return fetch(`/gp/rest/v1/tasks/${name_or_lsid}/`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'User-Agent': 'GenePatternRest',
            'Authorization': `Bearer ${auth_token()}`
        }
    }).then(response => response.json())
}

/**
 * Get the full lsid from the module name
 *
 * @param name
 * @returns {Promise<*>}
 */
export async function lsid_from_name(name) {
    const task_list = await fetch(`/gp/rest/v1/tasks/all.json`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'User-Agent': 'GenePatternRest',
            'Authorization': `Bearer ${auth_token()}`
        }
    }).then(response => response.json());
    for (const m of task_list.all_modules)
        if (name === m.name)
            return m.lsid;
}

/**
 * Run a job on the GenePattern server
 *
 * @param task - Task name or LSID
 * @param params - List of param objects in the format: [{'name': 'foo', values: ['bar']}]
 * @returns {Promise<any>}
 */
export async function run(name_or_lsid, params) {
    // Ensure we have the full lsid (required by /jobs endpoint)
    if (!is_lsid(name_or_lsid)) name_or_lsid = await lsid_from_name(name_or_lsid);

    return fetch('/gp/rest/v1/jobs', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'User-Agent': 'GenePatternRest',
            'Authorization': `Bearer ${auth_token()}`
        },
        body: JSON.stringify({
            lsid: name_or_lsid,
            params: params,
            tags: ['VisualizerApp']
        })
    }).then(response => response.json())
}

/**
 * Wait for N seconds then continue execution.
 * Must be used in async function and awaited
 *
 * @param seconds
 * @returns {Promise<unknown>}
 */
export async function wait(seconds) {
    // Simulate some asynchronous operation
    const result = await new Promise(resolve => {
        setTimeout(() => {
            resolve("Result after seconds");
        }, seconds * 1000);
    });

    return result;
}

/**
 * Get an update on the job from the server, return the new job object
 *
 * @param job - Integer or job json
 * @returns {Promise<any>}
 */
export async function update_job(job) {
    const job_number = !!parseInt(job) ? job : job.jobId;
    return fetch(`/gp/rest/v1/jobs/${job_number}`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'User-Agent': 'GenePatternRest',
            'Authorization': `Bearer ${auth_token()}`
        }
    }).then(response => response.json())
}

/**
 * Poll the server every few seconds until the job is complete
 *
 * @param job - job json
 * @param update_callback - callback function to do each update, passing updated json as param
 * @param seconds - poll interval (default: 10 seconds)
 * @returns {Promise<*>}
 */
export async function poll_job(job, update_callback=null, seconds=10) {
    while (!job?.status?.isFinished) {
        await wait(seconds);
        job = await update_job(job)
        if (update_callback) await update_callback(job);
    }
    return job;
}