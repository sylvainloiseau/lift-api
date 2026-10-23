<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:variable name="title">Saint-Agnant-de-Versillat variety: lexicon</xsl:variable>
	<xsl:variable name="author"></xsl:variable>

	<!--
	 | Switch between true() and false() to include or exclude the corresponding material:
	 -->
	<xsl:variable name="makeThematicDictionary" select="true()" />
	<xsl:variable name="makeListOfFigure" select="false()" />
	<xsl:variable name="makeFigure" select="false()" />

	<xsl:variable name="urlillustration" select="'/Users/sloiseau/Recherche/Corpus/Corpus/tuwari/20190507/export/pictures/'"/>

	<!--
	 | The latex "polyglossia" package defined typographic rules for some
	 | frequent languages. If some of the language used in the Lift
	 | document are defined in polyglossia, you can benefit from this
	 | definition to have it typesetted correctly. See
	 |	https://ctan.org/pkg/polyglossia/doc/polyglossia.pdf
	 |.  for a list of available languages
	 |
	 | In order to do so, for each language, you have to associate
	 | the polyglossia name with the abbreviation of the language used in
	 | the Lift document:
	 |       'lift abbreviation' : 'polyglossia',

	 | For instance, for english, considering that
	 | the Lift document used the "en" abbreviation, and that polygossia
     | use the string 'english' you may enter:
     |
	 |       'en' : 'english',
	-->
	<xsl:variable name="languages" select="
	map {
		'fr' : 'french',
		'tpi' : 'french',
		'tww' : 'tuwari',
        'en' : 'english'
	}
        "/>

<!--
\foreignlanguage{language}{text}
\usepackage[french,british]{babel}
-->

	<!--
	 |     <entry key="analysis_language">en,english</entry>
	 |     <entry key="analysis_language">fr,french</entry>
	 |     <entry key="analysis_language">tpi,french</entry>
	 |     <entry key="vernacular_language">tww,tuwari</entry>
	 | </xsl:variable>
	-->


</xsl:stylesheet>
