<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

				
	<xsl:template name="latex-preamble">
	<xsl:text>\documentclass[10pt,a4paper,twoside]{book} % 10pt font size, A4 paper and two-sided margins

%\usepackage[top=3.5cm,bottom=3.5cm,left=3.7cm,right=4.7cm,columnsep=30pt]{geometry} % Document margins and spacings
\usepackage[top=2cm,bottom=2cm,left=2cm,right=2cm,columnsep=30pt]{geometry} % Document margins and spacings

%\usepackage[utf8]{inputenc} % Required for inputting international characters
%\usepackage[T1]{fontenc} % Output font encoding for international characters

%\usepackage{palatino} % Use the Palatino font
\usepackage{fontspec}
\setmainfont{Charis SIL} % bold and italics

\usepackage{graphicx}
\usepackage{microtype} % Improves spacing

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


\setcounter{secnumdepth}{-2} % no numbering of section

\title{}
\author{}

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

\usepackage{expex}
%\DeclareUnicodeCharacter{294}{?}
%\DeclareUnicodeCharacter{200E}{ }
%\usepackage{lmodern}
\usepackage{hyperref}
%----------------------------------------------------------------------------------------
</xsl:text>
	</xsl:template>

	<xsl:template name="latex-startdocument">
	<xsl:text>\begin{document}</xsl:text>
	<xsl:text>\maketitle</xsl:text>
	<xsl:text>\tableofcontents</xsl:text>
	<xsl:text>&#10;</xsl:text>
	</xsl:template>

	<xsl:template name="latex-enddocument">
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

</xsl:stylesheet>