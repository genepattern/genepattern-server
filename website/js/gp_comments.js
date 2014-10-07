function updateCommentTotal(event)
{
    var totalComments = 0;
    if(event != null && event.results != null && event.results.total_comment != undefined
        && event.results.total_comment != null)
    {
        totalComments = event.results.total_comment;
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
        limit: 10,
        auto_refresh: false,
        refresh: 10000,
        placeHolderText: "Add a comment...",
        maxlength: 1023,
        onComplete: updateCommentTotal,
        transition: 'slideToggle'
    });

    $("button.Comments").click(function()
    {
        $('div.comments').toggle();

        var hideMode = $(this).text();
        if (hideMode == "Show Comments") {
            $(this).text("Hide Comments");
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

        //change the toggle image to indicate hide or show
        var imageSrc = toggleImg.attr("src");
        if (imageSrc.indexOf('collapse') != -1)
        {
            imageSrc = imageSrc.replace("collapse", "expand");
        }
        else
        {
            imageSrc = imageSrc.replace("expand", "collapse");
        }

        toggleImg.attr("src", imageSrc);
    });

    //hide by default
    $("#commentsHeader").click();

});