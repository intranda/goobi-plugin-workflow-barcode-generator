<?xml version="1.0" encoding="iso-8859-1"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:goobi="http://www.goobi.io/logfile" version="1.1" exclude-result-prefixes="fo">
	<xsl:output method="xml" indent="yes" />
	<xsl:template match="goobi:process">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">

			<!-- general layout -->
			<fo:layout-master-set>
				<fo:simple-page-master master-name="page" page-width="21.0cm" page-height="29.7cm" margin-left="1cm" margin-top="0.5cm"
					margin-right="1cm">
					<fo:region-body />
				</fo:simple-page-master>
			</fo:layout-master-set>

			<!-- run through each item to generate a new page with a barcode -->
			<xsl:for-each select="goobi:item">
				<fo:page-sequence master-reference="page">
					<fo:flow flow-name="xsl-region-body" font-family="opensans, unicode">

						<!-- Institution logos -->
						<fo:block>
							<fo:external-graphic src="logo_italy.png" content-width="190mm" top="0cm"/>
						</fo:block>

						<!-- Separator -->
						<fo:block border-top-width="1pt" border-top-style="solid" border-top-color="#cccccc" margin-top="0pt" />

						<!-- identifier as readable text -->
						<fo:block text-align="center" font-weight="bold" font-size="20pt" margin-top="7cm">
							<xsl:value-of select="." />
						</fo:block>

						<!-- Separator -->
						<fo:block border-top-width="1pt" border-top-style="solid" border-top-color="#cccccc" margin-top="20pt" margin-bottom="20pt" />

						<!-- Barcode generation -->
						<xsl:variable name="barcode" select="." />
						<fo:block text-align="center">
							<fo:instream-foreign-object>
								<barcode:barcode xmlns:barcode="http://barcode4j.krysalis.org/ns" message="{$barcode}">
									<barcode:code128>
										<barcode:module-width>0.80mm</barcode:module-width>
										<barcode:height>60mm</barcode:height>
									</barcode:code128>
								</barcode:barcode>
							</fo:instream-foreign-object>
						</fo:block>

						<!-- Separator -->
						<fo:block border-top-width="1pt" border-top-style="solid" border-top-color="#cccccc" margin-top="8.3cm" margin-bottom="10pt" />

						<!-- Goobi logo -->
						<fo:block-container position="fixed" left="1cm" top="27cm">
							<fo:block>
								<fo:external-graphic src="logo.png" content-width="40mm" />
							</fo:block>
						</fo:block-container>

						<!-- Goobi URL -->
						<fo:block-container position="fixed" left="18cm" top="28.1cm">
							<fo:block font-size="7pt">
								https://goobi.io
							</fo:block>
						</fo:block-container>

					</fo:flow>
				</fo:page-sequence>
			</xsl:for-each>
		</fo:root>
	</xsl:template>
</xsl:stylesheet>
