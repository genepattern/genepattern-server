function updateCommentTotalAfterLoad(event)
{
    var totalComments = 0;
    if(event != null && event.results != null && event.results.total_comment != undefined
        && event.results.total_comment != null)
    {
        totalComments = event.results.total_comment;
    }
    $("#commentHeaderTotal").empty().append("(" +  totalComments + ")");

    var comment = $.cookie("show_comments_value"+currentJobNumber);

    if(comment != undefined && comment != null)
    {
        $("#commentsContent").find(".posted-comments-postbox").find("textarea").val(comment);
    }

    if($.cookie("show_comments_focus"+currentJobNumber))
    {
        $("#commentsContent").find(".posted-comments-postbox").find("textarea").focus();
    }
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
        transition: 'none'
    });
}

$(function()
{
    setupComments();

    $("#commentsHeader").click(function () {
        $(this).next().toggle();

        var toggleImg = $(this).find(".sectionToggle");

        if (toggleImg == null) {
            //toggle image not found
            // just log error and return
            console.log("Could not find toggle image for hiding and showing comments section");

            return;
        }

        var isVisible = false;
        //change the toggle image to indicate hide or show
        var imageSrc = toggleImg.attr("src");
        if (imageSrc.indexOf('collapse') != -1)
        {
            imageSrc = imageSrc.replace("collapse", "expand");
            $.removeCookie("show_comments_"+currentJobNumber);
        }
        else
        {
            imageSrc = imageSrc.replace("expand", "collapse");
            isVisible = true;
            $.cookie("show_comments_"+currentJobNumber, true);
        }

        toggleImg.attr("src", imageSrc);
    });

    if($.cookie("show_comments_"+currentJobNumber) == null)
    {
        $("#commentsHeader").click();
    }

    //keep track of changes to the comment text area
    $("#commentsContent").on("keyup", "textarea", function()
    {
        $.cookie("show_comments_value"+currentJobNumber, $(this).val());
    });

    //keep track of changes to the comment text area
    $("#commentsContent").on("focus", "textarea", function()
    {
        $.cookie("show_comments_focus"+currentJobNumber, true);
    });

    //keep track of changes to the comment text area
    $("#commentsContent").on("blur", "textarea", function()
    {
        $.removeCookie("show_comments_focus"+currentJobNumber);
    });

});