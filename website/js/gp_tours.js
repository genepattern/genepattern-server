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
                element: "img[src*='GP-logo.gif']",
                intro: "You can click the GenePattern icon to return to this home page at any time."
            },
            {
                element: '#navband1',
                intro: "The navigation bar provides access to other pages."
            },
            {
                element: '#left-nav-modules-tab',
                intro: 'The Modules tab lists the analyses that you can run. Enter the first few characters of a module or pipeline name in the search box to locate that analysis.',
                position: 'right'
            },
            {
                element: '#left-nav-jobs-tab',
                intro: 'The Jobs tab lists the most recent analyses that you have run and their results files.',
                position: 'right'
            },
            {
                element: '#left-nav-files-tab',
                intro: 'The Files tab lists files that you have uploaded to the GenePattern server.',
                position: 'right'
            }];


        if($("#protocols").is(":visible"))
        {
            steps.push({
                element: '#protocols',
                intro: "The center pane is the main display pane, which GenePattern uses to display information and to prompt you for input.",
                position: 'left'
            });
        }

        if($("#submitJob").is(":visible"))
        {
            steps.push({
                element: '#submitJob',
                intro: "The center pane is the main display pane, which GenePattern uses to display information and to prompt you for input.",
                position: 'left'
            });
        }

        intro.setOptions({
            steps: steps,
            showStepNumbers: false,
            skipLabel: "End tour",
            tooltipClass: "tourStyle"
        });

        intro.onbeforechange(function(targetElement)
        {
            //hack to not show the hidden native file upload button
            $("#submitJob").find(".uploadedinputfile").hide();

            //switch the active left navigation tab to the approptiate one for the step
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

    $(".gp_3_8_1").click(function(event)
    {
        event.preventDefault();
        var intro = introJs();

        //create the steps
        var steps = [
            {
                element: "#left-nav-files-tab",
                intro: '<div class="tour-header"> Files Tab </div> The Files tab lists files that you have uploaded to the GenePattern server.',
                scrollToElement: true
            },
            {
                element: '#upload-dropzone-wrapper',
                intro: '<div class="tour-header"> Files Tab </div> Files can be drag and dropped to this target, which will trigger the selection of a ' +
                    ' destination directory, or to directories listed above. Files can also be selected from a ' +
                    'file browser by clicking this target.',
                position: 'top',
                scrollToElement: true
            },
            {
               // element: '#uploadTree',
                element: '#uploadTree a',
                intro: '<div class="tour-header"> Files Tab </div> Clicking on a target folder will bring up a slide-out menu.',
                scrollToElement: true
            },
            {
                element: '.file-widget',
                intro: '<div class="tour-header"> Files Tab </div> The slide-out menu has options for deleting the directory, creating a subdirectory within ' +
                    'the selected directory and uploading a file to the selected directory. ',
                position: 'right',
                scrollToElement: true
            },
            {
                element: "#left-nav-jobs-tab",
                intro: '<div class="tour-header"> Jobs Tab </div>The Jobs tab contains the list of most recently ' +
                    'run jobs and their result files.',
                position: 'right',
                scrollToElement: true
            },
            {
                element: ".job-name",
                intro: '<div class="tour-header"> Jobs Tab </div>Clicking a job will bring up a slide-out menu.',
                position: 'bottom',
                scrollToElement: true
            },
            {
                element: "#menus-jobs .file-widget",
                intro: '<div class="tour-header"> Jobs Tab </div> This slide-out menu has options for viewing the job ' +
                    'status, downloading a copy of the job, reloading a job and deleting a job.',
                position: 'right',
                scrollToElement: true
            },
            {
                element: ".job-details .job-file",
                intro: '<div class="tour-header"> Jobs Tab </div> Clicking a job output file will also bring up a ' +
                    'slide-out menu.',
                position: 'bottom',
                scrollToElement: true
            },
            {
                element: ".gp-tour-step",
                intro: '<div class="tour-header"> Jobs Tab </div> This slide-out menu has options for deleting the file, saving the file, ' +
                    'creating a provenance pipeline from the file, and sending the output file to another ' +
                    'module for downstream processing. <hr/> This is the end of tour. For more information please' +
                    ' see the <a href="http://www.broadinstitute.org/cancer/software/genepattern/doc/relnotes/3.8.1">release notes</a>.',
                position: 'right',
                scrollToElement: true
            }
        ];

        intro.setOptions({
            steps: steps,
            showStepNumbers: false,
            skipLabel: "End tour",
            tooltipClass: "tourStyle"
        });

        intro.onbeforechange(function(targetElement)
        {
            //switch the active left navigation tab to the approptiate one for the step
            if(intro._currentStep == 0)
            {
                $( "#left-nav" ).tabs( "option", "active", 2);
            }
            else if(intro._currentStep == 2)
            {
                $("#uploadTree").find("a").first().click();
            }
            else if(intro._currentStep == 4)
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
            else if(intro._currentStep == 5)
            {
                $(".job-name").find("a").first().click();
            }
            else if(intro._currentStep == 7)
            {
                $(".job-details .job-file").find("a").first().click();
            }
            else if(intro._currentStep == 8)
            {
                //hack to grab the slide out menu for a result file
                $("#menus-jobs .file-widget").last().addClass("gp-tour-step");
            }
        });

        intro.onexit(function()
        {
            $(".search-widget").searchslider("hide");

            initRecentJobs();

            //reset to original state before start of tour
            $( "#left-nav" ).tabs( "option", "active", last_left_nav_tab);

            $(".gp-tour-step").removeClass("gp-tour-step");
        });

        intro.oncomplete(function()
        {
            $(".search-widget").searchslider("hide");

            initRecentJobs();

            //reset to original state before start of tour
            $( "#left-nav" ).tabs( "option", "active", last_left_nav_tab);


            $(".gp-tour-step").removeClass("gp-tour-step");
        });

        last_left_nav_tab =  $("#left-nav").tabs( "option", "active");

        intro.start();
    });
});

function jqEscape(str) {
    return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}
