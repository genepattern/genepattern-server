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
        var commentObj = JSON.parse(comment);

        var index = commentObj.index;
        if(index == 0)
        {
            var textArea = $(".posted-comments-postbox").find("textarea").first();
            textArea.val(commentObj.text);
        }
    }

    if($.cookie("show_comments_focus"+currentJobNumber))
    {
        $("#commentsContent").find(".posted-comments-postbox").find("textarea").focus();
    }

    //only execute this once
    if($("#jobCommentsBtn").length == 0)
    {
        //the jqueryinput tags plugin submits a comment when ENTER button is pressed
        //instead we want to the user to press a submit button

        var newCommentTextArea = $("#commentsContent").find(".posted-comments-postbox").find("textarea").clone(true);
        newCommentTextArea.addClass("newJobComment");
        $("#commentsContent").append(newCommentTextArea);
        $(".newJobComment").hide();

        $("#commentsContent").find(".posted-comments-postbox").find("textarea").off("keypress");

        var submitComment = $("<button id='postJobCommentsBtn'>Save Comment</button>");
        submitComment.button().click(function()
        {
            var comment = $("#commentsContent").find(".posted-comments-postbox").find("textarea").val();
            $(".newJobComment").val(comment);

            //trigger enter event
            $(".newJobComment").trigger(jQuery.Event( 'keypress', { keyCode: 13, which: 13 } ));
        });

        $("#commentsContent").find(".posted-comments-postbox").append(submitComment);

        $(".posted-comments").on("focus", "textarea", function()
        {
            if($(this).siblings("textarea").length == 0)
            {
                var newEditedComment = $(this).clone(true);
                newEditedComment.addClass("editJobComment");
                $(this).before(newEditedComment);
                newEditedComment.hide();

                var editCommentBtn = $("<button class='editJobCommentsBtn'>Save Comment</button>");
                editCommentBtn.button().click(function(event)
                {
                    event.preventDefault();
                    var originalTextArea = $(this).prev("textarea");
                    var comment = originalTextArea.val();

                    var hiddenTextArea = originalTextArea.prev("textarea");
                    hiddenTextArea.val(comment);

                    //trigger enter event
                    hiddenTextArea.trigger(jQuery.Event( 'keypress', { keyCode: 13, which: 13 } ));

                    hiddenTextArea.hide();
                    $(this).hide();

                    //update the Cancel link if this was an edited comment
                    var editCommentLinks = $(this).parents(".posted-comment-container").find(".post-edit");
                    editCommentLinks.find("a").text("Edit");
                });

                $(this).after(editCommentBtn);

                $(this).off("keypress");
            }
            else
            {
                //There should already be a submit so just show it
                $(this).next().show();
            }
        });
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

    $.removeCookie("show_comments_value"+currentJobNumber);
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

    $.cookie.json = true;
    //keep track of changes to the comment text area
    $("#commentsContent").on("keyup", "textarea", function()
    {
        var index = $("#commentsContent textarea").index(this);

        var commentsObj = { text: $(this).val(), index: index};

        $.cookie("show_comments_value"+currentJobNumber, JSON.stringify(commentsObj));
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