function updateCommentTotalAfterLoad(event)
{
    var totalComments = 0;
    if(event != null && event.results != null && event.results.total_comment != undefined
        && event.results.total_comment != null)
    {
        totalComments = event.results.total_comment;
    }
    $("#commentHeaderTotal").empty().append("(" +  totalComments + ")");

    alert("comment total updated");
}

function updateCommentTotalAfterUpdate(event)
{
    var totalComments = 0;
    if(event != null && event.total_comment != undefined
        && event.total_comment != null)
    {
        totalComments = event.total_comment;
    }
    $("#commentHeaderTotal").empty().append("(" +  totalComments + ")");
}

function setupComments()
{
    $('.comments').comment({
        title: 'Comments',
        url_get: '/gp/rest/v1/jobs/' + currentJobNumber + '/comments',
        url_input: '/gp/rest/v1/jobs/' + currentJobNumber + '/comments/add',
        url_delete: '/gp/rest/v1/jobs/' + currentJobNumber + '/comments/delete',
        limit: 100,
        auto_refresh: false,
        refresh: 10000,
        placeHolderText: "Add a comment...",
        maxlength: 1023,
        onComplete: updateCommentTotalAfterLoad,
        onUpdate: updateCommentTotalAfterUpdate,
        transition: 'slideToggle'
    });

    $("button.Comments").click(function()
    {
        $('div.comments').toggle();

        var isVisible = false;

        var hideMode = $(this).text();
        if (hideMode == "Show Comments") {
            $(this).text("Hide Comments");
            isVisible = true;
        }
        else
        {
            $(this).text("Show Comments");
        }



    });
}

$(function() {

    setupComments();

    $("#commentsHeader").click(function () {
        $(this).next().toggle();

        var toggleImg = $(this).find(".sectionToggle");

        if (toggleImg == null) {
            //toggle image not found
            // just log error and return
            console.log("Could not find toggle image for hiding and showing parameter groups sections");

            return;
        }

        var isVisible = false;
        //change the toggle image to indicate hide or show
        var imageSrc = toggleImg.attr("src");
        if (imageSrc.indexOf('collapse') != -1)
        {
            imageSrc = imageSrc.replace("collapse", "expand");
        }
        else
        {
            imageSrc = imageSrc.replace("expand", "collapse");
            isVisible = true;
        }

        toggleImg.attr("src", imageSrc);

        /*if (isVisible) {
            $.cookie("show_job_comments", "true");
        }
        else {
            $.removeCookie("show_job_comments");
        }*/
    });


    //if comments were already visible before refresh then keep it that way
   // if (!($.cookie("show_job_comments")))
   // {
        $("#commentsHeader").click();
   // }
});