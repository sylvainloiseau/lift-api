<xsl:stylesheet
	version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:import href="style.xsl"/>

	<xsl:output encoding="UTF-8" method="text"
	/>

	<xsl:variable name="urlillustration" select="'/Users/sloiseau/Recherche/Corpus/Corpus/tuwari/20190507/export/pictures/'"/>
	
	
	<xsl:template match="/">
	<xsl:call-template name="latex-preamble" />	
	<xsl:call-template name="latex-startdocument" />
	<xsl:text>
	\section{Alphabetic dictionary}
	</xsl:text>
	<xsl:for-each-group select="/lift/entry" group-by="if (starts-with(lexical-unit/form/text, '-')) then substring(lexical-unit/form/text, 2, 1) else substring(lexical-unit/form/text, 1, 1)">
	<xsl:sort select="if (starts-with(lexical-unit/form/text, '-')) then substring(lexical-unit/form/text, 2, 1) else substring(lexical-unit/form/text, 1, 1)" />
	<xsl:call-template name="latex-startsection">
	<xsl:with-param name="letter" select="if (starts-with(lexical-unit/form/text, '-')) then substring(lexical-unit/form/text, 2, 1) else substring(lexical-unit/form/text, 1, 1)" />
	</xsl:call-template>
	<xsl:for-each select="current-group()">
	<xsl:sort select="if (starts-with(lexical-unit/form/text, '-')) then substring(lexical-unit/form/text, 2) else substring(lexical-unit/form/text, 1)" />
	<xsl:text>\label{</xsl:text><xsl:value-of select="./@id" /><xsl:text>}</xsl:text>
	<xsl:apply-templates select="."/>
	</xsl:for-each>
	<xsl:call-template name="latex-endsection"/>
	
    <xsl:for-each select="current-group()//sense[illustration]">
    <xsl:call-template name="make_illustration">
    <xsl:with-param name="sense" select="." />
    </xsl:call-template>    
    </xsl:for-each>

	</xsl:for-each-group>	

	<xsl:text>
	\section{Thematic dictionary}
	</xsl:text>

	<xsl:for-each-group select="/lift/entry//sense/trait[@name='semantic-domain-ddp4']" group-by="@value">
	<xsl:sort select="@value" />
	<xsl:call-template name="latex-startsection">
	<xsl:with-param name="letter" select="@value" />
	</xsl:call-template>
	<xsl:for-each select="current-group()">
	<xsl:sort select="if (starts-with(ancestor::entry/lexical-unit/form/text, '-')) then substring(ancestor::entry/lexical-unit/form/text, 2) else substring(ancestor::entry/lexical-unit/form/text, 1)" />
	<xsl:apply-templates select="ancestor::entry" />
	</xsl:for-each>
	<xsl:call-template name="latex-endsection"/>
	</xsl:for-each-group>	

	<xsl:call-template name="latex-enddocument" />
	</xsl:template>

<!--
............................................................
!
............................................................
-->
	<xsl:template match="entry">
	<xsl:text>\latexentrystart</xsl:text>
	<xsl:call-template name="make_headword">
	<xsl:with-param name="entry" select="." />
	</xsl:call-template>
	
	<xsl:if test="variant">
	<xsl:text>\latexvariantstart</xsl:text>
	<xsl:for-each select="variant">
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'variant'" />
	<xsl:with-param name="content" select="./form" />
	</xsl:call-template>
	<xsl:if test="position() != last()">
	<xsl:text>\latexvariantbetween</xsl:text>
	</xsl:if>
	</xsl:for-each>
	<xsl:text>\latexvariantend</xsl:text>
	</xsl:if>

	<xsl:apply-templates select="sense" />

   	<xsl:if test="relation[@type='_component-lexeme' and @ref]">
	<xsl:text>\latexrelationcomponentstart</xsl:text>
	<xsl:for-each select="relation[@type='_component-lexeme']">
	<xsl:if test="./trait[@name = 'complex-form-type']">
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'latexrelationcomponenttype'" />
<!--	il peut y en avoir d'autres ??? -->
	<xsl:with-param name="content" select="./trait[@name ='complex-form-type'][1]/@value"/>
	</xsl:call-template>	
	</xsl:if>
	<xsl:variable name="ref" select="@ref"/>
	<!--
	 | <xsl:message>
	 | <xsl:value-of select="/lift/entry[@id = $ref]/lexical-unit/form/text/text()" />
	 | </xsl:message>
	-->
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'latexrelationcomponent'" />
	<xsl:with-param name="content">
	<xsl:call-template name="make_headword_string">
	<xsl:with-param name="entry" select="/lift/entry[@id = $ref]" />
	</xsl:call-template>
	</xsl:with-param>
	</xsl:call-template>
	<xsl:if test="position() != last()">
	<xsl:text>\latexrelationcomponentbetween</xsl:text>
	</xsl:if>
	</xsl:for-each>
	<xsl:text>\latexrelationcomponentend</xsl:text>
	</xsl:if>

	<xsl:text>\latexentryend</xsl:text>
	<xsl:text>&#10;</xsl:text>
	</xsl:template>
	
<!--
............................................................
!
............................................................
-->
	<xsl:template match="sense">
	<xsl:text>\latexsensestart</xsl:text>

	<xsl:if test="preceding-sibling::sense or following-sibling::sense">
	<xsl:call-template name="make_sense_number">
	<xsl:with-param name="sense" select="."/>
	</xsl:call-template>	
	</xsl:if>
	
	<xsl:if test="./grammatical-info/@value">
	<xsl:call-template name="make_sense_gram">
	<xsl:with-param name="sense" select="." />
	</xsl:call-template>
	</xsl:if>

	<xsl:if test="definition">
	<xsl:text>\latexsensedefinitionstart</xsl:text>
	<xsl:for-each select="definition">
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'definition'" />
	<xsl:with-param name="content" select="./form/text" />
	</xsl:call-template>
	<xsl:if test="position() != last()">
	<xsl:text>\latexsensedefinitionbetween</xsl:text>
	</xsl:if>
	</xsl:for-each>
	<xsl:text>\latexsensedefinitionend</xsl:text>
	</xsl:if>

	<xsl:if test="./gloss">
	<xsl:text>\latexsenseglossstart</xsl:text>
	<xsl:apply-templates select="gloss" />
	<xsl:text>\latexsenseglossend</xsl:text>
	</xsl:if>
	
	<xsl:if test="relation[@type]">
	<xsl:text>\latexsenserelationstart</xsl:text>
	<xsl:for-each-group select="relation[@type]" group-by="@type">
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'latexsenserelationtype'" />
	<xsl:with-param name="content" select="@type" />
	</xsl:call-template>
	<xsl:for-each select="current-group()">
	<xsl:variable name="ref" select="@ref"/>
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'latexsenserelation'" />
	<xsl:with-param name="content">
	<xsl:call-template name="make_headword_string">
	<xsl:with-param name="entry" select="/lift/entry/sense[@id = $ref]/parent::entry" />
	</xsl:call-template>
	</xsl:with-param>
	</xsl:call-template>
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'latexsenserelationsensenumber'" />
	<xsl:with-param name="content">
	<xsl:call-template name="make_sense_number_string">
	<xsl:with-param name="sense" select="/lift/entry/sense[@id = $ref]" />
	</xsl:call-template>
	</xsl:with-param>
	</xsl:call-template>
	</xsl:for-each>
	<xsl:if test="position() != last()">
	<xsl:text>\latexsenserelationbetween</xsl:text>
	</xsl:if>
	</xsl:for-each-group>	
	<xsl:text>\latexsenserelationend</xsl:text>
	</xsl:if>

	<xsl:if test="trait[@name='semantic-domain-ddp4']">
	<xsl:text>\latexsensedomainstart</xsl:text>
	<xsl:for-each select="trait[@name='semantic-domain-ddp4']">
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'semanticdomain'" />
	<xsl:with-param name="content" select="./@value" />
	</xsl:call-template>
	<xsl:if test="position() != last()">
	<xsl:text>\latexsensedomainbetween</xsl:text>
	</xsl:if>
	</xsl:for-each>
	<xsl:text>\latexsensedomainend</xsl:text>
	</xsl:if>

	<xsl:if test="illustration">
	<xsl:text>\ $\bullet$\ See figure </xsl:text>
	<xsl:variable name="illustrationlabel" select="concat('illustration:', ./ancestor::entry/lexical-unit/form/text, ./ancestor::entry/@order, position())" />
	<xsl:for-each select="illustration">
		<xsl:text> \ref{</xsl:text>
		<xsl:value-of select="$illustrationlabel" />
		<xsl:text>}</xsl:text>
		<xsl:text> p. \pageref{</xsl:text>
		<xsl:value-of select="$illustrationlabel" />
		<xsl:text>}</xsl:text>
	<xsl:if test="position() != last()">
	<xsl:text>, </xsl:text>
	</xsl:if>
	</xsl:for-each>
	</xsl:if>

	<!--
	 | <xsl:call-template name="make_illustration">
	 | <xsl:with-param name="sense" select="." />
	 | </xsl:call-template>    
	-->

	<xsl:text>\latexsenseend</xsl:text>
	</xsl:template>

<!--
............................................................
!
............................................................
-->
	<xsl:template match="gloss">
	<!--
	Tester si gloses faites uniquement de maj
	 | COMPL
	-->
	<xsl:choose>
	<xsl:when test="matches(./text, '^[-_A-Z.]+$')">
		<xsl:call-template name="make_command_with_string">
		<xsl:with-param name="command" select="concat('glossgram', 
		./@lang)" />
		<xsl:with-param name="content" select="lower-case(./text)" />
		</xsl:call-template>	
	</xsl:when>
	<xsl:otherwise>
		<xsl:call-template name="make_command_with_string">
		<xsl:with-param name="command" select="concat('gloss', 
		./@lang)" />
		<xsl:with-param name="content" select="./text" />
		</xsl:call-template>	
	</xsl:otherwise>
	</xsl:choose>
	</xsl:template>
	
<!--
............................................................
!
............................................................
-->
	<xsl:template name="make_sense_number">
	<xsl:param name="sense" />
	<xsl:if test="$sense/parent::sense">
	<xsl:call-template name="make_sense_number">
	<xsl:with-param name="sense" select="$sense/parent::sense"/>
	</xsl:call-template>
	<xsl:text>.</xsl:text>
	</xsl:if>
	<xsl:value-of select="concat('\ding{', 181 + 1 + count($sense/preceding-sibling::sense), '}')"/>
	</xsl:template>

<!--
............................................................
!
............................................................
-->
	<xsl:template name="make_sense_number_string">
	<xsl:param name="sense" />
	<xsl:if test="$sense/parent::sense">
	<xsl:call-template name="make_sense_number_string">
	<xsl:with-param name="sense" select="$sense/parent::sense"/>
	</xsl:call-template>
	<xsl:text>.</xsl:text>
	</xsl:if>
	<xsl:value-of select="1 + count($sense/preceding-sibling::sense)"/>
	</xsl:template>

	<!--
............................................................
!
............................................................
 | <grammatical-info value="Noun">
 | <trait name="type" value="inflAffix"/>
 | <trait name="Noun-slot" value="NounGenderNumber"/></grammatical-info>
-->
	<xsl:template name="make_sense_gram">
	<xsl:param name="sense" />

		<xsl:text>\latexsensegramstart </xsl:text>
		
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'pos'" />
	<xsl:with-param name="content">
	<xsl:call-template name="abbreviate_pos">
	<xsl:with-param name="pos" select="./grammatical-info/@value" />
	</xsl:call-template>
	</xsl:with-param> 
	</xsl:call-template>
	
	<xsl:if test="./grammatical-info/trait[not(@name='type')]">
	<xsl:for-each select="./grammatical-info/trait[not(@name='type')]">

	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'gramtraitslot'" />
	<xsl:with-param name="content">
	<xsl:call-template name="abbreviate_slot">
	<xsl:with-param name="slot" select="./@name" />
	</xsl:call-template>
	</xsl:with-param>    
	</xsl:call-template>
	
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'gramtrait'" />
	<xsl:with-param name="content">
	<xsl:call-template name="abbreviate_trait">
	<xsl:with-param name="trait" select="./@value" />
	</xsl:call-template>
	</xsl:with-param> 	
<!--	<xsl:with-param name="content" select="./@value" />-->
	</xsl:call-template>
	<xsl:if test="position() != last()">
	<xsl:text>, </xsl:text>
	</xsl:if>
	</xsl:for-each>
	</xsl:if>
	
		<xsl:text>\latexsensegramend </xsl:text>
	</xsl:template>

	
	
<!--
............................................................
!
............................................................
-->
<xsl:template name="make_illustration">
	<xsl:param name="sense" />
		<xsl:if test="illustration">
	<xsl:for-each select="illustration">
	<xsl:text>\begin{figure}[tbp]
	\centering
	\includegraphics[width=0.3\linewidth]{</xsl:text>
	<xsl:value-of select="concat($urlillustration, ./@href)" />
	<xsl:text>}
	\caption{</xsl:text>
	<xsl:value-of select="ancestor::entry/lexical-unit/form/text" />
	<xsl:text>, see p. \pageref{</xsl:text><xsl:value-of select="ancestor::entry/@id" />
	<xsl:text>}}
	\label{</xsl:text><xsl:value-of select="concat('illustration:', $sense/ancestor::entry/lexical-unit/form/text, $sense/ancestor::entry/@order, position())" />
	<xsl:text>}
\end{figure}
</xsl:text>
	</xsl:for-each>
	</xsl:if>
	</xsl:template>
	
	
<!--
............................................................
!
............................................................
-->
	<xsl:template name="make_headword">
	<xsl:param name="entry" />
	<xsl:text>\entry{</xsl:text>
	<xsl:call-template name="make_headword_string">
	<xsl:with-param name="entry" select="$entry" />
	</xsl:call-template>
	<xsl:text>}</xsl:text>
	</xsl:template>

<!--
............................................................
!
............................................................
-->
	<xsl:template name="make_headword_string">
	<xsl:param name="entry" />
	<xsl:variable name="entrystring">
	<xsl:call-template name="escape_for_latex">
		<xsl:with-param name="string" select="$entry/lexical-unit/form/text/text()" />	
	</xsl:call-template>
	</xsl:variable>
	<xsl:choose>
	<xsl:when test="$entry/@order">
	<xsl:value-of select="concat($entrystring, '$_{', ./@order), '}$'"/>
	</xsl:when>
	<xsl:otherwise>
	<xsl:value-of select="$entrystring"/>
	</xsl:otherwise>
	</xsl:choose>
	</xsl:template>

<!--
............................................................
!
............................................................
-->
	<xsl:template name="make_command_with_string">
	<xsl:param name="command" />
	<xsl:param name="content" />
	<xsl:text>\</xsl:text>
	<xsl:value-of select="$command" />
	<xsl:text>{</xsl:text>
	<xsl:call-template name="escape_for_latex">
	<xsl:with-param name="string" select="$content" />	
	</xsl:call-template>
	<xsl:text>}</xsl:text>
	</xsl:template>

<!--
............................................................
!
............................................................
-->
	<xsl:template name="make_command_with_element">
	<xsl:param name="command" />
	<xsl:param name="element" />
	</xsl:template>

<!--
............................................................
!
............................................................
-->
	<xsl:template name="make_environement_with_string">
	<xsl:param name="environement" />
	<xsl:param name="string" />
	</xsl:template>
	
<!--
............................................................
!
............................................................
-->
	<xsl:template name="make_environement_with_element">
	<xsl:param name="environement" />
	<xsl:param name="element" />
	</xsl:template>

<!--
............................................................
!
............................................................
-->
	<xsl:template name="escape_for_latex">
	<xsl:param name="string" />
	<xsl:value-of select="replace($string, '_', '\\_')"/>	
	</xsl:template>

	
<!--
............................................................
!
............................................................
-->
	<xsl:template name="abbreviate_pos">
	<xsl:param name="pos" />
	<xsl:choose>
	
	<xsl:when test="$pos='Noun'">
	<xsl:text>n</xsl:text>
	</xsl:when>

		<xsl:when test="$pos='Verb'">
	<xsl:text>v</xsl:text>
	</xsl:when>

		<xsl:when test="$pos='Adjective'">
	<xsl:text>adj</xsl:text>
	</xsl:when>

		<xsl:when test="$pos='Adverb'">
	<xsl:text>adv</xsl:text>
	</xsl:when>

		<xsl:when test="$pos='Possessive Pronoun'">
	<xsl:text>poss</xsl:text>
	</xsl:when>

		<xsl:when test="$pos='Cardinal number'">
	<xsl:text>card.</xsl:text>
	</xsl:when>

		<xsl:when test="$pos='n_Npr'">
	<xsl:text>Npr</xsl:text>
	</xsl:when>
	
		<xsl:when test="$pos=''">
	<xsl:text></xsl:text>
	</xsl:when>

			<xsl:when test="$pos=''">
	<xsl:text></xsl:text>
	</xsl:when>


			<xsl:when test="$pos='Personal pronoun'">
	<xsl:text>pers. pro.</xsl:text>
	</xsl:when>

	<xsl:when test="$pos='Interrogative pro-form'">
	<xsl:text>inter. pro.</xsl:text>
	</xsl:when>
	
	<xsl:when test="$pos='Pronoun'">
	<xsl:text>pro.</xsl:text>
	</xsl:when>

	<xsl:when test="$pos='Interjection'">
	<xsl:text>interj.</xsl:text>
	</xsl:when>

	
	<xsl:otherwise>
				<xsl:value-of select="$pos"/>
				
	</xsl:otherwise>

	</xsl:choose>
	</xsl:template>

<!--
............................................................
!
............................................................
-->
		<xsl:template name="abbreviate_trait">
	<xsl:param name="trait" />
	<xsl:choose>
	
	<xsl:when test="$trait='Noun'">
	<xsl:text>n</xsl:text>
	</xsl:when>
	<xsl:otherwise>
				<xsl:value-of select="$trait"/>
				
	</xsl:otherwise>

	</xsl:choose>
	</xsl:template>

<!--
............................................................
!
............................................................
-->
		<xsl:template name="abbreviate_slot">
	<xsl:param name="slot" />
	<xsl:choose>

	<xsl:when test="$slot='Verb-slot'">
	<xsl:text>slot</xsl:text>
	</xsl:when>

	<xsl:when test="$slot='inflection- feature'">
	<xsl:text>infl. feat.</xsl:text>
	</xsl:when>

	<xsl:when test="$slot='Noun-infl-class'">
	<xsl:text>cl.</xsl:text>
	</xsl:when>
	<xsl:otherwise>
				<xsl:value-of select="$slot"/>
				
	</xsl:otherwise>

	</xsl:choose>
	</xsl:template>

</xsl:stylesheet>
