
QUnit.test("createChoiceDiv", function(assert) {
    // test createFileDivId
    assert.equal(createFileDivId("input.file", ""), "fileDiv-input.file-", "createFileDivId, empty groupId");
    assert.equal(createFileDivId("input.file", null), "fileDiv-input.file-", "createFileDivId, null groupId");
    assert.equal(createFileDivId("input.file", 1), "fileDiv-input.file-1", "createFileDivId, basic groupId");
    assert.equal(createFileDivId("input.file", 2), "fileDiv-input.file-2", "createFileDivId, an additional groupId");

    // test getCustomChoices
    var testChoiceInfo= { choices: [ 
            { name: "", value: ""}, 
            { name: "A", "value": "A.txt" }, 
            { value: "B.txt"}, 
            { value: "C.txt"} 
    ]};

    assert.deepEqual(getCustomChoices(
            testChoiceInfo,
            ["A.txt"]),
            [],
            "getCustomChoices from [\"\",A,B,C], initialValues=[A]");

    assert.deepEqual(getCustomChoices(
            testChoiceInfo,
            ["D.txt"]),
            ["D.txt"],
            "getCustomChoices from [\"\",A,B,C], initialValues=[D]");

    assert.deepEqual(getCustomChoices(
            testChoiceInfo,
            [""]),
            [],
            "getCustomChoices from [\"\",A,B,C], initialValues=[\"\"]");

    assert.deepEqual(getCustomChoices(
            testChoiceInfo,
            null),
            [],
            "getCustomChoices from [\"\",A,B,C], initialValues=null");

    assert.deepEqual(getCustomChoices(
            testChoiceInfo,
            ["A.txt", "D.txt"]),
            ["D.txt"],
            "getCustomChoices from [\"\",A,B,C], initialValues=[A,D]");

    assert.deepEqual(getCustomChoices(
            testChoiceInfo,
            ["", "A.txt"]),
            [],
            "getCustomChoices from [\"\",A,B,C], initialValues=[\"\",A]");

    assert.deepEqual(getCustomChoices(
            testChoiceInfo,
            ["D.txt", ""]),
            ["D.txt"],
            "getCustomChoices from [\"\",A,B,C], initialValues=[D, \"\"]");

    assert.deepEqual(getCustomChoices(
            testChoiceInfo,
            ["A.txt", "D.txt", "A.txt", "B.txt", "D.txt"]),
            ["D.txt", "D.txt"],
            "getCustomChoices from [\"\",A,B,C], initialValues=[A,D,A,B,D]");

  });

