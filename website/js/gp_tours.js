var last_left_nav_tab = null;

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
                intro: '<div class="tour-header"> GP Icon </div>You can click the GenePattern icon to return to this home page at any time.'
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
                intro: '<div class="tour-header"> Files Tab</div> The Files tab lists uploaded files on the GenePattern server',
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
            }
            else if(targetElement.id == "submitJob")
            {
                //hack to not show the hidden native file upload button
               // $("#submitJob").find(".uploadedinputfile").hide();
            }
        });

        intro.onexit(function()
        {
            $( "#left-nav" ).tabs( "option", "active", $(this).data("last-left-nav-tab"));
        });

        intro.oncomplete(function()
        {
            $( "#left-nav" ).tabs( "option", "active", $(this).data("last-left-nav-tab"));

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
                element: "#left-nav-jobs-tab",
                intro: '<div class="tour-header"> Jobs Tab </div>The Jobs tab lists most recently run jobs and their results files.',
                position: 'right',
                scrollToElement: true
            },
            {
                element: ".job-details .job-file",
                intro: '<div class="tour-header"> Jobs Tab </div> Click on a job output file to slide out menu.',
                position: 'bottom',
                scrollToElement: true
            },
            {
                element: "#menus-jobs .file-widget",
                intro: '<div class="tour-header"> Jobs Tab </div> The slide out menu has a new option to <b>Copy To Files Tab</b>. This option will put a copy of the file in the Files Tab',
                position: 'right',
                scrollToElement: true
            },
            {
                element: "#left-nav-files-tab",
                intro: '<div class="tour-header"> Files Tab </div> The Files tab lists uploaded files on the GenePattern server.',
                scrollToElement: true
            },
            {
                element: ".demo_file",
                intro: '<div class="tour-header"> Files Tab </div> Click on a file to slide out menu.',
                scrollToElement: true
            },
            {
                element: ".file-widget",
                intro: '<div class="tour-header"> Files Tab </div> The slide out menu has two new options labeled ' +
                    '<b>Rename File</b> and <b>Move File</b>. <b>Rename File </b> will rename the selected file or directory.' +
                    ' <b>Move File</b> will move the selected file or directory to a different folder in the Files Tab.',
                position: 'right',
                scrollToElement: true
            },
            {
                element: "#job-results",
                intro: '<div class="tour-header"> Job Results Summary Page </div> The Job Results Summary Page list all jobs and their results files that are on the server.',
                position: 'right',
                scrollToElement: true
            },
            {
                element: "#jobTable_length",
                intro: '<div class="tour-header"> Job Results Summary Page </div>The number of jobs listed per page can be set to 10, 20, 50, or 100 with 10 being the default',
                position: 'left',
                scrollToElement: true
            }
        ];

        intro.setOptions({
            steps: steps,
            showStepNumbers: false,
            skipLabel: "End tour",
            doneLabel: "End tour",
            tooltipClass: "tourStyle"
        });


        //include the owner search feature step if this is an admin user
        if($(".adminServerAllowed").is(":visible"))
        {
            steps.push({
                element: "#jobTable_paginate",
                intro: '<div class="tour-header"> Job Results Summary Page </div> Improvements to page navigation allows' +
                    ' navigation to the next, previous, first, last, or a specific page.',
                position: 'left',
                scrollToElement: true
            });

            steps.push({
                element: "#jobTable_filter",
                intro: '<div class="tour-header"> Job Results Summary Page </div> An admin user, can now '
                + ' search for jobs run by a specific user. <hr/> This is the end of the tour. To learn more about what'
                + ' is new, please see the <a href="http://www.broadinstitute.org/cancer/software/genepattern/doc/relnotes/3.8.2" target="_blank">release notes</a>.',
                position: 'left',
                scrollToElement: true
            });
        }
        else
        {
            steps.push(
            {
                element: "#jobTable_paginate",
                    intro: '<div class="tour-header"> Job Results Summary Page </div> Page navigation has also been improved.' +
                'You can navigate to the next, previous, first, last, or a specific page. <hr/>This is the end of the tour. To learn more about what'
                + ' is new, please see the release notes. <a href="http://www.broadinstitute.org/cancer/software/genepattern/doc/relnotes/3.8.2" target="_blank">release notes</a>.',
                position: 'left',
                scrollToElement: true
            });
        }

        intro.onbeforechange(function(targetElement)
        {
            //switch the active left navigation tab to the appropriate one for the step
            if(intro._currentStep == 0)
            {
                $(".search-widget").searchslider("hide");

                var demoJobJson = {
                    datetime: "2014-04-08 11:33:23.4",
                    jobId: "0", numOutputFiles: 1,
                    outputFiles: [
                        {
                            fileLength: 1949,
                            kind: "txt",
                            lastModified: 1396971203000,
                            link:
                            {
                                href: "http://127.0.0.1:8080/gp/jobResults/0/demo_job_result.gct",
                                name: "demo_job_result.gct",
                                sendTo: [ "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00236:5.9"]
                            }
                        }
                    ],
                    status:
                    {
                        executionLogLocation: "http://127.0.0.1:8080/gp/jobResults/0/gp_execution_log.txt",
                        hasError: false,
                        isFinished: true,
                        isPending: false
                    },
                    taskLsid: "urn:lsid:broad.mit.edu:demo.software.genepattern.module.analysis:00002:1",
                    taskName: "DemoAnalysis"
                }

                var tab = $("#left-nav-jobs-list");
                tab.empty();

                // Clear away any old jobs menus
                $("#menus-jobs").empty();

                renderJob(demoJobJson, tab);

                $( "#left-nav" ).tabs( "option", "active", 1);
            }
            else if(intro._currentStep == 1)
            {
                $(".job-details .job-file").find("a").first().click();
            }
            else if(intro._currentStep == 2)
            {
                //hack to grab the slide out menu for a result file
                $("#menus-jobs .file-widget").last().addClass("gp-tour-step");

                //highlight the "Copy To Files" button
                $("#menus-jobs .file-widget-actions").find(".module-listing").last().css("border", "3px solid red");
            }
            else if(intro._currentStep == 3)
            {
                $(".search-widget").searchslider("hide");

                $( "#left-nav" ).tabs( "option", "active", 2);

                $("#uploadTree").empty();
                $("#uploadTree").append('<ul>'+
                '<li class="jstree-last jstree-open" id="__2F"><ins class="jstree-icon">&nbsp;</ins><a data-partial="true" name="uploads" ' +
                    'onclick="JavaScript:openFileWidget(this,\'#menus-uploads\'); return false;" data-kind="directory" href="#"'+
                'class=""><ins class="jstree-icon">&nbsp;</ins>uploads </a> '+
                '<ul>' +
                    '<li class="jstree-last jstree-leaf"><ins class="jstree-icon">&nbsp;</ins><a ' +
                    ' title="2012-11-28 - 1.0 KB" data-partial="false" name=“demo_file.gct” ' +
                    'onclick="JavaScript:openFileWidget(this, \'#menus-uploads\'); return false;" data-kind=“get” href="#" class="demo_file"><ins class="jstree-icon">&nbsp;</ins><em>demo_file.gct</em></a>'+
                    '</li>' +
                '</ul>'+
                '</li> '+
                '</ul>');
            }
            else if(intro._currentStep == 4)
            {
                $(".demo_file").click();
            }
            else if(intro._currentStep == 5)
            {
                $("#menus-uploads .file-widget-actions").find(".module-listing:nth-child(5)").css("border", "3px solid red");
                $("#menus-uploads .file-widget-actions").find(".module-listing:nth-child(6)").css("border", "3px solid red");
            }
            else if(intro._currentStep == 6)
            {
                $(".search-widget").searchslider("hide");

                loadJobResults(true);

            }
        });

        intro.onexit(function()
        {
            $(".search-widget").searchslider("hide");

            initRecentJobs();

            //reset to original state before start of tour
            $( "#left-nav" ).tabs( "option", "active", last_left_nav_tab);

            $(".gp-tour-step").removeClass("gp-tour-step");

            $("#uploadTree").jstree("refresh");

            //window.location.href = startUrl;
            window.location.href = "/gp";
        });

        intro.oncomplete(function()
        {
            $(".search-widget").searchslider("hide");

            initRecentJobs();

            //reset to original state before start of tour
            $( "#left-nav" ).tabs( "option", "active", last_left_nav_tab);

            $(".gp-tour-step").removeClass("gp-tour-step");

            $("#uploadTree").jstree("refresh");
            window.location.href = "/gp";
        });

        last_left_nav_tab =  $("#left-nav").tabs( "option", "active");

        intro.start();
    });

});

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

function jqEscape(str) {
    return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}
