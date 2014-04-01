
var last_left_nav_tab = null;

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
                $("#submitJob").find(".uploadedinputfile").hide();
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
});

function jqEscape(str) {
    return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}
