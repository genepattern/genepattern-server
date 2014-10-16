var jobTagToIdMap = {};

function updateTagsTotal()
{
    var totalTags = Object.keys(jobTagToIdMap).length;
    $("#tagHeaderTotal").empty().append("(" +  totalTags + ")");
}

function addTag(tag)
{
    var queryString = "?tagText="+tag;
    $.ajax({
        type: "POST",
        url: "/gp/rest/v1/jobs/" + currentJobNumber +"/tags/add"+ queryString,
        cache: false,
        success: function (response) {

            console.log(response);
            console.log("job tag id: " + response.jobTagId);


            if(response != undefined && response != null && response.success
                && response.jobTagId != undefined && response.jobTagId != null)
            {
                jobTagToIdMap[tag] = response.jobTagId;
                updateTagsTotal();
            }
            else
            {
                $('#statusJobTags').removeTag(tag);
            }
        },
        dataType: "json",
        error: function (xhr, ajaxOptions, thrownError) {
            console.log("Response from server: status=" + xhr.status + " text=" + xhr.responseText);
            console.log(thrownError);
        }
    });
}

function deleteTag(tag)
{
    if( jobTagToIdMap[tag] == undefined || jobTagToIdMap[tag] == null)
    {
        //do nothing since id for tag could not be found
        return;
    }

    var jobTagId = jobTagToIdMap[tag];

    var queryString = "?jobTagId="+jobTagId;
    $.ajax({
        type: "POST",
        url: "/gp/rest/v1/jobs/" + currentJobNumber +"/tags/delete"+ queryString,
        cache: false,
        success: function (response) {

            console.log(response);

            delete jobTagToIdMap[tag];

            updateTagsTotal();
        },
        error: function (xhr, ajaxOptions, thrownError) {
            console.log("Response from server: status=" + xhr.status + " text=" + xhr.responseText);
            console.log(thrownError);
        }
    });
}

$(function() {
    var jobTagsInput = $('#statusJobTags').tagsInput(
    {
        defaultText:'Add tags...',
        width: '98%',
        height: '40px',
        onAddTag: addTag,
        onRemoveTag: deleteTag,
        autocomplete_url: '/gp/rest/v1/tags/',
        autocomplete:{
            minLength: 0,
            response: tagResponse
        },
        maxChars: 511
    });

    //import the tags
    $.ajax({
        type: "GET",
        url: "/gp/rest/v1/jobs/" + currentJobNumber +"/tags",
        cache: false,
        success: function (response) {

            console.log(response);
            if(response != null && response.tags != undefined && response.tags != null)
            {
                jobTagToIdMap = response.tags;

                var tagValues = Object.keys(jobTagToIdMap);
                tagValues.sort();

                jobTagsInput.importTags(tagValues.join(","));
                updateTagsTotal();
            }
        },
        dataType: "json",
        error: function (xhr, ajaxOptions, thrownError) {
            console.log("Response from server: status=" + xhr.status + " text=" + xhr.responseText);
            console.log(thrownError);
        }
    });

    $("#tagsHeader").click(function () {
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
    });

    $("#tagsHeader").click();
});
