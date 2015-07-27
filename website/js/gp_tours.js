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
                element: "#cite-gp",
                intro: '<div class="tour-header"> Cite GenePattern </div> The GenePattern citation is available here. ',
                position: 'top'
            },
            {
                element: "#left-nav-files-refresh",
                intro: '<div class="tour-header"> Refresh Files Tab </div>  The files in the Files tab can now be refreshed using the <i>Refresh</i>.',
                position: 'right'
            },
            {
                element: "#betaInfoDiv",
                intro: '<div class="tour-header"> Beta Modules </div> A label is now displayed for modules which are in beta release. ',
                position: 'bottom'
            },
            {
                intro: '<div class="tour-header"> The End</div> This is the end of the tour. To learn more about what'
                    + ' is new, please see the <a href="http://www.broadinstitute.org/cancer/software/genepattern/doc/relnotes/3.9.4" target="_blank">release notes</a>.',
            }
        ];

        var mainPaneHtml;

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
            if(intro._currentStep === 0)
            {
                $( "#left-nav" ).tabs( "option", "active", 2 );

                $('#main-pane').animate({
                    scrollTop: ($('#cite-gp').offset().top)
                }, 0);
            }
            else if(intro._currentStep === 1 && intro._direction==="forward")
            {
                mainPaneHtml = $("#main-pane").html();
                $("#main-pane").html(getBetaMainPaneHtml());

                $('#main-pane').animate({
                 scrollTop: ($('#betaInfoDiv').offset().top)
                 }, 0);
            }
        });


        /*intro.onafterchange(function(targetElement)
        {

        });*/

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

function launchTour()
{
    //alert("webtour: " + window.location);
    /*if (RegExp('webtour', 'gi').test(window.location))
     {
     alert("found webtour step 8");
     var introJobRes = introJs();

     //create the steps
     var steps = [
     {
     element: "#jobTable_paginate",
     intro: '<div class="tour-header"> Job Results Summary Page </div> Paginate',
     position: 'right',
     scrollToElement: true
     }
     ];

     introJobRes.setOptions({
     steps: steps,
     showStepNumbers: false,
     skipLabel: "End tour",
     tooltipClass: "tourStyle"
     });

     //while($("#jobTable_paginate".length == 0)){}
     introJobRes.start();
     }*/
}


function getBetaMainPaneHtml()
{
    return  ' <table class="group" cellpadding="2" cellspacing="0" width="100%">\
    <tbody>\
        <tr class="vertAlignTop">\
            <td class="buttonCol"><table cellpadding="2" cellspacing="2" width="100%">\
                <tbody>\
                    <tr class="vertAlignTop">\
                        <td class="leftAlignCol"><span style="font-weight:bold; color:red;"></span></td>\
                    </tr>\
                    <tr class="vertAlignTop">\
                        <td class="leftAlignCol"><table></table></td>\
                    </tr>\
                </tbody>\
            </table>\
            </td>\
        </tr>\
    </tbody>\
    </table>\
    <div id="submitJob" style="">\
    <div id="submitForm">\
        <div id="runTaskSettingsDiv">\
            <div class="task_header" id="taskHeaderDiv">\
                <span id="task_name" class="module_header ">ClsFileCreator</span>\
                <span id="task_version_main">\
                    <label for="task_versions">\
                    version\
                    </label>\
                    <span class="normal">3.7</span>\
                </span>\
                <div id="otherOptionsDiv" class="floatRight">\
                    <img id="otherOptions" alt="other options" src="/gp/images/gear_blue_and_white.png" height="20">\
                        <ul id="otherOptionsMenu" class="module-menu ui-menu ui-widget ui-widget-content ui-corner-all" style="position: absolute; top: 22px; left: -164px; display: none;" role="menu" tabindex="0" aria-activedescendant="clone">\
                            <li class="ui-menu-item" role="presentation">\
                                <a id="properties" href="#" onclick="jq("#otherOptionsMenu").hide();showProperties();" class="ui-corner-all" tabindex="-1" role="menuitem">Properties</a>\
                            </li>\
                            <li class="ui-menu-item" role="presentation">\
                                <a id="clone" href="#" onclick="jq("#otherOptionsMenu").hide();cloneTask();" class="ui-corner-all" tabindex="-1" role="menuitem">Clone</a>\
                            </li>\
                            <li class="ui-menu-item" role="presentation">\
                                <a id="export" target="_blank" onclick="jq("#otherOptionsMenu").hide();" href="/gp/makeZip.jsp?name=urn:lsid:broad.mit.edu:cancer.software.genepattern.module.visualizer:00261:3.7" class="ui-corner-all" tabindex="-1" role="menuitem">Export</a>\
                            </li>\
                            <li class="ui-menu-item" role="presentation">\
                                <a id="javaCode" class="viewCode ui-corner-all" href="#" onclick="jq("#otherOptionsMenu").hide();" tabindex="-1" role="menuitem">Java code</a>\
                            </li>\
                            <li class="ui-menu-item" role="presentation">\
                                <a id="matlabCode" class="viewCode ui-corner-all" href="#" onclick="jq("#otherOptionsMenu").hide();" tabindex="-1" role="menuitem">MATLAB code</a>\
                            </li>\
                            <li class="ui-menu-item" role="presentation">\
                                <a id="rCode" class="viewCode ui-corner-all" href="#" onclick="jq("#otherOptionsMenu").hide();" tabindex="-1" role="menuitem">R code</a>\
                            </li>\
                            <li class="ui-menu-item" role="presentation">\
                                <a id="pythonCode" class="viewCode ui-corner-all" href="#" onclick="jq("#otherOptionsMenu").hide();" tabindex="-1" role="menuitem">Python code</a>\
                            </li>\
                            <li class="ui-menu-item" role="presentation">\
                                <a onclick="jq("#otherOptionsMenu").hide();" id="ui-id-53" class="ui-corner-all" tabindex="-1" role="menuitem">\
                                    <input name="toggleDesc" id="toggleDesc" type="checkbox" checked="checked">\
                                        <label for="toggleDesc">\
                                        Show parameter descriptions\
                                        </label>\
                                    </a>\
                                </li>\
                            </ul>\
                        </div>\
                        <a class="floatRight" id="documentation" href="/gp/getTaskDoc.jsp?name=urn:lsid:broad.mit.edu:cancer.software.genepattern.module.visualizer:00261:3.7" target="_blank">Documentation</a>\
                    </div>\
                    <div id="betaInfoDiv">This module is currently in beta.</div>\
                    <table id="description_main">\
                        <tbody>\
                            <tr>\
                                <td id="mod_description">\
                                A tool to create a class label (CLS) file.</td>\
                                <td id="source_info">Source: <img src="/gp/images/winzip_icon.png" width="18" height="16">\
                                    <div id="source_info_tooltip" style="display: none; position: absolute; top: 120px; right: 20px;"></div>\
                                Installed from zip</td>\
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
                                        <button class="Run ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" role="button" aria-disabled="false"><span class="ui-button-text"><img src="/gp/images/run-green.gif" width="16" height="16" alt="Run" class="buttonIcon"> Run </span></button>\
                                        </span>\
                                    </div>\
                                    <hr class="clear">\
                                       <div id="paramsListingDiv">\
                                            <form id="runTaskForm" action="#">\
                                                <table id="paramsTable"></table>\
                                            </form>\
                                            <div id="paramGroup_0_0" class="paramGroupSection"><table class="paramsTable"><tbody><tr id="input.file" class="pRow"><td class="pTitle"><div class="pTitleDiv">input file*</div></td>\
                                            <td class="paramValueTd"><div class="valueEntryDiv"><div class="fileDiv mainDivBorder ui-droppable" id="fileDiv-input.file-1">\
                                                <div class="fileUploadDiv"><button class="uploadBtn ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" type="button" role="button" aria-disabled="false"><span class="ui-button-text">Upload File...</span></button><div class="inputFileBtn"><input class="uploadedinputfile requiredParam" type="file"></div>\
                                                <button type="button" class="urlButton ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" role="button" aria-disabled="false"><span class="ui-button-text">Add Path or URL...</span></button><span class="drop-box">Drag Files Here</span><div class="fileSizeCaption"> 2GB file upload limit using the Upload File... button. For files &gt; 2GB upload from the Files tab. </div></div><div class="fileListingDiv"></div></div></div></td></tr><tr class="paramDescription"><td></td><td colspan="3">A dataset - .gct</td></tr></tbody></table></div></div>\
                                            <div id="runTaskMiscDiv"><div id="launchJSNewWinDiv"><label><input type="checkbox" id="launchJSNewWin">Launch in a new window</label></div>\
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
                                                            <hr>\
                                                                <div style="height: 40px;">\
                                                                    <span class="floatLeft otherControlsDiv">\
                                                                    </span>\
                                                                    <span class="floatRight submitControlsDiv">\
                                                                        <button class="Reset ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" role="button" aria-disabled="false"><span class="ui-button-text">\
                                                                            <img src="/gp/images/reset.gif" width="16" height="16" alt="Reset" class="buttonIcon"> Reset </span></button>\
                                                                            <button class="Run ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" role="button" aria-disabled="false"><span class="ui-button-text">\
                                                                                <img src="/gp/images/run-green.gif" width="16" height="16" alt="Run" class="buttonIcon"> Run </span></button>\
                                                                            </span>\
                                                                        </div>\
                                                                    </div>\
                                                                </div>\
                                                            </div>';
}
