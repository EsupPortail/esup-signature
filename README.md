ESUP-SIGNATURE
==============

Esup-signature permet de signer des documents aux formats CAdES, XAdES ou par simple apposition une image dans un PDF

La signature s'appuie sur la solution DSS de la Commission Européenne, voir :
https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?pageId=46992515

Pour plus de détails voir:
https://www.esup-portail.org/wiki/display/SIGN/esup-signature

#####Ajout de sedalib dans le dépot maven

mvn install:install-file -Dfile=src/etc/sedalib-2.x.x.jar -DgroupId=fr.gouv.vitam -DartifactId=sedalib -Dversion=2.x.x

Installation esup signature

Prérequis :

maven > 3.1.0

Install sedalib

Suivre la procédure d'installation de sedatools : https://github.com/ProgrammeVitam/sedatools

cd /tmp
mkdir test-sedatools
cd test-sedatools
git clone https://github.com/ProgrammeVitam/java-libpst-origin.git
cd java-libpst-origin/
git checkout master
mvn clean install
(L'erreur rencontée lors de cette installation ne posent pas de problème pour la suite)
cd ..
git clone https://github.com/ProgrammeVitam/droid-origin.git
cd droid-origin
mvn clean install
cd ..
git clone https://github.com/ProgrammeVitam/sedatools.git
cd sedatools/
mvn clean install

Dossier de travail service tomcat : WorkingDirectory=/opt/tomcat-esup-signature/temp

Configuration :

path ojService