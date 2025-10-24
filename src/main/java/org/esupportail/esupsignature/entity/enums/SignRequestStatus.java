package org.esupportail.esupsignature.entity.enums;

import java.util.Arrays;

/**
 * Enumération représentant les différents statuts possibles d'une demande de signature.
 * Ces statuts permettent de suivre l'avancement du processus de signature.
 *
 * Les différents statuts disponibles sont :
 * - uploading : Le document est en cours de téléchargement.
 * - draft : La demande de signature est en mode brouillon.
 * - pending : La demande de signature est en attente.
 * - checked : La demande a été validée.
 * - signed : La demande a été signée.
 * - refused : La demande a été refusée par un utilisateur.
 * - deleted : Statut obsolète, La demande a été supprimée.
 * - completed : Le processus de signature est terminé.
 * - exported : Le document a été exporté aux emplacements de destination.
 * - archived : Statut obsolète, représente une demande archivée.
 * - cleaned : Statut obsolète, représente une demande nettoyée.
 *
 */
public enum SignRequestStatus {
    uploading, draft, pending, checked, signed, refused, @Deprecated deleted, completed, exported, @Deprecated archived, @Deprecated cleaned;

    public static SignRequestStatus[] activeValues() {
        return Arrays.stream(values())
                .filter(s -> {
                    try {
                        return SignRequestStatus.class
                                .getField(s.name())
                                .getAnnotation(Deprecated.class) == null;
                    } catch (NoSuchFieldException e) {
                        return false;
                    }
                })
                .toArray(SignRequestStatus[]::new);
    }
}
