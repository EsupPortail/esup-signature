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

    @Transactional
    public Comment getById(Long id) {
        return commentRepository.findById(id).orElseThrow();
    }

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

    @Transactional
    public void anonymizeComment(Long userId) {
        User user = userService.getById(userId);
        for(Comment comment : commentRepository.findCommentByCreateBy(user)) {
            comment.setCreateBy(userService.getAnonymousUser());
        }
    }

}
