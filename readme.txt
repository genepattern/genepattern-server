adGenePattern directory structure:

/: Top level build.xml, readme, license information

admin: GenePattern website - JSP pages for administering GP modules (adding, editting, getting zip files).

build: Tree where binary output of compilations are stored.  Cleared by ant clean.

clients: Client-side code for invoking GenePattern tasks.  Currently predominantly R package.

common: Source code that is common to multiple modules or that is shared between client and server side usage.

doc: End user installation and usage documentation.

modules: Tree divided by functional area (visualizers, filters, algorithms, etc.) with each module having its own tree for source code, documentation, and support libraries.  Each module also has its own build.xml file to make itself and manifest file to store installation configuration data.
