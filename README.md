![ESUP-SIGNATURE](https://github.com/EsupPortail/esup-signature/raw/master/src/main/resources/static/images/logo.png)

https://www.esup-portail.org/wiki/display/SIGN/

Esup-signature permet gérer les circuits de visa et de signer des documents aux formats CAdES, XAdES, PAdES ou par simple apposition d'une image dans un PDF.

Son objectif est de proposer un module de signature type micro-service permettant à un établissement d’intégrer la signature numérique dans ses services institutionnels.

L’usage commun au niveau des établissements de l'Enseignement Supérieur et de la Recherche des briques que sont shibboleth (fédération d'identités Renater), ldap (supann), TCS (Trusted Certificate Service) de Renater via digicert font d’esup-signature un outil qui peut potentiellement s’adapter aux différents établissements de l’ESR.

La signature électronique s’appuie sur la solution DSS de la Commission Européenne, voir :
https://ec.europa.eu/cefdigital/wiki/display/CEFDIGITAL/DSS+releases

La signature à l’aide d’une clé matérielle est possible à l’aide de l’application NexU en lien avec DSS. La liste des dispositifs matériels pris en charge est présente ici : https://nowina.lu/solutions/java-less-browser-signing-nexu/

# Changelog

Important, sur la page changelog vous trouverez aussi des détails sur les montés de version : 

https://www.esup-portail.org/wiki/display/SIGN/Change+log

# Prérequis

 * OpenJDK 14
 * Maven 3.1
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
Un jeu de test est exécuté au moment de la compilation, tous les prérequis et paramètres doivent être correctes pour que le projet compile.

# Démarrage

```
mvn spring-boot:run
```

# Démonstration

Site de démonstration, s'appuyant sur l'authentification shibboleth via la fédération Renater :

https://esup-signature-demo.univ-rouen.fr

# Licenses

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
