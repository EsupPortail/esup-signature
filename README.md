ESUP-SIGNATURE
==============

Esup-signature permet de signer des documents aux formats CAdES, XAdES ou par simple apposition une image dans un PDF

La signature s'appuie sur la solution DSS de la Commission Européenne, voir :
https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?pageId=46992515

L'application est en cours de développement (en Java Spring), elle propose pour l'instant :

- Création des utilisateurs via l'authentification CAS ou Shibboleth
- Upload de documents
- Signature par apposition d'un tampon image dans un PDF
- Signature PAdES (PDF) à l'aide d'un certificat
- Ajout de page de garde et conversion PDF/A
- Module pour sélectionner l'endroit de la signature
