package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.CommentRepository;
import org.esupportail.esupsignature.repository.LiveWorkflowStepRepository;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

/**
 * Service permettant de gérer les commentaires associés à des demandes de signature.
 *
 * Cette classe fournit des méthodes pour créer, récupérer, mettre à jour, supprimer
 * et anonymiser des commentaires. Les commentaires peuvent inclure des annotations
 * détaillées sur des documents, comme des post-its virtuels positionnés sur des pages spécifiques.
 */
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final SignRequestRepository signRequestRepository;
    private final SignRequestParamsRepository signRequestParamsRepository;
    private final UserService userService;
    private final LiveWorkflowStepRepository liveWorkflowStepRepository;

    public CommentService(CommentRepository commentRepository, SignRequestRepository signRequestRepository, SignRequestParamsRepository signRequestParamsRepository, UserService userService, LiveWorkflowStepRepository liveWorkflowStepRepository) {
        this.commentRepository = commentRepository;
        this.signRequestRepository = signRequestRepository;
        this.signRequestParamsRepository = signRequestParamsRepository;
        this.userService = userService;
        this.liveWorkflowStepRepository = liveWorkflowStepRepository;
    }

    /**
     * Récupère un commentaire à partir de son identifiant.
     *
     * @param id l'identifiant unique du commentaire à récupérer
     * @return le commentaire correspondant à l'identifiant fourni
     * @throws NoSuchElementException si aucun commentaire n'est trouvé pour l'identifiant donné
     */
    @Transactional
    public Comment getById(Long id) {
        return commentRepository.findById(id).orElseThrow();
    }

    /**
     * Crée un nouveau commentaire pour une demande de signature spécifique.
     *
     * @param signRequestId l'identifiant unique de la demande de signature à laquelle le commentaire sera associé
     * @param text le texte du commentaire
     * @param posX la position horizontale du commentaire sur la page en coordonnées
     * @param posY la position verticale du commentaire sur la page en coordonnées
     * @param pageNumer le numéro de la page où le commentaire sera placé
     * @param stepNumber le numéro de l'étape du flux de travail associé au commentaire
     * @param postit indique si le commentaire est un post-it (true) ou non (false)
     * @param postitColor la couleur du post-it, si applicable
     * @param userEppn l'identifiant EPPN de l'utilisateur qui crée le commentaire
     * @return le commentaire nouvellement créé
     * @throws EsupSignatureRuntimeException si le nombre maximal de post-its pour la demande de signature est dépassé
     */
    @Transactional
    public Comment create(Long signRequestId, String text, Integer posX, Integer posY, Integer pageNumer, Integer stepNumber, Boolean postit, String postitColor, String userEppn) {
        User user = userService.getByEppn(userEppn);
        SignRequest signRequest = signRequestRepository.findById(signRequestId).get();
        if(signRequest.getComments().stream().filter(Comment::getPostit).count() > 8) {
            throw new EsupSignatureRuntimeException("Trop de postit, tue le postit");
        }
        Comment comment = new Comment();
        comment.setText(text);
        comment.setCreateBy(user);
        comment.setCreateDate(new Date());
        comment.setPosX(posX);
        comment.setPosY(posY);
        comment.setPageNumber(pageNumer);
        comment.setStepNumber(stepNumber);
        comment.setPostit(postit);
        if(postitColor != null) {
            comment.setPostitColor(postitColor);
        }
        commentRepository.save(comment);
        signRequest.getComments().add(comment);
        return comment;
    }

    /**
     * Met à jour le commentaire associé à une demande de signature.
     *
     * @param signRequestId l'identifiant unique de la demande de signature
     * @param postitId l'identifiant unique du commentaire
     * @param text le nouveau texte du commentaire à mettre à jour
     * @throws EsupSignatureException si la demande de signature a été refusée, rendant la modification impossible
     */
    public void updateComment(Long signRequestId, Long postitId, String text) throws EsupSignatureException {
        Comment comment = getById(postitId);
        SignRequest signRequest = signRequestRepository.findById(signRequestId).orElseThrow();
        if(!signRequest.getStatus().equals(SignRequestStatus.refused)) {
            comment.setText(text);
            commentRepository.save(comment);
        } else {
            throw new EsupSignatureException("Demande refusé, modification impossible");
        }
    }

    /**
     * Supprime un post-it associé à une demande de signature donnée.
     *
     * Cette méthode supprime un commentaire de type post-it d'une demande de signature,
     * à condition que la demande n'ait pas été refusée. Une exception est levée si la
     * demande a été refusée, car les modifications ne sont pas autorisées dans ce cas.
     *
     * @param signRequestId l'identifiant unique de la demande de signature associée
     * @param postitId l'identifiant unique du post-it à supprimer
     * @throws EsupSignatureException si la demande de signature a été refusée, rendant la suppression impossible
     */
    @Transactional
    public void deletePostit(Long signRequestId, Long postitId) throws EsupSignatureException {
        Comment comment = getById(postitId);
        SignRequest signRequest = signRequestRepository.findById(signRequestId).orElseThrow();
        if(!signRequest.getStatus().equals(SignRequestStatus.refused)) {
            signRequest.getComments().remove(comment);
            deleteComment(postitId, signRequest);
        } else {
            throw new EsupSignatureException("Demande refusé, modification impossible");
        }
    }

    /**
     * Supprime un commentaire spécifique associé à une demande de signature.
     *
     * Cette méthode supprime un commentaire donné, ainsi que ses références dans les paramètres de demande de signature
     * et les étapes du flux de travail en direct associées. Si le commentaire fait partie d'une étape spécifique
     * du flux de travail, ses références sont également supprimées.
     *
     * @param commentId l'identifiant du commentaire à supprimer
     * @param signRequest la demande de signature associée au commentaire, peut être nul.
     *                    Si elle est nulle, la demande de signature sera recherchée en fonction du commentaire fourni.
     */
    @Transactional
    public void deleteComment(Long commentId, SignRequest signRequest) {
        Optional<Comment> comment = commentRepository.findById(commentId);
        if(comment.isPresent()) {
            if(signRequest == null) {
                signRequest = signRequestRepository.findSignRequestByCommentsContains(comment.get());
            }
            if (comment.get().getStepNumber() != null && comment.get().getStepNumber() > 0 && signRequest.getSignRequestParams().size() > comment.get().getStepNumber() - 1) {
                if(signRequest.getSignRequestParams().size() > comment.get().getStepNumber() - 1) {
                    if(signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().size() > comment.get().getStepNumber() - 1) {
                        LiveWorkflowStep liveWorkflowStep = signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().get(comment.get().getStepNumber() - 1);
                        Optional<SignRequestParams> signRequestParams = liveWorkflowStep.getSignRequestParams().stream().filter(srp -> srp.getxPos().equals(comment.get().getPosX()) && srp.getyPos().equals(comment.get().getPosY()) && srp.getSignPageNumber().equals(comment.get().getPageNumber())).findFirst();
                        if(signRequestParams.isPresent()) {
                            liveWorkflowStep.getSignRequestParams().remove(signRequestParams.get());
                            liveWorkflowStepRepository.save(liveWorkflowStep);
                            signRequest.getSignRequestParams().remove(signRequestParams.get());
                            signRequestParamsRepository.delete(signRequestParams.get());
                        }
                    }
                }
            }
            signRequest.getComments().remove(comment.get());
            commentRepository.delete(comment.get());
        }
    }

    /**
     * Anonymise les commentaires associés à un utilisateur donné.
     * Cette méthode remplace l'auteur de chaque commentaire créé par l'utilisateur
     * avec un utilisateur générique "anonyme".
     *
     * @param userId l'identifiant de l'utilisateur dont les commentaires doivent être anonymisés
     */
    @Transactional
    public void anonymizeComment(Long userId) {
        User user = userService.getById(userId);
        for(Comment comment : commentRepository.findCommentByCreateBy(user)) {
            comment.setCreateBy(userService.getAnonymousUser());
        }
    }

}
