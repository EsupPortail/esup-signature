ESUP-SIGNATURE
==============

Esup-signature permet de signer des documents aux formats CAdES, XAdES ou par simple apposition une image dans un PDF

La signature s'appuie sur la solution DSS de la Commission Européenne, voir :
https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?pageId=46992515

Pour plus de détails voir:
https://www.esup-portail.org/wiki/display/SIGN/esup-signature

#####Ajout de sedalib dans le dépot maven

mvn install:install-file -Dfile=src/etc/sedalib-2.1.0.jar -DgroupId=fr.gouv.vitam -DartifactId=sedalib -Dversion=2.1.0