
$(function()
{
    $("#gp_tours").click(function(event)
    {
        event.preventDefault();
        var intro = introJs();
        intro.setOptions({
            steps: [
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
                },
                {
                    element: '#protocols',
                    intro: "The center pane is the main display pane, which GenePattern uses to display information and to prompt you for input. " +
                        "Right now, the protocols are displayed here.",
                    position: 'left'
                }
            ],
            showStepNumbers: false,
            skipLabel: "End tour"
        });

        intro.onbeforechange(function(targetElement)
        {
            //switch the active left navigation tab to the approptiate one for the step
            if(intro._currentStep == 2)
            {
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
        });

        intro.start();
    });
});

function jqEscape(str) {
    return str.replace(/([;&,\.\+\*\~':"\!\^$%@\[\]\(\)=>\|])/g, '\\$1');
}
