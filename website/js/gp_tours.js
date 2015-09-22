var last_left_nav_tab = null;
var last_left_nav_tab_new = null;

$(document).ready(function() {
    last_left_nav_tab = $("#left-nav").tabs( "option", "active");
});

$(function()
{
    $(".gp_tours").click(function(event)
    {
        event.preventDefault();
        var intro = introJs();

        //create the steps
        var steps = [
            {
                element: "#topband",//"img[src*='GP-logo.gif']",
                intro: '<div class="tour-header"> GenePattern Icon </div>You can click the GenePattern icon to return to this home page at any time.'
            },
            {
                element: '#navband1',
                intro: '<div class="tour-header"> Navigation Bar </div>Access other pages from the navigation bar.'
            },
            {
                element: '#left-nav-modules-tab',
                intro: '<div class="tour-header"> Modules Tab </div> In the Modules tab, search for a module or pipeline by starting to type its name. Drag and drop modules to favorites and view recently run modules.',
                position: 'right'
            },
            {
                element: '#left-nav-jobs-tab',
                intro: '<div class="tour-header"> Jobs Tab </div>  The Jobs tab lists processing and finished jobs with results files.',
                position: 'right'
            },
            {
                element: '#left-nav-files-tab',
                intro: '<div class="tour-header"> Files Tab</div> The Files tab lists uploaded files on the GenePattern server. You can also copy job result files to the Files Tab.',
                position: 'right'
            },
            {
                element: '#left-nav-genomespace-tour-created',
                intro: '<div class="tour-header"> GenomeSpace Tab</div> If you sign-in using your GenomeSpace account, access via tab that appears here.',
                position: 'right'
        }];

        if($("#protocols").is(":visible"))
        {
            steps.push({
                element: '#protocols',
                intro: '<div class="tour-header"> Main Display Pane</div> The main display pane displays interactive information including protocols for common GenePattern analyses.',
                position: 'left'
            });
        }

        if($("#submitJob").is(":visible"))
        {
            steps.push({
                element: '#submitJob',
                intro: '<div class="tour-header"> Main Display Pane</div>The main display pane displays interactive information including protocols for common GenePattern analyses.',
                position: 'left'
            });
        }

        intro.setOptions({
            steps: steps,
            showStepNumbers: false,
            skipLabel: "End Tour",
            tooltipClass: "tourStyle"
        });

        intro.onbeforechange(function(targetElement)
        {
            //hack to not show the hidden native file upload button
            $("#submitJob").find(".uploadedinputfile").hide();

            //switch the active left navigation tab to the appropriate one for the step
            if(intro._currentStep == 2)
            {
                $(this).data("last-left-nav-tab", $("#left-nav").tabs( "option", "active"));

                $( "#left-nav" ).tabs( "option", "active", 0 );
            }
            else if(intro._currentStep == 3)
            {
                $( "#left-nav" ).tabs( "option", "active", 1 );
            }
            else if(intro._currentStep == 4)
            {
                $( "#left-nav" ).tabs( "option", "active", 2 );

                //add the genome space tab
                if($("#left-nav-genomespace-tour-created").length == 0)
                {
                    $("#left-nav-files-tab").after('<li id="left-nav-genomespace-tour-created">' +
                        '<a href="#left-nav-genomespace">GenomeSpace</a> </li>');

                    $("#left-nav-genomespace-tour-created").parent().after(
                        '<div id="left-nav-genomespace" class="left-nav-tab">' +
                        '<div class="left-nav-top">' +
                        '<button id="left-nav-genomespace-refresh"><span class="glyphicon glyphicon-refresh"></span> Refresh</button>' +
                        '<a href="http://gsui.genomespace.org" target="_blank"><img id="left-nav-genomespace-logo" src="/gp/pages/genomespace/genomespacelogo.png" /></a>' +
                        '</div>' +
                        '</div>');
                    $("#left-nav").tabs("refresh");
                }
            }
            else if(intro._currentStep == 5)
            {
                $( "#left-nav" ).tabs( "option", "active", 3 );
            }
            else if(targetElement.id == "submitJob")
            {
                //hack to not show the hidden native file upload button
               // $("#submitJob").find(".uploadedinputfile").hide();
            }
        });

        intro.onexit(function()
        {
            introTourCleanup();
        });

        intro.oncomplete(function()
        {
            introTourCleanup();
        });

        intro.start();
    });


    $(".gp_new").click(function(event)
    {
        event.preventDefault();
        var intro = introJs();

        //create the steps
        var steps = [
            {
                intro: '<div class="tour-header"> Batch Jobs </div> The batching feature has now been expanded to include all parameters instead of just files. ',
                position: 'top'
            },
            {
                element: "#numClustersBatchBox",
                intro: '<div class="tour-header"> Batch Jobs </div> You can enable batching just by clicking on the Batch checkbox. ',
                position: 'left'
            },
            {
                element: ".previewBatch",
                intro: '<div class="tour-header"> Preview Batch Jobs </div>  There is a <b>Preview Batch</b> button which allows you to preview the values will be set for each batch job.',
                position: 'left'
            },
            {
                element: "#batchMain",
                intro: '<div class="tour-header"> Preview Batch Jobs </div> The Preview Batch dialog lists the total number of batch jobs that will be launched and for each job the value that will be set for the batched parameters.',
                position: 'top'
            },
            {
                intro: '<div class="tour-header"> The End</div> This is the end of the tour. To learn more about what'
                    + ' is new, please see the <a href="http://www.broadinstitute.org/cancer/software/genepattern/doc/relnotes/3.9.5" target="_blank">release notes</a>.'
            }
        ];

        intro.setOptions({
            steps: steps,
            showStepNumbers: false,
            skipLabel: "End Tour",
            tooltipClass: "tourStyle",
            scrollToElement: false,
            prevLabel: "",
            showBullets: false
        });

        intro.onbeforechange(function(targetElement)
        {
            $("#batchMain").remove();

            if (intro._currentStep === 0) {

                $("#main-pane").html(getHierarchicalClusteringMainPaneHtml());
            }
            else if (intro._currentStep === 3)
            {
                var batchPreviewDiv = $('<div id="batchMain" class="ui-dialog ui-widget ui-widget-content ui-corner-all ui-draggable ui-resizable ui-dialog-buttons" tabindex="-1" role="dialog" aria-labelledby="ui-id-216" style="outline: 0; z-index: 1008; position: absolute; max-height: 350px; width: 500px; top: 80px; left: 100px; display: block;"><div class="ui-dialog-titlebar ui-widget-header ui-corner-all ui-helper-clearfix"><span id="ui-id-216" class="ui-dialog-title">&nbsp;</span><a href="#" class="ui-dialog-titlebar-close ui-corner-all" role="button"><span class="ui-icon ui-icon-closethick">close</span></a></div>' +
                    '<div id="batchInfoDialog" class="ui-dialog-content ui-widget-content" scrolltop="0" scrollleft="0" style="display: block; width: auto; max-height: 250px; height:  250px;"><div id="batchInfoDialogHeader"><h4> Total Batch Jobs: 2</h4></div><div><table><tbody><tr><td colspan="2">Batch #1</td></tr><tr><td width="40%">number of clusters</td><td width="60%">3</td></tr><tr><td width="40%">cluster by</td><td width="60%">rows</td></tr></tbody></table></div><div><table><tbody><tr><td colspan="2">Batch #2</td></tr><tr><td width="40%">number of clusters</td><td width="60%">5</td></tr><tr><td width="40%">cluster by</td><td width="60%">columns</td></tr></tbody></table></div></div><div class="ui-resizable-handle ui-resizable-n" style="z-index: 1000;"></div>' +
                    '<div class="ui-resizable-handle ui-resizable-e" style="z-index: 1000;"></div><div class="ui-resizable-handle ui-resizable-s" style="z-index: 1000;"></div><div class="ui-resizable-handle ui-resizable-w" style="z-index: 1000;"></div><div class="ui-resizable-handle ui-resizable-se ui-icon ui-icon-gripsmall-diagonal-se ui-icon-grip-diagonal-se" style="z-index: 1000;"></div><div class="ui-resizable-handle ui-resizable-sw" style="z-index: 1000;"></div><div class="ui-resizable-handle ui-resizable-ne" style="z-index: 1000;"></div><div class="ui-resizable-handle ui-resizable-nw" style="z-index: 1000;"></div><div class="ui-dialog-buttonpane ui-widget-content ui-helper-clearfix"><div class="ui-dialog-buttonset">' +
                    '<button type="button" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" role="button" aria-disabled="false"><span class="ui-button-text">OK</span></button></div></div></div>');
                $("#main-pane").append(batchPreviewDiv);
            }
        });

        intro.onexit(function()
        {
            introTourCleanup();
            window.location.href = "/gp";
        });

        intro.oncomplete(function()
        {
            window.location.href = "/gp";
        });

        //last_left_nav_tab_new =  $("#left-nav").tabs( "option", "active");

        intro.start();
    });
});


function jqEscape(str) {
    return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}

function introTourCleanup()
{
    //set tab back to original selection
    $( "#left-nav" ).tabs( "option", "active", $(this).data("last-left-nav-tab"));

    $("#left-nav-genomespace-tour-created").remove();
    $("#left-nav").tabs("refresh");
}

function getHierarchicalClusteringMainPaneHtml()
{
    return '<style>\
    .urlInput {\
        width: 320px;\
        margin-left: 20px;\
        }\
    </style>\
    <link href="/gp/js/themes/default/style.css" rel="stylesheet" type="text/css">\
        <div id="submitJob" style="">\
            <div id="submitForm">\
                <div id="runTaskSettingsDiv">\
                <div class="task_header" id="taskHeaderDiv">\
                <span id="task_name" class="module_header ">KMeansClustering</span>\
                <span id="task_version_main">\
                <label for="task_versions">\
                version\
                </label>\
                <span class="normal">2</span>\
            </span>\
            <div id="otherOptionsDiv" class="floatRight">\
                <img id="otherOptions" alt="other options" src="/gp/images/gear_blue_and_white.png" height="20">\
            </div>\
            <a class="floatRight" id="documentation" href="/gp/getTaskDoc.jsp?name=urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00081:2" target="_blank">Documentation</a>\
        </div>\
        <table id="description_main">\
            <tbody>\
                <tr>\
                    <td id="mod_description">\
                    Module that performs the K-Means Clustering algorithm</td>\
                    <td id="source_info">\
                        <div id="source_info_tooltip" style="display: none;"></div>\
                    </td>\
                </tr>\
            </tbody>\
        </table>\
        <hr class="clear">\
            <div>\
                <span class="otherControlsDiv">\
                * required field\
                </span>\
                <span class="submitControlsDiv floatRight">\
                    <button class="Reset ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" role="button" aria-disabled="false"><span class="ui-button-text"><img src="/gp/images/reset.gif" width="16" height="16" alt="Reset" class="buttonIcon"> Reset </span></button>\
                    <button class="previewBatch ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" role="button" aria-disabled="false"><span class="ui-button-text"><img src="/gp/images/Information_magnifier_icon.png" class="buttonIcon" height="16" width="16">Preview Batch</span></button>\
                    <button class="Run ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" role="button" aria-disabled="false"><span class="ui-button-text"><img src="/gp/images/run-green.gif" width="16" height="16" alt="Run" class="buttonIcon"> Run </span></button>\
                </span>\
                    </div>\
                    <hr class="clear">\
                        <div id="missingTasksDiv"></div>\
                        <div id="paramsListingDiv">\
                            <form id="runTaskForm" action="#">\
                                <table id="paramsTable"></table>\
                            </form>\
                            <div id="paramGroup_0_0" class="paramGroupSection"><table class="paramsTable"><tbody><tr id="input.filename" class="pRow"><td class="pTitle"><div class="pTitleDiv">input filename*</div></td>\
                                <td class="paramValueTd"><div class="valueEntryDiv"><div class="batchBox" title="A job will be launched for every file with a matching type."><input type="checkbox" id="batchCheckinput.filename"><label for="batchCheckinput.filename">Batch</label><a class="batchHelp" href="http://www.broadinstitute.org/cancer/software/genepattern/how-batching-works-in-genepattern-3-9-5" target="_blank"><img src="/gp/images/help_small.gif" width="12" height="12"></a></div><div class="fileDiv mainDivBorder ui-droppable" id="fileDiv-input.filename-1"><div class="fileUploadDiv"><button class="uploadBtn ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" type="button" role="button" aria-disabled="false"><span class="ui-button-text">Upload File...</span></button><div class="inputFileBtn"><input class="uploadedinputfile requiredParam" type="file"></div><button type="button" class="urlButton ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" role="button" aria-disabled="false"><span class="ui-button-text">Add Path or URL...</span></button><span class="drop-box">Drag Files Here</span><div class="fileSizeCaption"> 2GB file upload limit using the Upload File... button. For files &gt; 2GB upload from the Files tab. </div></div><div class="fileListingDiv"></div></div></div></td></tr><tr class="paramDescription"><td></td><td colspan="3">File containing data to cluster - .res, .gct, .odf</td></tr><tr id="output.base.name" class="pRow"><td class="pTitle"><div class="pTitleDiv">output base name*</div></td><td class="paramValueTd"><div class="valueEntryDiv"><div class="tagsContent textDiv "><div class="batchBox" title="A job will be launched for every value specified."><input type="checkbox" id="batchCheckoutput.base.name"><label for="batchCheckoutput.base.name">Batch</label><a class="batchHelp" href="http://www.broadinstitute.org/cancer/software/genepattern/how-batching-works-in-genepattern-3-9-5" target="_blank"><img src="/gp/images/help_small.gif" width="12" height="12"></a></div><input type="text" class="pValue" id="output_base_nameText" value="<input.filename_basename>_KMcluster_output"></div></div></td></tr><tr class="paramDescription"><td></td><td colspan="3">The base output file name - .gct</td></tr><tr id="number.of.clusters" class="pRow" style="background-color: rgb(245, 245, 245);"><td class="pTitle"><div class="pTitleDiv">number of clusters*</div></td>\
                                <td class="paramValueTd"><div class="valueEntryDiv"><div class="tagsContent textDiv "><div id="numClustersBatchBox" class="batchBox" title="A job will be launched for every value specified."><input type="checkbox" id="batchChecknumber.of.clusters" checked="checked"><label for="batchChecknumber.of.clusters">Batch</label><a class="batchHelp" href="http://www.broadinstitute.org/cancer/software/genepattern/how-batching-works-in-genepattern-3-9-5" target="_blank"><img src="/gp/images/help_small.gif" width="12" height="12"></a></div><input class="pValue" id="number_of_clustersText" style="display: none;"><div id="number_of_clustersText_tagsinput" class="tagsinput" style="width: 88%; min-height: 40px; height: 100%;"><div id="number_of_clustersText_addTag"><input id="number_of_clustersText_tag" value="" data-default="Add value and press enter..." style="color: rgb(204, 204, 204); width: 170px;"></div><div class="tags_clear"></div></div></div></div></td></tr><tr class="paramDescription" style="background-color: rgb(245, 245, 245);"><td></td><td colspan="3">Number of centroids (clusters)</td></tr><tr id="seed.value" class="pRow"><td class="pTitle"><div class="pTitleDiv">seed value*</div></td><td class="paramValueTd"><div class="valueEntryDiv"><div class="tagsContent textDiv "><div class="batchBox" title="A job will be launched for every value specified."><input type="checkbox" id="batchCheckseed.value"><label for="batchCheckseed.value">Batch</label><a class="batchHelp" href="http://www.broadinstitute.org/cancer/software/genepattern/how-batching-works-in-genepattern-3-9-5" target="_blank"><img src="/gp/images/help_small.gif" width="12" height="12"></a></div><input type="text" class="pValue" id="seed_valueText" value="12345"></div></div></td></tr><tr class="paramDescription"><td></td><td colspan="3">Seed value for random number generator</td></tr><tr id="cluster.by" class="pRow"><td class="pTitle"><div class="pTitleDiv">cluster by*</div></td><td class="paramValueTd"><div class="valueEntryDiv"><div class="selectChoice"><div class="batchBox" title="A job will be launched for every value specified."><input type="checkbox" id="batchCheckcluster.by"><label for="batchCheckcluster.by">Batch</label><a class="batchHelp" href="http://www.broadinstitute.org/cancer/software/genepattern/how-batching-works-in-genepattern-3-9-5" target="_blank"><img src="/gp/images/help_small.gif" width="12" height="12"></a></div><select class="choice requiredParam" id="cluster.by_1" style="display: none;"><option value="0">rows</option><option value="1">columns</option></select><button type="button" class="ui-multiselect ui-widget ui-state-default ui-corner-all mSelect" aria-haspopup="true" style="width: 300px;"><span class="ui-icon ui-icon-triangle-1-s"></span><span>rows</span></button></div></div></td></tr><tr class="paramDescription"><td></td><td colspan="3">Whether to cluster by rows or columns</td></tr><tr id="distance.metric" class="pRow"><td class="pTitle"><div class="pTitleDiv">distance metric*</div></td><td class="paramValueTd"><div class="valueEntryDiv"><div class="selectChoice"><div class="batchBox" title="A job will be launched for every value specified."><input type="checkbox" id="batchCheckdistance.metric"><label for="batchCheckdistance.metric">Batch</label><a class="batchHelp" href="http://www.broadinstitute.org/cancer/software/genepattern/how-batching-works-in-genepattern-3-9-5" target="_blank"><img src="/gp/images/help_small.gif" width="12" height="12"></a></div><select class="choice requiredParam" id="distance.metric_1" style="display: none;"><option value="0">Euclidean</option><option value="1"></option></select><button type="button" class="ui-multiselect ui-widget ui-state-default ui-corner-all mSelect" aria-haspopup="true" style="width: 300px;"><span class="ui-icon ui-icon-triangle-1-s"></span><span>Euclidean</span></button></div></div></td></tr><tr class="paramDescription"><td></td><td colspan="3">How to compute distance between points</td></tr></tbody></table></div></div>\
                                <div id="runTaskMiscDiv">\
                                    <div class="pHeaderTitleDiv top-level-background">\
                                        <img src="/gp/images/toggle_collapse.png" alt="toggle image" width="19" height="19" class="paramSectionToggle">\
                                        Comments\
                                        </div>\
                                        <div class="commentsContent paramgroup-spacing">\
                                            <textarea id="jobComment" placeholder="Add a comment..."></textarea>\
                                        </div>\
                                        <div class="pHeaderTitleDiv top-level-background">\
                                            <img src="/gp/images/toggle_collapse.png" alt="toggle image" width="19" height="19" class="paramSectionToggle">\
                                            Tags\
                                            </div>\
                                            <div class="tagsContent paramgroup-spacing">\
                                                <input id="jobTags" style="display: none;"><div id="jobTags_tagsinput" class="tagsinput" style="width: 97%; min-height: 40px; height: 100%;"><div id="jobTags_addTag"><input id="jobTags_tag" value="" data-default="Add tag and press enter..." class="ui-autocomplete-input" autocomplete="off" style="color: rgb(204, 204, 204); width: 170px;"><span role="status" aria-live="polite" class="ui-helper-hidden-accessible"></span></div><div class="tags_clear"></div></div>\
                                                </div>\
                                                </div>\
                                                <hr>\
                                                    <div style="height: 40px;">\
                                                        <span class="floatLeft otherControlsDiv">\
                                                        </span>\
                                                        <span class="floatRight submitControlsDiv">\
                                                            <button class="Reset ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" role="button" aria-disabled="false"><span class="ui-button-text"><img src="/gp/images/reset.gif" width="16" height="16" alt="Reset" class="buttonIcon"> Reset </span></button>\
                                                                <button class="Run ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" role="button" aria-disabled="false"><span class="ui-button-text"><img src="/gp/images/run-green.gif" width="16" height="16" alt="Run" class="buttonIcon"> Run </span></button>\
                                                                </span>\
                                                    </div>\
                                                </div>\
                                            </div>';
}
