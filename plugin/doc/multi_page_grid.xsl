<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:goobi="http://www.goobi.io/logfile" version="1.1" exclude-result-prefixes="fo">
	<xsl:output method="xml" indent="yes" />
	
	<xsl:template match="goobi:process">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

			<!-- general layout -->
			<fo:layout-master-set>
				<fo:simple-page-master master-name="page" page-width="14.8cm" page-height="21.0cm" margin-left="1cm" margin-top="0.7cm"
					margin-right="1cm">
					<fo:region-body />
				</fo:simple-page-master>
			</fo:layout-master-set>

			<!-- run through each item to generate a new page with a barcode -->
			<xsl:for-each select="goobi:item">
				<xsl:if test="position() = 1 or ((position()-1) mod 12 = 0)">
					<xsl:call-template name="mypage">
						<xsl:with-param name="pageStart" select="position()" />
						<xsl:with-param name="pageEnd" select="position() + 11" />
					</xsl:call-template>
				</xsl:if>
			</xsl:for-each>
		</fo:root>
	</xsl:template>

<xsl:template name="mypage">
	<xsl:param name="pageStart" />
	<xsl:param name="pageEnd" />
	
	<fo:page-sequence master-reference="page">
		<fo:flow flow-name="xsl-region-body" font-family="opensans, unicode">

			<fo:table line-height="13pt" table-layout="fixed">
				<fo:table-column column-width="4.2cm"/>
				<fo:table-column column-width="4.2cm"/>
				<fo:table-column column-width="4.2cm"/>
				<fo:table-body>
					<xsl:for-each select="../goobi:item">
						<xsl:if test="position() ge $pageStart and position() le $pageEnd">
							<xsl:if test="position() = 1 or (position()-1) mod 3 = 0">
								<xsl:call-template name="myrow">
									<xsl:with-param name="rowStart" select="position()" />
									<xsl:with-param name="rowEnd" select="position() + 2" />
								</xsl:call-template>
							</xsl:if>	
						</xsl:if>
					</xsl:for-each>
				</fo:table-body>
			</fo:table>

		</fo:flow>
	</fo:page-sequence>
	
</xsl:template>

<xsl:template name="myrow">
	<xsl:param name="rowStart" />
	<xsl:param name="rowEnd" />

	<fo:table-row>
		<xsl:for-each select="../goobi:item">
			<xsl:if test="position() ge $rowStart and position() le $rowEnd">
				<fo:table-cell height="3cm" border-color="#ccc" border-style="solid" border-width="1pt" padding="10pt" padding-top="20pt">
					<fo:block>
						<!-- Barcode generation -->
						<xsl:variable name="barcode" select="." />
						<fo:block text-align="center">
							<fo:instream-foreign-object>
								<barcode:barcode xmlns:barcode="http://barcode4j.krysalis.org/ns" message="{$barcode}">
									<barcode:code128>
										<barcode:module-width>0.21mm</barcode:module-width>
										<barcode:height>14mm</barcode:height>
									</barcode:code128>
								</barcode:barcode>
							</fo:instream-foreign-object>
						</fo:block>
					</fo:block>
				</fo:table-cell>
			</xsl:if>
		</xsl:for-each>
	</fo:table-row>
	
</xsl:template>


</xsl:stylesheet>
