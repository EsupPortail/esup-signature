package org.esupportail.esupsignature.entity.enums;

/**
 * Enumération représentant les niveaux de signature.
 *
 * Chaque niveau de signature est associé à une valeur numérique qui reflète
 * son niveau de sécurité ou de complexité :
 *
 * Signature simple : adaptée aux documents internes ou aux échanges ne présentant pas de risque particulier, lorsqu’il n’existe pas d’obligation légale imposant un niveau supérieur. (apposition d’une image + traces)
 * Signature avancée : recommandée pour les documents nécessitant de pouvoir identifier le signataire et disposer d’un élément de preuve supplémentaire, par exemple pour des contrats ou des engagements comportant un risque juridique modéré. (nécessite un certificat électronique)
 * Signature qualifiée : à privilégier pour les actes à portée européenne ou pour les situations à fort enjeu (importance juridique ou financière), car elle bénéficie d’une reconnaissance mutuelle dans l’Union européenne et d’une présomption de fiabilité équivalente à une signature manuscrite. (nécessite un certificat eIDas)
  */
public enum SignLevel {
    simple(2), advanced(3), qualified(4);

    private final int value;

    SignLevel(final int newValue) {
        value = newValue;
    }

    public int getValue() { return value; }

}
