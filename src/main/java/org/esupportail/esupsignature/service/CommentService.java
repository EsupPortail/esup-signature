package org.esupportail.esupsignature.service;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.SignRequestParams;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.exception.EsupSignatureRuntimeException;
import org.esupportail.esupsignature.repository.CommentRepository;
import org.esupportail.esupsignature.repository.SignRequestParamsRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Service
public class CommentService {

    @Resource
    private CommentRepository commentRepository;

    @Resource
    private SignRequestRepository signRequestRepository;

    @Resource
    private SignRequestParamsRepository signRequestParamsRepository;

    @Resource
    private UserService userService;

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

    @Transactional
    public void deleteComment(Long commentId, SignRequest signRequest) {
        Optional<Comment> comment = commentRepository.findById(commentId);
        if(comment.isPresent()) {
            if(signRequest == null) {
                signRequest = signRequestRepository.findSignRequestByCommentsContains(comment.get());
            }
            if (comment.get().getStepNumber() != null && comment.get().getStepNumber() > 0 && signRequest.getSignRequestParams().size() > comment.get().getStepNumber() - 1) {
                if(signRequest.getSignRequestParams().size() > comment.get().getStepNumber() - 1) {
                    SignRequestParams signRequestParams = signRequest.getSignRequestParams().get(comment.get().getStepNumber() - 1);
                    signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().get(comment.get().getStepNumber() - 1).getSignRequestParams().remove(signRequestParams);
                    signRequest.getSignRequestParams().remove(signRequestParams);
                    signRequestParamsRepository.delete(signRequestParams);
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
