<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>
<%@ page import="org.genepattern.server.util.MessageUtils" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <head>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link rel="SHORTCUT ICON" href="favicon.ico">
        <title>GenePattern add/update module help</title>
        <style>
            .example {
                font-family: Courier, Courier New, serif;
                font-size: 10pt;
            }

            .exampleLink {
                font-family: Courier, Courier New, serif;
                font-size: 10pt;
                color: blue;
                text-decoration: underline
            }
        </style>
        <%
            MessageUtils messages = new MessageUtils();
        %>
        <jsp:include page="navbarHead.jsp" />
    </head>

    <body>
        <jsp:include page="navbar.jsp" />
        <h2><%=messages.get("ApplicationName")%> add/update analysis module help</h2>

        <h4>Adding a new module to the <%=messages.get("ApplicationName")%> server</h4>

        If you have an algorithm or utility that you would like to add to <%=messages.get("ApplicationName")%>,
        you'll need to upload it to the <%=messages.get("ApplicationName")%> server, along with descriptive information that the clients (graphical client, web browser, R, Java) will use to display and run it. It takes just a few minutes to enter the
        necessary information. Once you have done so, you can run the module immediately and can
        share it with others. You'll be able to decide which parameters from the algorithm to
        expose to the user, and can replace command line parameter names that are hard to remember
        with some that are self-explanatory. You can also create drop-down list choices for
        parameters, further reducing the ability to invoke a module with bad inputs when the choices
        are constrained.<br><br>

        When you save a module, the information about it that you have typed in is valdated to make sure of the following:
        <ul>
            <li> Every parameter you haven't marked as optional must be listed in the command line.</li>
            <li> Every command line parameter must be either a parameter, environment variable, or system property.</li>
            <li> The module name and parameter names must be legal - in general, you should avoid punctuation marks and other special characters.</li>
        </ul>
        If everything checks out, the uploaded files are saved in the <%=messages.get("ApplicationName")%> module library and the module registered in the module database. The module and its uploaded files are indexed in the background so that they are immediately available for searching.<br><br>

        Each input field is explained in detail below. There are three sections to the form:
        <ol>
            <li><b>Authorship Information.</b> A name, description, and other <a href="#taskLevelAttributes">module-level attributes</a>.</li>
            <li><b>Support Files. </b><a href="#supportFiles">Support files</a> are your module plus any accompanying applications, scripts, libraries, documentation, configuration files, etc.</li>
            <li><b>Parameter Information.</b> Descriptions of each <a href="#inputParameters">input parameter</a> (name, datatype, description, etc.).
            </li>
        </ol>

        An example for each field is given based on the Consensus Clustering module, which may be
        uploaded from the
        <a href="<%=request.getContextPath()%>/pages/taskCatalog.jsf?taskType=Clustering&state=new&state=updated&state=up%20to%20date&name=ConsensusClustering">module repository</a> if you haven't already installed it.
        <br>

        <h2><a name="taskLevelAttributes">Entering module-level attributes</a></h2>

        <a name="Name"><h4>Name</h4></a>
        The name of the module will be used in the drop-down module catalog lists and as a directory name on the server with
        this name. It should be a short but descriptive name, without spaces or punctuation, and may be mixed
        upper- and lower-case.<br><br> ConsensusClustering example: <span class="example">ConsensusClustering</span>

        <a name="catalog"><h4>Module catalog</h4></a>
        This is a drop-down list of the modules currently installed. You can use this to view or edit the parameters for a currently installed module.
        The "run" button gives you the option of running the selected module.
        The "clone..." button allows you to copy a module to a new name prior to editing it. The suggested name
        for a cloned module is copyOf<i>original</i>.<br><br>

        <a name="LSID"><h4>LSID</h4></a>
        The Life Science Identifier (LSID) used to uniquely identify a GenePattern module. LSIDs are created automatically by the GenePattern server when a module is saved.

        <a name="Description"><h4>Description</h4></a>
        The description is where to explain what your module does, and why someone would want to use it.
        It can be anywhere from a sentence to a short paragraph in length.
        The description, sometimes in abridged form, is displayed in the pipeline designer module choice list,
        in generated code when creating scripts from pipelines, and in the graphical client.
        It's a very good way for you to document succinctly why your module exists.<br><br>

        ConsensusClustering example: <span class="example">Resampling-based clustering method</span>

        <a name="Author"><h4>Author</h4></a>
        Enter the author&apos;s name and affiliation (company or academic institution). If you share this module
        with others, they will know how to give the author credit and whom to contact with questions, suggestions,
        or enhancement ideas.<br><br>

        ConsensusClustering example: <span class="example">Stefano Monti</span>

        <a name="Privacy"><h4>Privacy</h4></a>
        Modules may be marked as either public or private. Public modules are accessible to everyone who uses the server
        on which it resides. Private modules may be accessed only by the module's owner, which is the username that the user logged in with. When a module is first created, the default is to mark it private. Private modules are not visible to others building pipelines or running modules. You can update your module's privacy at any time.

        <br>
        <br>
        ConsensusClustering example: <span class="example">public</span>

        <a name="Quality"><h4>Quality level</h4></a>
        The quality level is a simple three-level classification that lets the user know what level of confidence the
        author has in the robustness of the module. The three levels have no strict definitions. In increasing order of
        quality expectations, they are: are &quot;development&quot;, &quot;preproduction&quot;, and &quot;production&quot;.
        <br>
        <br>
        ConsensusClustering example: <span class="example">development</span>

        <a name="Command"><h4>Command line</h4></a>
        The crux of adding a module to the <%=messages.get("ApplicationName")%> server is to provide the command line that will be used to
        launch the module, including substitutions for settings that will be specified differently for each invocation.
        In the command line field of the form, you will provide a combination of the fixed text and the dynamically-changed
        text which together constitute the command line for an invocation of the module.<br><br>

        Perhaps the trickiest thing about specifying a command line is making it truly platform-independent. Sure, it works fine
        for your computer, right now. But if you zip it and send it to an associate, are they running a Mac? Windows? Unix?
        You may not know, and you shouldn't need to care. By carefully describing the command line using substitution variables,
        you can pretty well ensure that your module will run anywhere.<br><br>

        Parameters that require substitution should be enclosed in brackets (ie. &lt;filename&gt;).
        All parameters listed in the parameters section must be mentioned in the command line
        unless their respective optional field is checked. A default value
        may be provided and will be used if the user fails to specify a value when
        invoking the module.<br><br>

        In addition to parameter names, you may also use environment variables,
        <a href="http://java.sun.com/docs/books/tutorial/essential/system/properties.html" target="_blank" style="white-space: nowrap;">Java system properties</a>,
        and any properties defined in the %<%=messages.get("ApplicationName")%>InstallDir%/resources/genepattern.properties file.
        In particular, there are predefined values for &lt;java&gt;, &lt;perl&gt;, and
        &lt;R&gt;, three languages that are used within various modules that may be downloaded from the module catalog at
        the public <%=messages.get("ApplicationName")%> website. Useful substitution properties include:<br><br>

        <table>
            <tr>
                <td valign="top"><span class="example">&lt;java&gt;</span></td>
                <td>path to Java, the same one running the <%=messages.get("ApplicationName")%> server</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;perl&gt;</span></td>
                <td>path to Perl, installed with <%=messages.get("ApplicationName")%> server on Windows, otherwise the one already installed on your system</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;R&gt;</span></td>
                <td>path to a program that runs R and takes as input a script of R commands. R is installed with <%=messages.get("ApplicationName")%>server on Windows and MacOS</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;java_flags&gt;</span></td>
                <td>memory size and other Java JVM settings from the <%=messages.get("ApplicationName")%>/resources/genepattern.properties file</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;libdir&gt;</span></td>
                <td>directory where the module's support files are stored</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;job_id&gt;</span></td>
                <td>job number</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;name&gt;</span></td>
                <td>name of the module being run</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;<i>filename</i>_basename&gt;</span></td>
                <td>for each input file parameter, the filename without directory</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;<i>filename</i>_extension&gt;</span></td>
                <td>for each input file parameter, the extension without filename or directory</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;<i>filename</i>_file&gt;</span></td>
                <td>for each input file parameter, the input filename without directory</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;path.separator&gt;</span></td>
                <td>Java classpath delimiters (&#58; or &#59;), useful for specifying a classpath for Java-based modules</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;file.separator&gt;</span></td>
                <td>/ or \ for directory delimiter</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;line.separator&gt;</span></td>
                <td>newline, carriage return, or both for line endings</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;user.dir&gt;</span></td>
                <td>current directory where the job is executing</td>
            </tr>
            <tr>
                <td valign="top"><span class="example">&lt;user.home&gt;</span></td>
                <td>user's home directory</td>
            </tr>
        </table>
        <br>

        Rather than having to customize your module's command line for the exact location of the language runtime
        on each computer, you may simply refer to, for example,<br>
        <span class="example">&lt;java&gt; -cp &lt;libdir&gt;mymodule.jar com.foo.MyModule &lt;arg1&gt;</span><br>
        to run your module.  <%=messages.get("ApplicationName")%> will then take care of locating the Java runtime,
        asking it to begin execution at the <span class="example">MyModule</span> class using code from the uploaded file
        <span class="example">mymodule.jar</span>.<br><br>

        Here's the Consensus Clustering command line (actually all on one line): <br><span class="example">&lt;java&gt; &lt;java_flags&gt; -DR_HOME=&lt;R_HOME&gt; -cp &lt;libdir&gt;geneweaver.jar edu.mit.wi.genome.geneweaver.clustering.ConsensusClustering &lt;input.filename&gt; &lt;kmax&gt; &lt;niter&gt; &lt;normalize.type&gt; -N &lt;norm.iter&gt; -S &lt;resample&gt; -t &lt;algo&gt; -L &lt;merge.type&gt; -i &lt;descent.iter&gt; -o &lt;out.stub&gt; -s -d &lt;create.heat.map&gt; -z &lt;heat.map.size&gt; -l1 -v</span>

        <a name="TaskType"><h4>Module Category</h4></a>
        The module type helps someone who is building a pipeline by creating an organizing theme around the types of modules.
        If the module type for a new module doesn't fit well within the existing list, click the &quot;new...&quot;
        button and add a new module type
        entry. The list of module types is created dynamically based upon the module types of all of the installed modules.
        If the last module of a given type is deleted, that module type will be removed from the module type list.<br><br>

        ConsensusClustering example: <span class="example">Clustering</span>

        <a name="cpu"><h4>CPU type</h4></a>
        If your module is compiled for a specific platform (Intel, Alpha, PowerPC, etc.), please indicate that here.
        CPU requirements are enforced when the module is run.<br><br>

        ConsensusClustering example: <span class="example">any</span>

        <a name="os"><h4>Operating system </h4></a>
        If your module requires a specific operating system (Windows, Linux, MacOS, etc.), please indicate that here.
        Operating system requirements are enforced when the module is run.<br><br>

        ConsensusClustering example: <span class="example">any</span>

        <a name="Language"><h4>Language</h4></a>
        There is no specific language support or requirement enforcement at this time. However, by describing the
        primary language that a module is implemented in, you give some hints to the prospective user about their
        system requirements.<br><br>

        ConsensusClustering example: <span class="example">Java</span>

        <a name="MinLanguage"><h4>min. language level</h4></a>
        If your module requires at least a certain revision of the language runtime environment, please indicate which (eg.
        <span class="example">1.3.1_07</span>) to use. This is not currently enforced, but provides useful information to the prospective module
        user.<br><br>

        ConsensusClustering example: <span class="example"><i>none specified</i></span>

        <a name="VersionComment"><h4>Version Comment</h4></a>
        Describes what has changed since the last version of the module.
        <br><br>
        ConsensusClustering example: <span class="example"><i>Added ability to create heatmap images of clusters</i></span>

        <a name="OutputDescription"><h4>Output File Formats</h4></a>

        Select the file format(s) of the output files generated by your module. To select multiple file formats, use CTRL-click. If your module generates an output file format not included in the list, click New to add that format to the list.
        <hr>
        <h2><a name="supportFiles">Uploading, deleting and viewing support files</a></h2>

        <a name="SupportFiles"><h4>Support Files</h4></a>
        Any files required by your module, such as scripts, libraries, property files, DLLs, executable programs, etc. should
        be uploaded to the server in the support files section. These files may be referenced in the command line field
        using the <span class="example">&lt;libdir&gt;<i>filename</i></span> nomenclature.<br><br>

        Any files that you upload will appear in the "Current files" section below the download file selectors. If you
        have more than five files to upload, simply upload the first five, save, edit the module again, and upload more.
        There is no upper limit on the number of files which may be uploaded, assuming there is enough space. Files that
        have been uploaded appear as links on the in this section. You may view or download them by clicking appropriately
        in your browser.
        <br><br>

        If you've mistakenly uploaded files where are not required, or which are no longer necessary, you may select them
        from among those listed in the drop-down list, click the "delete" button, and then confirm that the file should be
        deleted.<br><br>

        ConsensusClustering example: <span class="example">Current files: </span> <span class="exampleLink">Acme.jar</span> <span class="exampleLink">archiver.jar</span> <span class="exampleLink">common_cmdline.jar</span> <span class="exampleLink">ConsensusClustering.pdf</span> <span class="exampleLink">file_support.jar</span> <span class="exampleLink">geneweaver.jar</span> <span class="exampleLink">gp-common.jar</span> <span class="exampleLink">ineq_0.2-2.tar.gz</span>
        <span class="exampleLink">ineq_0.2-2.tgz</span> <span class="exampleLink">jaxb-rt-1.0-ea.jar</span> <span class="exampleLink">my.local.install.r</span> <span class="exampleLink">RunSomAlg.jar</span> <span class="exampleLink">trove.jar</span> <span class="exampleLink">version.txt</span>
        <br>
        <a name="Doc"><h4>Documentation</h4></a>
        Module authors should document their work to help make it more accessible to other users. When you upload
        documentation files, the documentation is made available in conjunction with the module itself. When the module
        is zipped and shared, the documentation will travel with it.  <%=messages.get("ApplicationName")%> doesn't require any specific formats, but
        we suggest that you consider generating your documentation in either PDF or HTML format for maximum portability. In general, the <%=messages.get("ApplicationName")%> server will recognize as documentation files that have standard extensions: .doc, .pdf, .txt, .html, .rdf. You can add to or change these extensions by modifying the files.doc property in the <%=messages.get("ApplicationName")%>/resources/genepattern.properties file.<br><br>

        A good document file for a module gives a detailed description of each input parameter, what the output file format
        and content are, and explains the algorithm sufficiently for the reader to either immediately comprehend it, or
        at least to have a reference to a paper, journal, or book where it is explained.
        <br>
        <br>
        ConsensusClustering example: <span class="exampleLink"><a href="docs/help/ConsensusClustering.pdf">ConsensusClustering.pdf</a></span>


        <hr>
        <h2><a name="inputParameters">Input parameters</a></h2>

        The input parameters section of the form appears perhaps to be the most daunting. And yet there is
        little that is required to make a working module declaration. Each parameter in the command line that comes from a
        user input must have a an entry in this section. Otherwise the clients would know nothing about how to
        prompt the user for input nor could they explain to the user what type of input is expected.<br>

        <h4>Name</h4>
        Each parameter has a name, which can be whatever
        you like, using letters, numbers, and period as a separator character between &quot;words&quot;. It can be of mixed
        upper- and lower-case. The name is used inside &lt;brackets&gt; within the command line to indicate that the
        value of that variable should be substituted at that position within the command line. The name is also used as
        a label within the web and graphical clients to prompt the user for the value for that field. And the name is used as
        a way of identifying which parameter is which for the scripting clients.<br><br>

        When one of the <%=messages.get("ApplicationName")%> clients needs to deal with such a parameter, it will pass along a whole
        file, rather than just the name of the file.<br><br>

        ConsensusClustering examples: <span class="example">kmax</span>, <span class="example">input.filename</span>

        <h4>Description</h4>
        The description field is optional, but is very useful. It allows the module author to provide a more detailed description
        than the name itself. What is the &quot;kmax&quot; parameter used for? Does it interact with any other parameters?
        Do you have any advice about what is a reasonable range of settings for it? The description is displayed by the
        clients when they prompt for input for each field.<br><br>

        ConsensusClustering example: <span class="example">Type of clustering algorithm</span>

        <h4>Choices</h4>

        Some parameters are best represented as a drop-down list of choices. By constraining input to those from the list,
        the user is saved typing and cannot make a mistake by choosing an invalid setting (unless there is a dependency
        on some other parameter). The entry in the choices field is a simple semi-colon delimited set of choices.<br><br>

        ConsensusClustering example: <span class="example">hierarchical;SOM;NMF</span>

        <br><br>
        But what if you want the user to be able to choose &quot;human-readable&quot; choices yet have the program accept the same
        ugly or complicated commands that it always has? No problem. Each choice may have a command-line component (which
        will be on the left) and
        a human-readable component (on the right), separated by an &quot;=&quot;. Here's an example:<br><br>

        <span class="example"><font size="-1">hierarchical=Hierarchical clustering;SOM=Self-organizing map;NMF=Non-negative Matrix Factorization;=nothing specified;3.14159265=pi</font></span>
        <br>

        <form>
            <table>
                <tr>
                    <td valign="top">
                        which would create a drop-down list that looks like this:
                    </td>
                    <td>
                        <select size="5">
                            <option>Hierarchical clustering</option>
                            <option>Self-organizing map</option>
                            <option>Non-negative Matrix Factorization</option>
                            <option>nothing specified</option>
                            <option>pi</option>
                        </select>
                    </td>
                </tr>
            </table>
        </form>
        <br><br>

        <h4>Default value</h4>

        Some parameters should have a default value which will be supplied on the module's command line if no setting
        is supplied by the user when invoking the module. This is not the same as the module's own internal defaults.
        Instead, this allows the <%=messages.get("ApplicationName")%> module declaration author to create a default, even when none exists internally
        within the module.<br><br>

        Default values for parameters that have a choice list must be either blank, or one of the values from the choice list.
        Any other setting will result in an error message. If no default for a choice list is provided, the first entry
        on the list will be the default.<br><br>

        The default value may use substitution variables, just like the rest of the command line. So
        a valid default for an output file might be <span class="example">&lt;input.filename_basename&gt;.foo</span>,
        meaning that the output file will have the same stem as the input.filename parameter, but will have a .foo extension.
        <br><br>

        ConsensusClustering examples: <span class="example">NMF</span>, <span class="example">5</span>, <span class="example">&lt;input.filename_basename&gt;.foo</span>

        <h4>Optional</h4>

        Some parameters are not required on the command line. These parameters, when left blank by the user when the module
        is invoked, result in nothing being added to the command line for that parameter.


        <h4>Prefix when specified</h4>

        Some optional parameters need to have extra text prefixing them on the command line when they are specified.
        For example, you might need to write &quot;-F <i>filename</i>&quot; to pass in a filename. However, if the
        filename is optional, you would not necessarily want to specify &quot;-F&quot;. The solution to this problem is to
        set the prefix to &quot;-F&nbsp;&quot; (note that the ending space is included). If the optional filename is
        provided, then the prefix is added in too.<br><br>

        example: <span class="example">-F&nbsp;</span>

        <h4>Type</h4>

        Declaration of the type of an input parameter allows the client to make a smarter presentation of the input to the
        user. (As of <%=messages.get("ApplicationName")%> 1.2, all parameters are being treated as either text or input file types). Parameter type
        choices are: text, integer, floating point, and input file.

        <br><br>

        <form>
            ConsensusClustering example: <select>
            <option>text</option>
            <option>integer</option>
            <option>floating point</option>
            <option selected>input file</option>
        </select>
        </form>

        <h4>File Format</h4>
        When you select a parameter type of input file, a drop-down list of file formats appears in the file format column. Select the valid file format(s) for this parameter. To select multiple file formats, use CTRL-click. If your module requires an input file format not included in the list, scroll back to the Output Description field and click New to add that format to the list.

        <br>
        <jsp:include page="footer.jsp" />
    </body>
</html>
