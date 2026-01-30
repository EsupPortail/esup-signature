package org.esupportail.esupsignature.entity.enums;


/**
 * Enumération représentant les différents paramètres de l'interface utilisateur (UI).
 * Attention : ne jamais modifier l'ordre ou supprimer une valeur. Seul l'ajout est autorisé
 * Ces paramètres définissent les différentes sections d'aide et d'information
 * disponibles dans l'application, ainsi que certains paramètres spécifiques liés
 * aux filtres, favoris et alertes.
 *
 * Les valeurs suivantes sont disponibles :
 *
 * - homeHelp : Aide pour la page d'accueil.
 * - signRequestHelp : Aide pour les demandes de signature.
 * - userParamsHelp : Aide pour la gestion des paramètres utilisateurs.
 * - workflowFilterStatus : Statut des filtres pour les workflows.
 * - formFilterStatus : Statut des filtres pour les formulaires.
 * - globalFilterStatus : Statut des filtres globaux.
 * - workflowVisaAlert : Alertes pour les visas liés aux workflows.
 * - favoriteWorkflows : Workflows favoris.
 * - favoriteForms : Formulaires favoris.
 * - home2Help : Aide supplémentaire pour la page d'accueil.
 */
public enum UiParams {
    homeHelp,
    signRequestHelp,
    userParamsHelp,
    workflowFilterStatus,
    formFilterStatus,
    globalFilterStatus,
    workflowVisaAlert,
    favoriteWorkflows,
    favoriteForms,
    home2Help
}
