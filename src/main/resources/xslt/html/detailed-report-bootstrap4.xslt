<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dss="http://dss.esig.europa.eu/validation/detailed-report">

    <xsl:output method="html" encoding="utf-8" indent="yes" omit-xml-declaration="yes" />

    <xsl:template match="/dss:DetailedReport">
        <div>
            <xsl:attribute name="class">card</xsl:attribute>
            <div>
                <xsl:attribute name="class">card-header bg-primary</xsl:attribute>
                <xsl:attribute name="data-target">#collapseDR</xsl:attribute>
                <xsl:attribute name="data-toggle">collapse</xsl:attribute>
                Validation
            </div>
            <div>
                <xsl:attribute name="class">card-body collapse in</xsl:attribute>
                <xsl:attribute name="id">collapseDR</xsl:attribute>

                <xsl:comment>Generated by DSS v.5.7</xsl:comment>

                <xsl:apply-templates select="dss:Certificate"/>
                <xsl:apply-templates select="dss:BasicBuildingBlocks[@Type='CERTIFICATE']"/>

                <xsl:apply-templates select="dss:Signature"/>
                <xsl:apply-templates select="dss:Timestamp"/>
                <xsl:apply-templates select="dss:BasicBuildingBlocks[@Type='SIGNATURE']"/>
                <xsl:apply-templates select="dss:BasicBuildingBlocks[@Type='COUNTER_SIGNATURE']"/>
                <xsl:apply-templates select="dss:BasicBuildingBlocks[@Type='TIMESTAMP']"/>
                <xsl:apply-templates select="dss:BasicBuildingBlocks[@Type='REVOCATION']"/>

                <xsl:apply-templates select="dss:TLAnalysis" />
            </div>
        </div>

    </xsl:template>

    <xsl:template match="dss:Signature">
        <div>
            <xsl:attribute name="class">card mb-3</xsl:attribute>
            <div>
                <xsl:attribute name="class">card-header bg-primary</xsl:attribute>
                <xsl:attribute name="data-target">#collapseSignatureValidationData<xsl:value-of select="@Id"/></xsl:attribute>
                <xsl:attribute name="data-toggle">collapse</xsl:attribute>

                <xsl:if test="@CounterSignature = 'true'">
                    <span>
                        <xsl:attribute name="class">badge badge-info float-right</xsl:attribute>
                        Counter-signature
                    </span>
                </xsl:if>

                Signature <xsl:value-of select="@Id"/>
            </div>
            <xsl:if test="count(child::*[name(.)!='Conclusion']) &gt; 0">
                <div>
                    <xsl:attribute name="class">card-body collapse in</xsl:attribute>
                    <xsl:attribute name="id">collapseSignatureValidationData<xsl:value-of select="@Id"/></xsl:attribute>
                    <xsl:apply-templates select="dss:ValidationProcessBasicSignature" />
                    <xsl:apply-templates select="dss:Timestamp" />
                    <xsl:apply-templates select="dss:ValidationProcessLongTermData" />
                    <xsl:apply-templates select="dss:ValidationProcessArchivalData" />

                    <xsl:apply-templates select="dss:ValidationSignatureQualification"/>
                </div>
            </xsl:if>
        </div>
    </xsl:template>

    <xsl:template match="dss:Timestamp">
        <div>
            <xsl:attribute name="class">card mb-3</xsl:attribute>
            <div>
                <xsl:attribute name="class">card-header</xsl:attribute>
                <xsl:attribute name="data-target">#collapseTimestamp<xsl:value-of select="@Id"/></xsl:attribute>
                <xsl:attribute name="data-toggle">collapse</xsl:attribute>

                Timestamp <xsl:value-of select="@Id"/>
            </div>
            <xsl:if test="count(child::*[name(.)!='Conclusion']) &gt; 0">
                <div>
                    <xsl:attribute name="class">card-body collapse in</xsl:attribute>
                    <xsl:attribute name="id">collapseTimestamp<xsl:value-of select="@Id"/></xsl:attribute>
                    <xsl:apply-templates select="dss:ValidationProcessTimestamp"/>
                    <xsl:apply-templates select="dss:ValidationTimestampQualification"/>
                </div>
            </xsl:if>
        </div>
    </xsl:template>

    <xsl:template match="dss:BasicBuildingBlocks">
        <div>
            <xsl:if test="@Id != ''">
                <xsl:attribute name="id"><xsl:value-of select="@Id"/></xsl:attribute>
            </xsl:if>
            <xsl:attribute name="class">card mb-3</xsl:attribute>
            <div>
                <xsl:attribute name="class">card-header bg-primary</xsl:attribute>
                <xsl:attribute name="data-target">#collapseBasicBuildingBlocks<xsl:value-of select="@Id"/></xsl:attribute>
                <xsl:attribute name="data-toggle">collapse</xsl:attribute>

                Basic Building Blocks <br/>
                <xsl:value-of select="@Type"/> (Id = <xsl:value-of select="@Id"/>)
            </div>
            <xsl:if test="count(child::*[name(.)!='Conclusion']) &gt; 0">
                <xsl:variable name="PSV" select="dss:PSV" />
                <xsl:variable name="SubXCV" select="dss:XCV/dss:SubXCV" />
                <div>
                    <xsl:attribute name="class">card-body collapse</xsl:attribute>
                    <xsl:attribute name="id">collapseBasicBuildingBlocks<xsl:value-of select="@Id"/></xsl:attribute>

                    <xsl:apply-templates select="dss:FC" />
                    <xsl:apply-templates select="dss:ISC" />
                    <xsl:apply-templates select="dss:VCI" />
                    <xsl:apply-templates select="dss:CV" />
                    <xsl:apply-templates select="dss:SAV" />
                    <xsl:apply-templates select="dss:XCV" />
                    <xsl:if test="$PSV != ''">
                        <hr />
                    </xsl:if>
                    <xsl:apply-templates select="dss:PSV" />
                    <xsl:apply-templates select="dss:PCV" />
                    <xsl:apply-templates select="dss:VTS" />
                    <xsl:if test="$SubXCV != ''">
                        <hr />
                    </xsl:if>
                    <xsl:apply-templates select="dss:XCV/dss:SubXCV" />
                </div>
            </xsl:if>
        </div>
    </xsl:template>

    <xsl:template match="dss:ValidationProcessBasicSignature|dss:ValidationProcessLongTermData|dss:ValidationProcessArchivalData|dss:Certificate">
        <div>
            <xsl:attribute name="class">card mb-3</xsl:attribute>
            <div>
                <xsl:attribute name="class">card-header</xsl:attribute>
                <xsl:attribute name="data-target">#collapse<xsl:value-of select="name(.)"/><xsl:value-of select="../@Id"/></xsl:attribute>
                <xsl:attribute name="data-toggle">collapse</xsl:attribute>

                <xsl:call-template name="badge-conclusion">
                    <xsl:with-param name="Conclusion" select="dss:Conclusion" />
                    <xsl:with-param name="AdditionalClass" select="' float-right'" />
                </xsl:call-template>

                <xsl:value-of select="concat(@Title, ' ')"/>

                <xsl:if test="dss:ProofOfExistence/dss:Time">
                    <i>
                        <xsl:attribute name="class">fa fa-clock-o</xsl:attribute>
                        <xsl:attribute name="title">Best signature time : <xsl:value-of select="dss:ProofOfExistence/dss:Time" /></xsl:attribute>
                    </i>
                </xsl:if>

            </div>
            <xsl:if test="count(child::*[name(.)!='Conclusion']) &gt; 0">
                <div>
                    <xsl:attribute name="class">card-body collapse in</xsl:attribute>
                    <xsl:attribute name="id">collapse<xsl:value-of select="name(.)"/><xsl:value-of select="../@Id"/></xsl:attribute>
                    <xsl:apply-templates/>
                </div>
            </xsl:if>
        </div>
    </xsl:template>

    <xsl:template match="dss:ValidationProcessTimestamp">
        <div>
            <xsl:attribute name="class">card mb-3</xsl:attribute>
            <div>
                <xsl:attribute name="class">card-header</xsl:attribute>
                <xsl:attribute name="data-target">#collapseTimestampValidationData<xsl:value-of select="../@Id"/></xsl:attribute>
                <xsl:attribute name="data-toggle">collapse</xsl:attribute>

                <xsl:call-template name="badge-conclusion">
                    <xsl:with-param name="Conclusion" select="dss:Conclusion" />
                    <xsl:with-param name="AdditionalClass" select="' float-right'" />
                </xsl:call-template>

                <xsl:value-of select="@Title"/>

                <br />

                <xsl:value-of select="concat(@Type, ' ')"/>

                <i>
                    <xsl:attribute name="class">fa fa-clock-o</xsl:attribute>
                    <xsl:attribute name="title">Production time : <xsl:value-of select="@ProductionTime"/></xsl:attribute>
                </i>
            </div>
            <xsl:if test="count(child::*[name(.)!='Conclusion']) &gt; 0">
                <div>
                    <xsl:attribute name="class">card-body collapse in</xsl:attribute>
                    <xsl:attribute name="id">collapseTimestampValidationData<xsl:value-of select="../@Id"/></xsl:attribute>
                    <xsl:apply-templates/>
                </div>
            </xsl:if>
        </div>
    </xsl:template>

    <xsl:template match="dss:TLAnalysis">
        <xsl:if test="@Id != ''">
            <xsl:attribute name="id"><xsl:value-of select="@Id"/></xsl:attribute>
        </xsl:if>

        <div>
            <xsl:attribute name="class">card mb-3</xsl:attribute>
            <div>
                <xsl:attribute name="class">card-header</xsl:attribute>
                <xsl:attribute name="data-target">#collapseTL<xsl:value-of select="@CountryCode"/></xsl:attribute>
                <xsl:attribute name="data-toggle">collapse</xsl:attribute>

                <xsl:call-template name="badge-conclusion">
                    <xsl:with-param name="Conclusion" select="dss:Conclusion" />
                    <xsl:with-param name="AdditionalClass" select="' float-right'" />
                </xsl:call-template>

                <xsl:value-of select="@Title"/>
            </div>
            <xsl:if test="count(child::*[name(.)!='Conclusion']) &gt; 0">
                <div>
                    <xsl:attribute name="class">card-body collapse</xsl:attribute>
                    <xsl:attribute name="id">collapseTL<xsl:value-of select="@CountryCode"/></xsl:attribute>
                    <xsl:apply-templates/>
                </div>
            </xsl:if>
        </div>
    </xsl:template>

    <xsl:template match="dss:ValidationSignatureQualification">
        <div>
            <xsl:attribute name="class">card</xsl:attribute>
            <div>
                <xsl:attribute name="class">card-header</xsl:attribute>
                <xsl:attribute name="data-target">#collapseSigAnalysis<xsl:value-of select="@Id"/></xsl:attribute>
                <xsl:attribute name="data-toggle">collapse</xsl:attribute>

                <span>
                    <xsl:attribute name="class">badge badge-secondary float-right</xsl:attribute>
                    <xsl:value-of select="@SignatureQualification"/>
                </span>

                <xsl:value-of select="@Title"/>
            </div>
            <div>
                <xsl:attribute name="class">card-body collapse in</xsl:attribute>
                <xsl:attribute name="id">collapseSigAnalysis<xsl:value-of select="@Id"/></xsl:attribute>
                <xsl:apply-templates/>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="dss:ValidationTimestampQualification">
        <div>
            <xsl:attribute name="class">card</xsl:attribute>
            <div>
                <xsl:attribute name="class">card-header</xsl:attribute>
                <xsl:attribute name="data-target">#collapseTstAnalysis<xsl:value-of select="@Id"/></xsl:attribute>
                <xsl:attribute name="data-toggle">collapse</xsl:attribute>

                <span>
                    <xsl:attribute name="class">badge badge-secondary float-right</xsl:attribute>
                    <xsl:value-of select="@TimestampQualification"/>
                </span>

                <xsl:value-of select="@Title"/>
            </div>
            <div>
                <xsl:attribute name="class">card-body collapse in</xsl:attribute>
                <xsl:attribute name="id">collapseTstAnalysis<xsl:value-of select="@Id"/></xsl:attribute>
                <xsl:apply-templates/>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="dss:ValidationCertificateQualification">
        <div>
            <xsl:attribute name="class">card mt-3</xsl:attribute>
            <div>
                <xsl:attribute name="class">card-header</xsl:attribute>
                <xsl:attribute name="data-target">#cert-qual-<xsl:value-of select="generate-id(.)"/></xsl:attribute>
                <xsl:attribute name="data-toggle">collapse</xsl:attribute>

                <span>
                    <xsl:attribute name="class">badge badge-secondary float-right</xsl:attribute>
                    <xsl:value-of select="@CertificateQualification"/>
                </span>

                <xsl:value-of select="concat(@Title, ' ')"/>

                <i>
                    <xsl:attribute name="class">fa fa-clock-o</xsl:attribute>
                    <xsl:attribute name="title"><xsl:value-of select="@DateTime"/></xsl:attribute>
                </i>
            </div>
            <div>
                <xsl:attribute name="class">card-body collapse in</xsl:attribute>
                <xsl:attribute name="id">cert-qual-<xsl:value-of select="generate-id(.)"/></xsl:attribute>
                <xsl:apply-templates/>
            </div>
        </div>
    </xsl:template>

    <xsl:template name="badge-conclusion">
        <xsl:param name="Conclusion"/>
        <xsl:param name="AdditionalClass"/>

        <xsl:variable name="indicationText" select="$Conclusion/dss:Indication"/>
        <xsl:variable name="indicationCssClass">
            <xsl:choose>
                <xsl:when test="$indicationText='PASSED'">badge-success</xsl:when>
                <xsl:when test="$indicationText='INDETERMINATE'">badge-warning</xsl:when>
                <xsl:when test="$indicationText='FAILED'">badge-danger</xsl:when>
                <xsl:otherwise>badge-secondary</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:choose>
            <xsl:when test="string-length(dss:Conclusion/dss:SubIndication) &gt; 0">
                <span>
                    <xsl:attribute name="class">badge <xsl:value-of select="$indicationCssClass" /> <xsl:value-of select="$AdditionalClass" /></xsl:attribute>
                    <xsl:if test="string-length(dss:Conclusion/dss:Error) &gt; 0">
                        <xsl:attribute name="title"><xsl:value-of select="dss:Conclusion/dss:Error"/></xsl:attribute>
                    </xsl:if>
                    <xsl:if test="string-length(dss:Conclusion/dss:Warning) &gt; 0">
                        <xsl:attribute name="title"><xsl:value-of select="dss:Conclusion/dss:Warning"/></xsl:attribute>
                    </xsl:if>
                    <xsl:value-of select="dss:Conclusion/dss:SubIndication"/>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span>
                    <xsl:attribute name="class">badge <xsl:value-of select="$indicationCssClass" /> <xsl:value-of select="$AdditionalClass" /></xsl:attribute>
                    <xsl:value-of select="dss:Conclusion/dss:Indication"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="dss:FC|dss:ISC|dss:VCI|dss:CV|dss:SAV|dss:XCV|dss:PSV|dss:PCV|dss:VTS">
        <div>
            <xsl:attribute name="id"><xsl:value-of select="../@Id"/>-<xsl:value-of select="name()"/></xsl:attribute>
            <xsl:attribute name="class">row mt-2</xsl:attribute>
            <div>
                <xsl:attribute name="class">col</xsl:attribute>
                <strong>
                    <xsl:value-of select="@Title"/> :
                </strong>

                <xsl:call-template name="badge-conclusion">
                    <xsl:with-param name="Conclusion" select="dss:Conclusion" />
                    <xsl:with-param name="AdditionalClass" select="" />
                </xsl:call-template>
            </div>
        </div>
        <xsl:apply-templates select="dss:Constraint" />
    </xsl:template>

    <xsl:template match="dss:SubXCV|dss:RAC|dss:RFC">
        <div>
            <xsl:variable name="parentId">
                <xsl:choose>
                    <xsl:when test="name()='SubXCV'" ><xsl:value-of select="../../@Id"/></xsl:when>
                    <xsl:otherwise><xsl:value-of select="../@Id"/></xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="currentId" select="concat(name(), '-', @Id, '-', $parentId)"/>
            <xsl:attribute name="id"><xsl:value-of select="$currentId"/></xsl:attribute>
            <div>
                <xsl:attribute name="class">card mt-3</xsl:attribute>
                <div>
                    <xsl:attribute name="data-target">#collapseSubXCV<xsl:value-of select="$currentId"/></xsl:attribute>
                    <xsl:attribute name="data-toggle">collapse</xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="@TrustAnchor = 'true'">
                            <xsl:attribute name="class">card-header border-bottom-0</xsl:attribute>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:attribute name="class">card-header</xsl:attribute>
                        </xsl:otherwise>
                    </xsl:choose>

                    <xsl:choose>
                        <xsl:when test="@TrustAnchor = 'true'">
                            <i>
                                <xsl:attribute name="class">fa fa-certificate float-right</xsl:attribute>
                                <xsl:attribute name="title">Trust Anchor</xsl:attribute>
                            </i>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:call-template name="badge-conclusion">
                                <xsl:with-param name="Conclusion" select="dss:Conclusion" />
                                <xsl:with-param name="AdditionalClass" select="' float-right'" />
                            </xsl:call-template>
                        </xsl:otherwise>
                    </xsl:choose>

                    <xsl:value-of select="@Title"/>

                    <xsl:if test="@Id">
                        <br />

                        <xsl:value-of select="concat('Id = ', @Id)"/>
                    </xsl:if>
                </div>

                <xsl:if test="name() != 'SubXCV' or @TrustAnchor != 'true'">
                    <div>
                        <xsl:attribute name="class">card-body collapse in</xsl:attribute>
                        <xsl:attribute name="id">collapseSubXCV<xsl:value-of select="$currentId"/></xsl:attribute>
                        <xsl:apply-templates/>
                    </div>
                </xsl:if>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="dss:Constraint">
        <div>
            <xsl:attribute name="class">row</xsl:attribute>
            <div>
                <xsl:attribute name="class">col-md-10</xsl:attribute>
                <xsl:value-of select="dss:Name"/>
                <xsl:if test="@Id">
                    <xsl:variable name="NameId" select="dss:Name/@NameId"/>
                    <a>
                        <xsl:choose>
                            <xsl:when test="$NameId='BBB_XCV_SUB'">
                                <xsl:attribute name="href">#SubXCV-<xsl:value-of select="concat(@Id, '-', ../../@Id)"/></xsl:attribute>
                            </xsl:when>
                            <xsl:when test="$NameId='PSV_IPSVC'">
                                <xsl:attribute name="href">#<xsl:value-of select="@Id"/>-PSV</xsl:attribute>
                            </xsl:when>
                            <xsl:when test="$NameId='PSV_IPCVA'">
                                <xsl:attribute name="href">#<xsl:value-of select="@Id"/>-PCV</xsl:attribute>
                            </xsl:when>
                            <xsl:when test="$NameId='PCV_IVTSC'">
                                <xsl:attribute name="href">#<xsl:value-of select="@Id"/>-VTS</xsl:attribute>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:attribute name="href">#<xsl:value-of select="@Id"/></xsl:attribute>
                            </xsl:otherwise>
                        </xsl:choose>
                        <xsl:attribute name="title">Details</xsl:attribute>
                        <xsl:attribute name="class">ml-2</xsl:attribute>
                        <i>
                            <xsl:attribute name="class">fa fa-arrow-circle-right</xsl:attribute>
                        </i>
                    </a>
                </xsl:if>
            </div>
            <div>
                <xsl:attribute name="class">col-md-2</xsl:attribute>
                <xsl:variable name="statusText" select="dss:Status"/>
                <xsl:choose>
                    <xsl:when test="$statusText='OK'">
                        <i>
                            <xsl:attribute name="class">fa fa-check-circle text-success</xsl:attribute>
                            <xsl:attribute name="title"><xsl:value-of select="$statusText" /></xsl:attribute>
                        </i>
                    </xsl:when>
                    <xsl:when test="$statusText='NOT OK'">
                        <i>
                            <xsl:attribute name="class">fa fa-times-circle text-danger</xsl:attribute>
                            <xsl:attribute name="title"><xsl:value-of select="$statusText" /> : <xsl:value-of select="dss:Error" /></xsl:attribute>
                        </i>
                    </xsl:when>
                    <xsl:when test="$statusText='WARNING'">
                        <i>
                            <xsl:attribute name="class">fa fa-exclamation-circle text-warning</xsl:attribute>
                            <xsl:attribute name="title"><xsl:value-of select="$statusText" /> : <xsl:value-of select="dss:Warning" /></xsl:attribute>
                        </i>
                    </xsl:when>
                    <xsl:when test="$statusText='INFORMATION'">
                        <i>
                            <xsl:attribute name="class">fa fa-info-circle text-info</xsl:attribute>
                            <xsl:attribute name="title"><xsl:value-of select="$statusText" /> : <xsl:value-of select="dss:Info" /></xsl:attribute>
                        </i>
                    </xsl:when>
                    <xsl:when test="$statusText='IGNORED'">
                        <i>
                            <xsl:attribute name="class">fa fa-eye-slash text-muted</xsl:attribute>
                            <xsl:attribute name="title"><xsl:value-of select="$statusText" /> : The check is skipped by the validation policy</xsl:attribute>
                        </i>
                    </xsl:when>
                    <xsl:otherwise>
                        <span>
                            <xsl:value-of select="dss:Status" />
                        </span>
                    </xsl:otherwise>
                </xsl:choose>

                <xsl:if test="dss:AdditionalInfo">
                    <i>
                        <xsl:attribute name="class">fa fa-plus-circle text-info ml-2</xsl:attribute>
                        <xsl:attribute name="data-toggle">tooltip</xsl:attribute>
                        <xsl:attribute name="data-placement">right</xsl:attribute>
                        <xsl:attribute name="title"><xsl:value-of select="dss:AdditionalInfo" /></xsl:attribute>
                    </i>
                </xsl:if>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="*">
        <xsl:comment>
            Ignored tag:
            <xsl:value-of select="name()" />
        </xsl:comment>
    </xsl:template>

</xsl:stylesheet>