![ESUP-SIGNATURE](https://github.com/EsupPortail/esup-signature/raw/master/src/main/resources/static/images/logo-alt.png)

https://www.esup-portail.org/wiki/display/SIGN/

Esup-signature permet gérer les circuits de visa et de signer des documents aux formats CAdES, XAdES, PAdES ou par simple apposition d'une image dans un PDF.

Son objectif est de proposer un module de signature type micro-service permettant à un établissement d’intégrer la signature numérique dans ses services institutionnels.

L’usage commun au niveau des établissements de l’Enseignement Supérieur et de la Recherche des briques que sont shibboleth (fédération d'identités Renater), ldap (supann), TCS (Trusted Certificate Service) de Renater via digicert font d’esup-signature un outil qui peut potentiellement s’adapter aux différents établissements de l’ESR.

La signature électronique s’appuie sur la solution DSS de la Commission Européenne, voir :
https://ec.europa.eu/cefdigital/wiki/display/CEFDIGITAL/DSS+releases

La signature à l’aide d’une clé matérielle est possible à l’aide de l’application Esup-DSS-CLient en lien avec DSS. 
Pour plus d’informations, voir :https://www.esup-portail.org/wiki/display/SIGN/Esup-DSS-Client

# Changelog

Important, sur la page changelog, vous trouverez aussi des détails sur les montées de version : 

https://www.esup-portail.org/wiki/display/SIGN/Change+log

# Prérequis

 * OpenJDK 17
 * Maven 3.5
 * GhostScript
 * PostgreSQL
 
# Installation

https://www.esup-portail.org/wiki/display/SIGN/Installation

# Configuration

https://github.com/EsupPortail/esup-signature/blob/master/src/main/resources/application.yml

Voir : https://www.esup-portail.org/wiki/display/SIGN/Sources+et+configuration#Sourcesetconfiguration-Configuration

# Compilation

```
mvn clean package
```
Les tests exécutés par défaut pendant `package` sont ceux pilotés par Surefire. Les tests UI Playwright sont exécutés lors de `verify` via Failsafe.
Pour l’éviter :

```
mvn clean package -DskipTests
```

# Tests UI Playwright

Prérequis supplémentaires :

 * docker-compose

Le navigateur Chromium utilisé par Playwright est téléchargé automatiquement au premier lancement. Pour préinstaller explicitement le navigateur de test et ses dépendances Linux :

```
mvn -q -DskipTests -Dexec.classpathScope=test org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install --with-deps chromium"
```
 
```
mvn verify -Djava.io.tmpdir=/opt/esup-signature-test-work/tmp -DskipSurefire=true -DskipDockerCompose=false
```

Cette commande déclenche les tests UI Playwright de `PlaywrightTest.java` via Failsafe.

Artefacts utiles après exécution :

 * rapports JUnit/Failsafe : `target/failsafe-reports/`
 * captures, traces et vidéos Playwright en cas d'échec : `target/playwright-artifacts/`
 * index HTML local pour parcourir les échecs téléchargés depuis GitHub : `target/playwright-artifacts/index.html`

Convention Maven retenue :

 * tests unitaires / intégration légers : `*Test.java` via Surefire
 * exception de configuration pour l'UI : `PlaywrightTest.java` est explicitement exclu de Surefire et inclus dans Failsafe


# Démarrage

```
mvn spring-boot:run -DskipTests
```

# Esup-DSS-Client

Pour signer à l'aide de certificats sur support cryptographique USB (eIDas) il est necessaire d'installer une application sur le poste client.
Esup-DSS-Client est disponible ici :

https://github.com/EsupPortail/esup-dss-client

# Démonstration

Site de démonstration, s’appuyant sur l’authentification shibboleth via la fédération Renater :

https://esup-signature-demo.univ-rouen.fr

# Licenses

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
