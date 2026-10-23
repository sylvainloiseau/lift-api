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
\newcommand{\glossfr}[1]{ \textit{`#1'}}
\newcommand{\glosstpi}[1]{ (\textit{`#1'})}
\newcommand{\glossgramen}[1]{ \textsc{`#1'}}
\newcommand{\glossgramfr}[1]{ \textsc{`#1'}}
\newcommand{\glossgramtpi}[1]{ (\textsc{`#1'})}
% A gloss in a given language: \gloss&lt;lang&gt; when it is defined above, else \gloss.
\newcommand{\glossin}[2]{\ifcsname gloss#1\endcsname\csname gloss#1\endcsname{#2}\else\gloss{#2}\fi}
\newcommand{\glossgramin}[2]{\ifcsname glossgram#1\endcsname\csname glossgram#1\endcsname{#2}\else\gloss{#2}\fi}

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
</xsl:text>
		<xsl:if test="$mainFont != ''">
			<xsl:text>\setmainfont{</xsl:text>
			<xsl:value-of select="$mainFont"/>
			<xsl:text>}&#10;</xsl:text>
		</xsl:if>
		<xsl:text>
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
</xsl:text>
		<xsl:call-template name="set_languages"/>
		<xsl:text>
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
	<xsl:text>% Compile with XeLaTeX or LuaLaTeX (fontspec, polyglossia).
\documentclass[9pt,a4paper,twoside, openright]{book}
%extbook
	</xsl:text>

	<!--
	 | Include the list of all packages
	-->
	<xsl:call-template name="latex-package" />

	<!--
	 | Command defined for styling the dictionary components
	-->
	<xsl:call-template name="latex-styling-command" />

	<!--
	 | Title and author
	-->
	<xsl:text>\title{</xsl:text>
	<xsl:call-template name="escape_for_latex">
		<xsl:with-param name="string" select="$title"/>
	</xsl:call-template>
	<xsl:text>}</xsl:text>
	<xsl:text>\author{</xsl:text>
	<xsl:call-template name="escape_for_latex">
		<xsl:with-param name="string" select="$author"/>
	</xsl:call-template>
	<xsl:text>}</xsl:text>
    </xsl:template>

	<xsl:template name="latex-startdocument">
	<xsl:text>\begin{document}</xsl:text>
	<xsl:text>\maketitle</xsl:text>
	<xsl:text>\tableofcontents</xsl:text>
	<xsl:text>&#10;</xsl:text>
	</xsl:template>

	<xsl:template name="latex-enddocument">
		<xsl:variable name="listOfFigures">
			<xsl:call-template name="is-true">
				<xsl:with-param name="value" select="$makeListOfFigure"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:if test="$listOfFigures = 'true'">
			<xsl:text>\listoffigures</xsl:text>
		</xsl:if>
		<xsl:text>\end{document}</xsl:text>
	</xsl:template>

	<xsl:template name="latex-startsection">
		<xsl:param name="letter" />
		<xsl:text>&#10;</xsl:text>
		<xsl:text>\subsection{</xsl:text>
		<xsl:call-template name="escape_for_latex">
			<xsl:with-param name="string" select="$letter"/>
		</xsl:call-template>
		<xsl:text>}</xsl:text>
		<xsl:text>\begin{multicols}{2}</xsl:text>
	</xsl:template>

	<xsl:template name="latex-endsection">
		<xsl:text>\end{multicols}</xsl:text>
		<xsl:text>&#10;</xsl:text>
	</xsl:template>

<!--
............................................................
! Languages and fonts, from the parameters "languages", "sortLanguage" and "fonts"
............................................................
-->
	<xsl:template name="set_languages">
		<xsl:variable name="default">
			<xsl:call-template name="default-language"/>
		</xsl:variable>
		<xsl:text>\setdefaultlanguage{</xsl:text>
		<xsl:value-of select="$default"/>
		<xsl:text>}&#10;</xsl:text>
		<xsl:call-template name="set-other-languages">
			<xsl:with-param name="pairs" select="$languages"/>
			<xsl:with-param name="default" select="$default"/>
		</xsl:call-template>
		<xsl:call-template name="set-fonts">
			<xsl:with-param name="pairs" select="$fonts"/>
		</xsl:call-template>
	</xsl:template>

	<!-- The polyglossia name of sortLanguage, or english. -->
	<xsl:template name="default-language">
		<xsl:param name="code" select="$sortLanguage"/>
		<xsl:param name="pairs" select="$languages"/>
		<xsl:variable name="name">
			<xsl:call-template name="polyglossia-name">
				<xsl:with-param name="code" select="$code"/>
				<xsl:with-param name="pairs" select="$pairs"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="$name != ''"><xsl:value-of select="$name"/></xsl:when>
			<xsl:otherwise>english</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!--
	 | \setotherlanguage for each name of the code=name pairs: once per name, since two
	 | codes may map to one name, and not for the default language.
	 -->
	<xsl:template name="set-other-languages">
		<xsl:param name="pairs"/>
		<xsl:param name="default"/>
		<xsl:param name="done" select="';'"/>
		<xsl:if test="normalize-space($pairs) != ''">
			<xsl:variable name="pair" select="substring-before(concat($pairs, ';'), ';')"/>
			<xsl:variable name="name" select="normalize-space(substring-after($pair, '='))"/>
			<xsl:variable name="new" select="$name != '' and $name != $default
				and not(contains($done, concat(';', $name, ';')))"/>
			<xsl:if test="$new">
				<xsl:text>\setotherlanguage{</xsl:text>
				<xsl:value-of select="$name"/>
				<xsl:text>}&#10;</xsl:text>
			</xsl:if>
			<xsl:call-template name="set-other-languages">
				<xsl:with-param name="pairs" select="substring-after($pairs, ';')"/>
				<xsl:with-param name="default" select="$default"/>
				<xsl:with-param name="done">
					<xsl:choose>
						<xsl:when test="$new"><xsl:value-of select="concat($done, $name, ';')"/></xsl:when>
						<xsl:otherwise><xsl:value-of select="$done"/></xsl:otherwise>
					</xsl:choose>
				</xsl:with-param>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

	<!-- \newfontfamily\<name>font{<font>} for each name=font pair. -->
	<xsl:template name="set-fonts">
		<xsl:param name="pairs"/>
		<xsl:if test="normalize-space($pairs) != ''">
			<xsl:variable name="pair" select="substring-before(concat($pairs, ';'), ';')"/>
			<xsl:variable name="name" select="normalize-space(substring-before($pair, '='))"/>
			<xsl:variable name="font" select="normalize-space(substring-after($pair, '='))"/>
			<xsl:if test="$name != '' and $font != ''">
				<xsl:text>\newfontfamily\</xsl:text>
				<xsl:value-of select="$name"/>
				<xsl:text>font{</xsl:text>
				<xsl:value-of select="$font"/>
				<xsl:text>}&#10;</xsl:text>
			</xsl:if>
			<xsl:call-template name="set-fonts">
				<xsl:with-param name="pairs" select="substring-after($pairs, ';')"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

	<!-- The polyglossia name of a language code, from the parameter "languages", or ''. -->
	<xsl:template name="polyglossia-name">
		<xsl:param name="code"/>
		<xsl:param name="pairs" select="$languages"/>
		<xsl:if test="$code != '' and normalize-space($pairs) != ''">
			<xsl:variable name="pair" select="substring-before(concat($pairs, ';'), ';')"/>
			<xsl:choose>
				<xsl:when test="normalize-space(substring-before($pair, '=')) = $code">
					<xsl:value-of select="normalize-space(substring-after($pair, '='))"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name="polyglossia-name">
						<xsl:with-param name="code" select="$code"/>
						<xsl:with-param name="pairs" select="substring-after($pairs, ';')"/>
					</xsl:call-template>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
