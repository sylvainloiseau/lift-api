<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
				
	<xsl:variable name="title">Tuwari list of glosses</xsl:variable>
	<xsl:variable name="author"></xsl:variable>

    
	<!--
	 | The latex "polyglossia" package defined typographic rules for some 
	 | frequent languages. If some of the language used in the Lift 
	 | document are defined in polyglossia, you can benefit from this 
	 | definition to have it typesetted correctly. See 
	 |   http://tug.ctan.org/macros/xetex/latex/polyglossia/polyglossia.pdf
	 |.  for a list of available languages
	 |
	 | In order to do so, for each language, you have to associate
	 | the polyglossia name with the abbreviation of the language used in 
	 | the Lift document. For instance, for english, and considering that
	 | the Lift document used the "en" abbreviation, you may enter:
	 |
	 | english is automatically loaded and should be declared
	 |       'en' : 'english', 
	-->
	<xsl:variable name="languages" select="
	map {
      'fr' : 'french',
      'tpi' : 'french',
      'tww' : 'tuwari'
	  }
	"/>
	<!--
	 |     <entry key="analysis_language">en,english</entry>
	 |     <entry key="analysis_language">fr,french</entry>
	 |     <entry key="analysis_language">tpi,french</entry>
	 |     <entry key="vernacular_language">tww,tuwari</entry>
	 | </xsl:variable>
	-->
	
	<!--
     | Switch between anything and the empty string to include or exclude the corresponding material:
	 | For instance, the following will include a thematic dictionary:
	 | <xsl:variable name="makeThematicDictionary" select="'1'" />
	 | While the following will exclude it:
	 | <xsl:variable name="makeThematicDictionary" select="''" />
     -->
	<xsl:variable name="makeThematicDictionary" select="True" />
	<xsl:variable name="makeListOfFigure" select="'1'" />
	
</xsl:stylesheet>