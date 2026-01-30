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
        <xsl:variable name="indicationText" select="dss:Indication"/>
        <xsl:variable name="idToken" select="@Id"/>
        <xsl:variable name="id" select="concat('report_', position())"/>
        <xsl:variable name="nodeName" select="name()"/>
        <xsl:variable name="indicationCssClass">
            <xsl:choose>
                <xsl:when test="$indicationText='TOTAL_PASSED'">success</xsl:when>
                <xsl:when test="$indicationText='PASSED'">success</xsl:when>
                <xsl:when test="$indicationText='INDETERMINATE'">warning</xsl:when>
                <xsl:when test="$indicationText='FAILED'">danger</xsl:when>
                <xsl:when test="$indicationText='TOTAL_FAILED'">danger</xsl:when>
            </xsl:choose>
        </xsl:variable>

        <xsl:attribute name="id"><xsl:value-of select="$id"/></xsl:attribute>
        <xsl:attribute name="name"><xsl:value-of select="$idToken"/></xsl:attribute>

        <!-- Filename -->
        <xsl:if test="dss:Filename">
            <dl class="row mb-0">
                <dt class="col-sm-5">
                    <xsl:choose>
                        <xsl:when test="$nodeName = 'Signature'">Nom du fichier de signature:</xsl:when>
                        <xsl:when test="$nodeName = 'Timestamp'">Timestamp filename:</xsl:when>
                    </xsl:choose>
                </dt>
                <dd class="col-sm-7">
                    <xsl:value-of select="dss:Filename"/>
                </dd>
            </dl>
        </xsl:if>

        <!-- Qualification -->
<!--        <xsl:if test="dss:SignatureLevel | dss:TimestampLevel">-->
<!--            <dl class="row mb-0">-->
<!--                <dt class="col-sm-5">Qualification :</dt>-->
<!--                <dd class="col-sm-7">-->
<!--                    <xsl:if test="dss:SignatureLevel"><xsl:value-of select="dss:SignatureLevel"/></xsl:if>-->
<!--                    <xsl:if test="dss:TimestampLevel"><xsl:value-of select="dss:TimestampLevel"/></xsl:if>-->
<!--                    <i class="fa fa-info-circle text-info ml-2" data-bs-toggle="tooltip" data-placement="right"-->
<!--                       title="{dss:SignatureLevel/@description | dss:TimestampLevel/@description}"/>-->
<!--                </dd>-->
<!--            </dl>-->
<!--        </xsl:if>-->


        <!-- Signing Time -->
        <xsl:if test="dss:SigningTime">
            <dl class="row mb-0">
                <dt class="col-sm-5">Date de la signature (GMT +00h00):</dt>
                <dd class="col-sm-7"><xsl:value-of select="dss:SigningTime"/></dd>
            </dl>
        </xsl:if>


        <!-- Certificate Chain -->
        <dl class="row mb-0">
            <dt class="col-sm-5">Chaine de certification:</dt>
            <dd class="col-sm-7">
                <xsl:choose>
                    <xsl:when test="dss:CertificateChain">
                        <ul class="list-unstyled mb-0">
                            <xsl:for-each select="dss:CertificateChain/dss:Certificate">
                                <li>
                                    <i class="fa fa-link mr-2"/>
                                    <xsl:choose>
                                        <xsl:when test="position()=1"><b><xsl:value-of select="dss:QualifiedName"/></b></xsl:when>
                                        <xsl:otherwise><xsl:value-of select="dss:QualifiedName"/></xsl:otherwise>
                                    </xsl:choose>
                                </li>
                            </xsl:for-each>
                        </ul>
                    </xsl:when>
                    <xsl:otherwise>/</xsl:otherwise>
                </xsl:choose>
            </dd>
        </dl>
        <!-- Validity -->
        <dl class="row mb-0">
            <dt class="col-sm-5">Validité:</dt>
            <dd class="col-sm-7">
                <div class="mr-2">
                    <xsl:choose>
                        <xsl:when test="$indicationText='TOTAL_PASSED' or $indicationText='PASSED'">
                            <i class="fa fa-check-circle align-middle text-success"/>
                        </xsl:when>
                        <xsl:when test="$indicationText='INDETERMINATE'">
                            <i class="fa fa-exclamation-circle align-middle text-warning"/>
                        </xsl:when>
                        <xsl:when test="$indicationText='FAILED' or $indicationText='TOTAL_FAILED'">
                            <i class="fa fa-times-circle align-middle text-danger"/>
                        </xsl:when>
                    </xsl:choose>
                     
                    <xsl:variable name="semanticText" select="//dss:Semantic[@Key = $indicationText]"/>
                    <xsl:if test="string-length($semanticText) &gt; 0">
                        <xsl:attribute name="data-bs-toggle">tooltip</xsl:attribute>
                        <xsl:attribute name="data-placement">right</xsl:attribute>
                        <xsl:attribute name="title"><xsl:value-of select="$semanticText"/></xsl:attribute>
                    </xsl:if>
                    <xsl:value-of select="$semanticText"/>
                </div>
            </dd>
        </dl>

        <!-- SubIndication -->
        <xsl:apply-templates select="dss:SubIndication">
            <xsl:with-param name="indicationClass" select="$indicationCssClass"/>
        </xsl:apply-templates>

        <!-- AdESValidationDetails -->
        <xsl:if test="dss:AdESValidationDetails">
            <dl class="row mb-0">
                <dt class="col-sm-5"></dt>
                <dd class="col-sm-7">
                    <xsl:value-of select="dss:AdESValidationDetails"/>
                </dd>
            </dl>
        </xsl:if>

        <!-- Signature Format -->
        <xsl:if test="@SignatureFormat">
            <dl class="row mb-0">
                <dt class="col-sm-5">Format de la signature :</dt>
                <dd class="col-sm-7"><xsl:value-of select="@SignatureFormat"/></dd>
            </dl>
        </xsl:if>

        <!-- Production Time -->
        <xsl:if test="dss:ProductionTime">
            <dl class="row mb-0">
                <dt class="col-sm-5">Production time:</dt>
                <dd class="col-sm-7"><xsl:value-of select="dss:ProductionTime"/></dd>
            </dl>
        </xsl:if>

        <!-- Signature Position -->
        <xsl:if test="$nodeName='Signature'">
            <dl class="row mb-0">
                <dt class="col-sm-5">Position de la signature dans le document:</dt>
                <dd class="col-sm-7">
                    <xsl:value-of select="count(preceding-sibling::dss:Signature)+1"/> sur
                    <xsl:value-of select="count(ancestor::*/dss:Signature)"/>
                </dd>
            </dl>
        </xsl:if>

<!--        &lt;!&ndash; Signature Scope &ndash;&gt;-->
<!--        <xsl:if test="dss:SignatureScope">-->
<!--            <xsl:for-each select="dss:SignatureScope">-->
<!--                <dl class="row mb-0">-->
<!--                    <dt class="col-sm-5">Périmètre de la signature:</dt>-->
<!--                    <dd class="col-sm-7">-->
<!--                        <xsl:value-of select="@name"/> (<xsl:value-of select="@scope"/>)<br/>-->
<!--                        <xsl:value-of select="."/>-->
<!--                    </dd>-->
<!--                </dl>-->
<!--            </xsl:for-each>-->
<!--        </xsl:if>-->

    </xsl:template>

    <!-- SubIndication Template -->
    <xsl:template match="dss:SubIndication">
        <xsl:param name="indicationClass"/>
        <xsl:variable name="subIndicationText" select="."/>
        <xsl:variable name="semanticText" select="//dss:Semantic[@Key = $subIndicationText]"/>

        <dl class="row mb-0">
            <dt class="col-sm-5">Information complémentaire :</dt>
            <dd class="col-sm-7">
                <div>
                    <xsl:if test="string-length($semanticText) &gt; 0">
                        <xsl:attribute name="data-bs-toggle">tooltip</xsl:attribute>
                        <xsl:attribute name="data-placement">right</xsl:attribute>
                        <xsl:attribute name="title"><xsl:value-of select="$semanticText"/></xsl:attribute>
                    </xsl:if>
                    <xsl:value-of select="$semanticText"/>
                </div>
            </dd>
        </dl>
    </xsl:template>

</xsl:stylesheet>
