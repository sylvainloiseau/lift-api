<?xml version="1.0" encoding="utf-8"?>

<!--
 | % Sources :
 | % https://www.overleaf.com/latex/examples/dictionary-template/pdztbwjxrpmz
 | % https://github.com/paolobrasolin/tex-dicISCZ/blob/master/preamble.tex
-->

<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	
<!--
............................................................
! Latex command for styling the dictionary components
............................................................
-->
	<xsl:template name="latex-styling-command">
		<xsl:text>
%\newcommand{\entry}[4]{\textbf{#1}\markboth{#1}{#1}\ {(#2)}\ \textit{#3}\ $\bullet$\ {#4}} % Defines the command to print each word on the page, \markboth{}{} prints the first word on the page in the top left header and the last word in the top right
\newcommand{\entry}[1]{\noindent\textbf{#1}\markboth{#1}{#1}}

\newcommand\latexentrystart{}
\newcommand\latexentryend{

}
\newcommand{\latexsensestart}{\ }
\newcommand\latexsenseend{}

\newcommand\latexsensegramstart{ (}
\newcommand\latexsensegramend{)}
\newcommand{\pos}[1]{\textit{#1}}
\newcommand{\gramtrait}[1]{ #1}
\newcommand{\gramtraitslot}[1]{ #1:}

\newcommand\latexsenseglossstart{}
\newcommand\latexsenseglossend{}
\newcommand{\gloss}[1]{ `lang?: #1'}
\newcommand{\glossen}[1]{ \textit{`#1'}}
\newcommand{\glosstpi}[1]{ (\textit{`#1'})}
\newcommand{\glossgramen}[1]{ \textsc{`#1'}}
\newcommand{\glossgramtpi}[1]{ (\textsc{`#1'})}

\newcommand\latexsensedomainstart{ $\diamond$\ } %\textbardbl
\newcommand\latexsensedomainend{}
\newcommand\latexsensedomainbetween{, }
\newcommand{\semanticdomain}[1]{#1}

\newcommand\latexsensedefinitionstart{ }
\newcommand\latexsensedefinitionend{}
\newcommand\latexsensedefinitionbetween{, }
\newcommand{\definition}[1]{'#1'}

\newcommand\latexvariantstart{ [}
\newcommand\latexvariantend{]}
\newcommand\latexvariantbetween{, }
\newcommand{\variant}[1]{#1}

\newcommand\latexrelationcomponentstart{ \guilsinglleft}
\newcommand\latexrelationcomponentend{\guilsinglright}
\newcommand\latexrelationcomponentbetween{, }
\newcommand{\latexrelationcomponent}[1]{#1}
\newcommand{\latexrelationcomponenttype}[1]{#1}

\newcommand\latexsenserelationstart{ $\rightarrow$}
\newcommand\latexsenserelationend{}
\newcommand\latexsenserelationbetween{, }
\newcommand{\latexsenserelation}[1]{ #1}
\newcommand{\latexsenserelationsensenumber}[1]{ (#1)}
\newcommand{\latexsenserelationtype}[1]{ #1:}
</xsl:text>
	</xsl:template>

<!--
............................................................
! Latex packages
............................................................
-->
	<xsl:template name="latex-package">
		<xsl:text>
%\usepackage[top=3.5cm,bottom=3.5cm,left=3.7cm,right=4.7cm,columnsep=30pt]{geometry} % Document margins and spacings
\usepackage[top=2cm,bottom=2cm,left=2cm,right=2cm,columnsep=30pt]{geometry} % Document margins and spacings

% \newlength\lead \setlength\lead{9.6pt} % = 1.2 * 8pt
% \usepackage{geometry}
% 
% % Basic geometry of document
% \geometry
%   { headsep    =   \lead
%   , textwidth  = 42\lead
%   , textheight = 60\lead
%   , hmarginratio = 2:3
%   , vmarginratio = 2:3
%   , bindingoffset = 0cm
%   , onecolumn
%   }
% 
% % Geometry of the dictionary section
% \newcommand\dictionarygeometry{\newgeometry
%   { textwidth  = 42\lead
%   , textheight = 60\lead
%   , hmarginratio = 2:3
%   , vmarginratio = 3:2
%   , bindingoffset = 0cm
%   , twocolumn
%   }}
% 
%  % Two columns layout
% \setlength\columnsep    {2\lead}
% \setlength\columnseprule{0.4pt}
% 
% % Necessary for baseline alignment
% \topskip=\lead
% \raggedbottom
% \setlength\parskip{0pt} % it's better to avoid glue
% 
% % This value is ENORMOUS but I can't think of a better non-manual solution.
% % Stil, microtype helps a lot to lower it.
% \setlength\emergencystretch{17pt}
% 
% % End of importation

%\usepackage{palatino} % Use the Palatino font
\usepackage{fontspec}
\setmainfont{Charis SIL}

\usepackage{graphicx}
\usepackage{microtype}

\usepackage{pifont} % for number in circle

\usepackage{multicol} % Required for splitting text into multiple columns

\usepackage[bf,sf,center]{titlesec} % Required for modifying section titles - bold, sans-serif, centered

\usepackage{fancyhdr} % Required for modifying headers and footers
\fancyhead[L]{\textsf{\rightmark}} % Top left header
\fancyhead[R]{\textsf{\leftmark}} % Top right header
\renewcommand{\headrulewidth}{1.4pt} % Rule under the header
\fancyfoot[C]{\textbf{\textsf{\thepage}}} % Bottom center footer
\renewcommand{\footrulewidth}{1.4pt} % Rule under the footer
\pagestyle{fancy} % Use the custom headers and footers throughout the document

\usepackage{polyglossia}
%\setotherlanguage{arabic}
% \textenglish, etc.

\setcounter{secnumdepth}{-2} % no numbering of section

\usepackage{expex}
%\DeclareUnicodeCharacter{294}{?}
%\DeclareUnicodeCharacter{200E}{ }
%\usepackage{lmodern}
\usepackage[nottoc]{tocbibind} %include the list of figure in the table of contents.
\usepackage{hyperref}
%----------------------------------------------------------------------------------------
		</xsl:text>
	</xsl:template>

<!--
............................................................
! Here the components of the preamble are glued together.
............................................................
-->
	<xsl:template name="latex-preamble">
	<xsl:text>%&amp;xelatex
\documentclass[9pt,a4paper,twoside, openright]{book}
%extbook
	</xsl:text>

	<!--
	 | Include the list of all packages
	-->
	<xsl:call-template name="latex-package" />

	<!--
	 | Set languages for polyglossia,
	 | The languages are defined in parameters.xsl
	 |
	 | Switching to the analysis language is available through
	 | \text<$flex_name_of_analysis_language>
	 | for instance:
	 | \texten
	<xsl:call-template name="set_languages" />
	-->
	
	<!--
	 | Command defined for styling the dictionary components
	-->
	<xsl:call-template name="latex-styling-command" />

	<!--
	 | Title and author
	-->
	<xsl:text>\title{</xsl:text>
	<xsl:value-of select="$title"/>
	<xsl:text>}</xsl:text>
	<xsl:text>\author{</xsl:text>
	<xsl:value-of select="$author"/>
	<xsl:text>}</xsl:text>
    </xsl:template>

	<xsl:template name="latex-startdocument">
	<xsl:text>\begin{document}</xsl:text>
	<xsl:text>\maketitle</xsl:text>
	<xsl:text>\tableofcontents</xsl:text>
	<xsl:text>&#10;</xsl:text>
	</xsl:template>

	<xsl:template name="latex-enddocument">
		<xsl:if test="$makeListOfFigure">
			<xsl:text>\listoffigures</xsl:text>
		</xsl:if>
		<xsl:text>\end{document}</xsl:text>
	</xsl:template>

	<xsl:template name="latex-startsection">
		<xsl:param name="letter" />
		<xsl:text>&#10;</xsl:text>
		<xsl:text>\subsection{</xsl:text>
		<xsl:value-of select="$letter" />
		<xsl:text>}</xsl:text>
		<xsl:text>\begin{multicols}{2}</xsl:text>		
	</xsl:template>

	<xsl:template name="latex-endsection">
		<xsl:text>\end{multicols}</xsl:text>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>

	<xsl:template name="set_languages">
		<xsl:text>&#10;</xsl:text>
		<xsl:text>\setdefaultlanguage{</xsl:text>
		<xsl:value-of select="'english'"/>
		<xsl:text>}</xsl:text>
		<xsl:text>&#10;</xsl:text>
		<!-- TODO not available before polyglossia v1.46 -->
		<xsl:text>%\setlanguagealias{english}{en}</xsl:text>
		<xsl:text>&#10;</xsl:text>

		<xsl:for-each select="map:keys($languages)"
		xmlns:map="http://www.w3.org/2005/xpath-functions/map">
			<xsl:text>\setotherlanguage{</xsl:text>
			<xsl:value-of select="$languages(.)"/>
			<xsl:text>}</xsl:text>
			<!-- not available before polyglossia v1.46 -->
			<xsl:text>%\setlanguagealias{</xsl:text>
			<xsl:value-of select="$languages(.)"/>
			<xsl:text>}</xsl:text>
			<xsl:text>{</xsl:text>
			<xsl:value-of select="."/>
			<xsl:text>}</xsl:text>
			<xsl:text>&#10;</xsl:text>
		</xsl:for-each>
		
		<!--
		 | <xsl:for-each select="key('languages', 'analysis_language')">
		 |     <xsl:text>\setotherlanguage{</xsl:text>
		 |     <xsl:value-of select="'english'"/>
		 |     <xsl:text>}</xsl:text>
		 |     <xsl:variable name="language_abbreviation">
		 |         <xsl:analyze-string select="." regex="^([^,]+),[^,]+$">
		 |             <xsl:matching-substring>
		 |                 <xsl:value-of select="regex-group(1)"/>
		 |             </xsl:matching-substring>
		 |         </xsl:analyze-string>
		 |     </xsl:variable>
		 |     <xsl:variable name="language_name">
		 |         <xsl:analyze-string select="." regex="^[^,]+,([^,]+)$">
		 |             <xsl:matching-substring>
		 |                 <xsl:value-of select="regex-group(1)"/>
		 |             </xsl:matching-substring>
		 |         </xsl:analyze-string>
		 |     </xsl:variable>
		 |     <xsl:text>\setlanguagealias{</xsl:text>
		 |     <xsl:value-of select="$language_name"/>
		 |     <xsl:text>}</xsl:text>
		 |     <xsl:text>{</xsl:text>
		 |     <xsl:value-of select="$language_abbreviation"/>
		 |     <xsl:text>}</xsl:text>
		 | </xsl:for-each>
		-->
		
		<xsl:text>&#10;</xsl:text>
	</xsl:template>
	
</xsl:stylesheet>