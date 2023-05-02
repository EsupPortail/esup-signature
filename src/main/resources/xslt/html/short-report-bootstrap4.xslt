<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dss="http://dss.esig.europa.eu/validation/simple-report">

    <xsl:output method="html" encoding="utf-8" indent="yes" omit-xml-declaration="yes" />

    <xsl:template match="/dss:SimpleReport">
        <xsl:apply-templates select="dss:Signature"/>
    </xsl:template>

    <xsl:template match="dss:DocumentName"/>
    <xsl:template match="dss:SignatureFormat"/>
    <xsl:template match="dss:SignaturesCount"/>
    <xsl:template match="dss:ValidSignaturesCount"/>
    <xsl:template match="dss:ValidationTime"/>
    <xsl:template match="dss:ContainerType"/>

    <xsl:template match="dss:Signature">
        <xsl:variable name="indicationText" select="dss:Indication/text()"/>
        <xsl:variable name="idToken" select="@Id" />
        <xsl:variable name="id" select="concat('report_', position())"/>
        <xsl:variable name="nodeName" select="name()" />
        <xsl:variable name="indicationCssClass">
            <xsl:choose>
                <xsl:when test="$indicationText='TOTAL_PASSED'">success</xsl:when>
                <xsl:when test="$indicationText='PASSED'">success</xsl:when>
                <xsl:when test="$indicationText='INDETERMINATE'">warning</xsl:when>
                <xsl:when test="$indicationText='FAILED'">danger</xsl:when>
                <xsl:when test="$indicationText='TOTAL_FAILED'">danger</xsl:when>
            </xsl:choose>
        </xsl:variable>
            <div class="alert alert-info mb-1">
                <xsl:attribute name="id"><xsl:value-of select="$id"/></xsl:attribute>
                <xsl:attribute name="name"><xsl:value-of select="$idToken"/></xsl:attribute>

                <xsl:if test="dss:Filename">
                    <dl>
                        <xsl:attribute name="class">row mb-0</xsl:attribute>
                        <xsl:if test="$nodeName = 'Signature'">
                            <dt>
                                <xsl:attribute name="class">col-sm-5</xsl:attribute>
                                Nom du fichier de signature:
                            </dt>
                        </xsl:if>
                        <xsl:if test="$nodeName = 'Timestamp'">
                            <dt>
                                <xsl:attribute name="class">col-sm-5</xsl:attribute>

                                Timestamp filename:
                            </dt>
                        </xsl:if>
                        <dd>
                            <xsl:attribute name="class">col-sm-7</xsl:attribute>

                            <xsl:value-of select="dss:Filename" />
                        </dd>
                    </dl>
                </xsl:if>

                <xsl:if test="dss:SignatureLevel | dss:TimestampLevel">
                    <dl>
                        <xsl:attribute name="class">row mb-0</xsl:attribute>
                        <dt>
                            <xsl:attribute name="class">col-sm-5</xsl:attribute>
                            Qualification :
                        </dt>
                        <dd>
                            <xsl:attribute name="class">col-sm-7</xsl:attribute>

                            <xsl:if test="dss:SignatureLevel">
                                <xsl:value-of select="dss:SignatureLevel" />
                            </xsl:if>
                            <xsl:if test="dss:TimestampLevel">
                                <xsl:value-of select="dss:TimestampLevel" />
                            </xsl:if>
                            <i>
                                <xsl:attribute name="class">fa fa-info-circle text-info ml-2</xsl:attribute>
                                <xsl:attribute name="data-bs-toggle">tooltip</xsl:attribute>
                                <xsl:attribute name="data-placement">right</xsl:attribute>

                                <xsl:if test="dss:SignatureLevel">
                                    <xsl:attribute name="title"><xsl:value-of select="dss:SignatureLevel/@description" /></xsl:attribute>
                                </xsl:if>
                                <xsl:if test="dss:TimestampLevel">
                                    <xsl:attribute name="title"><xsl:value-of select="dss:TimestampLevel/@description" /></xsl:attribute>
                                </xsl:if>
                            </i>
                        </dd>
                    </dl>
                </xsl:if>

                <xsl:if test="@SignatureFormat">
                    <dl>
                        <xsl:attribute name="class">row mb-0</xsl:attribute>
                        <dt>
                            <xsl:attribute name="class">col-sm-5</xsl:attribute>
                            Format de la signature :
                        </dt>
                        <dd>
                            <xsl:attribute name="class">col-sm-7</xsl:attribute>

                            <xsl:value-of select="@SignatureFormat"/>
                        </dd>
                    </dl>
                </xsl:if>

                <dl>
                    <xsl:attribute name="class">row mb-0</xsl:attribute>
                    <dt>
                        <xsl:attribute name="class">col-sm-5</xsl:attribute>
                        Validité:
                    </dt>
                    <dd>
                        <xsl:attribute name="class">col-sm-7 text-<xsl:value-of select="$indicationCssClass" /></xsl:attribute>

                        <div>
                            <xsl:attribute name="class">badge mr-2 bg-<xsl:value-of select="$indicationCssClass" /></xsl:attribute>

                            <xsl:variable name="dssIndication" select="dss:Indication" />
                            <xsl:variable name="semanticText" select="//dss:Semantic[contains(@Key,$dssIndication)]"/>

                            <xsl:if test="string-length($semanticText) &gt; 0">
                                <xsl:attribute name="data-bs-toggle">tooltip</xsl:attribute>
                                <xsl:attribute name="data-placement">right</xsl:attribute>
                                <xsl:attribute name="title"><xsl:value-of select="$semanticText" /></xsl:attribute>
                            </xsl:if>

                            <xsl:value-of select="$indicationText" />
                        </div>

                        <xsl:choose>
                            <xsl:when test="$indicationText='TOTAL_PASSED'">
                                <i>
                                    <xsl:attribute name="class">fa fa-check-circle align-middle</xsl:attribute>
                                </i>
                            </xsl:when>
                            <xsl:when test="$indicationText='PASSED'">
                                <i>
                                    <xsl:attribute name="class">fa fa-check-circle align-middle</xsl:attribute>
                                </i>
                            </xsl:when>
                            <xsl:when test="$indicationText='INDETERMINATE'">
                                <i>
                                    <xsl:attribute name="class">fa fa-exclamation-circle align-middle</xsl:attribute>
                                </i>
                            </xsl:when>
                            <xsl:when test="$indicationText='FAILED'">
                                <i>
                                    <xsl:attribute name="class">fa fa-times-circle align-middle</xsl:attribute>
                                </i>
                            </xsl:when>
                            <xsl:when test="$indicationText='TOTAL_FAILED'">
                                <i>
                                    <xsl:attribute name="class">fa fa-times-circle align-middle</xsl:attribute>
                                </i>
                            </xsl:when>
                        </xsl:choose>
                    </dd>
                </dl>

                <xsl:apply-templates select="dss:SubIndication">
                    <xsl:with-param name="indicationClass" select="$indicationCssClass"/>
                </xsl:apply-templates>

                <xsl:apply-templates select="dss:AdESValidationDetails" />

                <dl>
                    <xsl:attribute name="class">row mb-0</xsl:attribute>
                    <dt>
                        <xsl:attribute name="class">col-sm-5</xsl:attribute>
                        Chaine de certification:
                    </dt>
                    <xsl:choose>
                        <xsl:when test="dss:CertificateChain">
                            <dd>
                                <xsl:attribute name="class">col-sm-7</xsl:attribute>

                                <ul>
                                    <xsl:attribute name="class">list-unstyled mb-0</xsl:attribute>

                                    <xsl:for-each select="dss:CertificateChain/dss:Certificate">
                                        <xsl:variable name="index" select="position()"/>

                                        <li>
                                            <i><xsl:attribute name="class">fa fa-link mr-2</xsl:attribute></i>
                                            <xsl:choose>
                                                <xsl:when test="$index = 1">
                                                    <b><xsl:value-of select="dss:QualifiedName" /></b>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:value-of select="dss:QualifiedName" />
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </li>
                                    </xsl:for-each>
                                </ul>
                            </dd>
                        </xsl:when>
                        <xsl:otherwise>
                            <dd>/</dd>
                        </xsl:otherwise>
                    </xsl:choose>
                </dl>

                <xsl:if test="dss:SigningTime">
                    <dl>
                        <xsl:attribute name="class">row mb-0</xsl:attribute>
                        <dt>
                            <xsl:attribute name="class">col-sm-5</xsl:attribute>
                            Date de la signature (GMT +00h00):
                        </dt>
                        <dd>
                            <xsl:attribute name="class">col-sm-7</xsl:attribute>
                            <xsl:value-of select="dss:SigningTime"/>
                        </dd>
                    </dl>
                </xsl:if>

                <xsl:if test="dss:ProductionTime">
                    <dl>
                        <xsl:attribute name="class">row mb-0</xsl:attribute>
                        <dt>
                            <xsl:attribute name="class">col-sm-5</xsl:attribute>
                            Production time:
                        </dt>
                        <dd>
                            <xsl:attribute name="class">col-sm-7</xsl:attribute>

                            <xsl:value-of select="dss:ProductionTime"/>
                        </dd>
                    </dl>
                </xsl:if>

                <xsl:if test="$nodeName = 'Signature'">
                    <dl>
                        <xsl:attribute name="class">row mb-0</xsl:attribute>
                        <dt>
                            <xsl:attribute name="class">col-sm-5</xsl:attribute>
                            Position de la signature dans le document:
                        </dt>
                        <dd>
                            <xsl:attribute name="class">col-sm-7</xsl:attribute>

                            <xsl:value-of select="count(preceding-sibling::dss:Signature) + 1"/> sur <xsl:value-of select="count(ancestor::*/dss:Signature)"/>
                        </dd>
                    </dl>
                </xsl:if>

                <xsl:if test="dss:SignatureScope">
                    <xsl:for-each select="dss:SignatureScope">
                        <dl>
                            <xsl:attribute name="class">row mb-0</xsl:attribute>
                            <dt>
                                <xsl:attribute name="class">col-sm-5</xsl:attribute>
                                Périmètre de la signature:
                            </dt>
                            <dd>
                                <xsl:attribute name="class">col-sm-7</xsl:attribute>

                                <xsl:value-of select="@name"/> (<xsl:value-of select="@scope"/>)<br />
                                <xsl:value-of select="."/>
                            </dd>
                        </dl>
                    </xsl:for-each>
                </xsl:if>
            </div>
    </xsl:template>

    <xsl:template match="dss:SubIndication">
        <xsl:param name="indicationClass" />

        <xsl:variable name="subIndicationText" select="." />
        <xsl:variable name="semanticText" select="//dss:Semantic[contains(@Key,$subIndicationText)]"/>

        <dl>
            <xsl:attribute name="class">row mb-0</xsl:attribute>
            <dt>
                <xsl:attribute name="class">col-sm-5</xsl:attribute>

                Information complémentaire :
            </dt>
            <dd>
                <xsl:attribute name="class">col-sm-7</xsl:attribute>
                <div>
                    <xsl:attribute name="class">badge bg-<xsl:value-of select="$indicationClass" /></xsl:attribute>

                    <xsl:if test="string-length($semanticText) &gt; 0">
                        <xsl:attribute name="data-bs-toggle">tooltip</xsl:attribute>
                        <xsl:attribute name="data-placement">right</xsl:attribute>
                        <xsl:attribute name="title"><xsl:value-of select="$semanticText" /></xsl:attribute>
                    </xsl:if>

                    <xsl:value-of select="$subIndicationText" />
                </div>
            </dd>
        </dl>
    </xsl:template>

</xsl:stylesheet>