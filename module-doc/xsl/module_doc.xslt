<?xml version="1.0" encoding="utf-8"?>
<!--
	Author: 
	File: 
	Date: 
	Purpose: 
-->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html" indent="yes" encoding="UTF-8"/>

<xsl:template match="module_doc">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1" />
<title><xsl:value-of select="name" /></title>
	<link href="../css/module_doc.css" rel="stylesheet" type="text/css" />
</head>

  <body>
  <h1><xsl:value-of select="name" /></h1>

<p>
<xsl:value-of select="author" />		
	<br />
	Contact: <xsl:value-of select="contact" />		
</p>

<div class="mdoc_list">

  <div class="mdoc_list_item">
    <div class="mdoc_list_toc">
                   <p> <a href="#description">Description</a> <br />
                   <a href="#interpretation">Interpreting Results</a> <br />
                   <a href="#parameters">Input Parameters</a> <br />
                   <a href="#results">Result files</a> <br />
                   <a href="#dependencies">Platform Dependencies</a> <br />
                   <a href="#references">References</a> <br />
                   <a href="#related">Related Modules</a>
                   </p>
    </div> <!-- close mdoc_list_toc -->
    <div class="mdoc_list_block">
    <div class="steps">
    
<a name="description"><h2>Description</h2></a>
	<xsl:apply-templates select="overview/description" />
</div> <!-- close steps -->
  </div> <!-- close mdoc_list_block -->
</div>  <!-- close mdoc_list_item -->
</div>  <!-- close mdoc_list -->	
	
    <div class="steps">
<a name="interpretation"><h2>Interpreting Results</h2></a>
	<xsl:apply-templates select="interpretation/description" />
</div>

<!-- start input parameters section -->
<xsl:if test="count(input_parameters/parameter) > 0">
    <div class="steps"><a name="parameters"></a>
<xsl:if test="count(input_parameters/parameter[./@type='basic']) > 0">
<h2>Basic Parameters</h2>
<p>Parameters that are essential to the analysis.</p>
<xsl:for-each select="input_parameters/parameter[./@type='basic']">
	<xsl:apply-templates select="." />
</xsl:for-each>
</xsl:if>

<xsl:if test="count(input_parameters/parameter[./@type='advanced']) > 0">
<h2>Advanced Parameters</h2>
<p>Parameters whose default values are sufficient for most analyses.</p>
<xsl:for-each select="input_parameters/parameter[./@type='advanced']">
	<xsl:apply-templates select="." />
</xsl:for-each>
</xsl:if>
</div>
</xsl:if> <!-- end input parameters section -->

<!-- start output files section -->
<xsl:if test="count(output_files) > 0">
    <div class="steps">
<a name="results"><h2>Result Files</h2></a>
<xsl:for-each select="output_files/output_file">
	<xsl:apply-templates select="." />
</xsl:for-each>
</div>
</xsl:if> <!-- end output files section -->

<!-- start dependencies section -->
    <div class="steps">
<a name="dependencies"><h2>Platform Dependencies</h2></a>
	<xsl:apply-templates select="dependencies" />
</div>

<!-- start references section -->
<xsl:if test="count(references) > 0">
    <div class="steps">
<a name="references"><h2>References</h2></a>
<xsl:for-each select="references/reference">
	<xsl:apply-templates select="." />
</xsl:for-each>
</div>
</xsl:if> <!-- end references section -->


  </body>
  </html>
</xsl:template>  
<!-- END OUTPUT -->

<!--
 
	templates section

 -->

<!-- start parameter template -->
<xsl:template match="parameter">

  <h3><xsl:value-of select="@name" /> :   
  <xsl:choose>
  <xsl:when test="@required='yes'">Required</xsl:when>
  <xsl:when test="@required='no'">Optional</xsl:when>
  </xsl:choose>
  </h3>

<xsl:apply-templates select="description" />

<xsl:if test="count(choices) > 0">
<h4>Options</h4>
<ul>
  <xsl:for-each select="choices/choice">      <li><xsl:value-of select="./value" />
      <xsl:if test="count(./description) > 0">
      	: <xsl:apply-templates select="./description" />
      </xsl:if>
      </li>
  </xsl:for-each></ul></xsl:if>

</xsl:template> <!-- end parameter template -->

<!-- start output_file template -->
<xsl:template match="output_file">

  <h3><xsl:value-of select="@name" /> 
  <xsl:if test="@file_format">
  	: <xsl:value-of select="@file_format" />
  </xsl:if>
  </h3>
  
  <xsl:if test="count(./example_file) > 0">
  <p>Example file: 
      <xsl:for-each select="./example_file">			<a href="linked_files/{.}"><xsl:value-of select="." /></a> 
    </xsl:for-each>
  </p>
  </xsl:if>
  
  <xsl:apply-templates select="./description" />

</xsl:template> <!-- end output_file template -->

<!-- start dependencies template -->
<xsl:template match="dependencies">
<ul>
	<li>CPU: <xsl:value-of select="./cpu" /></li>
	<li>Operating system: <xsl:value-of select="./os" /></li>
	<li>Language: <xsl:value-of select="./language" /></li>
	<li>Language version: <xsl:value-of select="./language_version" /></li>
</ul>
</xsl:template> <!-- end dependencies template -->

<!-- start reference template -->
<xsl:template match="reference">
	<p><xsl:value-of select="." /></p>
</xsl:template> <!-- end references template -->



<!-- start description template -->
<xsl:template match="description">

<xsl:value-of select="text" disable-output-escaping="yes"/>

<!-- we would need to auto-generate the thumbnails -->
<xsl:if test="count(./image_file) > 0">
  <table>    <xsl:for-each select="./image_file">      <tr>          <td>
			<a href="images/{.}"><img src="images/small_{.}.png" alt="{.}" /></a>
           </td>      </tr>    </xsl:for-each>  </table></xsl:if>

</xsl:template>    <!-- end description template -->

</xsl:stylesheet>
