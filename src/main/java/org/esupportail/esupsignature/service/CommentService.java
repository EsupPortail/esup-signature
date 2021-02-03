package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class CommentService {

    @Resource
    private CommentRepository commentRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private UserService userService;

    @Transactional
    public Comment create(Long signRequestId, String text, Integer posX, Integer posY, Integer pageNumer, Integer stepNumber, Boolean spot, Boolean postit, String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        SignRequest signRequest = signRequestService.getById(signRequestId);
        Comment comment = new Comment();
        comment.setText(text);
        comment.setCreateBy(user);
        comment.setCreateDate(new Date());
        comment.setPosX(posX);
        comment.setPosY(posY);
        comment.setPageNumber(pageNumer);
        comment.setStepNumber(stepNumber);
        comment.setSpot(spot);
        comment.setPostit(postit);
        commentRepository.save(comment);
        signRequest.getComments().add(comment);
        return comment;
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).get();
        SignRequest signRequest = signRequestService.getSignRequestByComment(comment);
        if(comment.getSpot()) {
            signRequest.getSignRequestParams().remove(comment.getStepNumber() - 1);
            signRequest.getParentSignBook().getLiveWorkflow().getLiveWorkflowSteps().get(comment.getStepNumber() - 1).setSignRequestParams(null);
        }
        signRequest.getComments().remove(comment);
        commentRepository.delete(comment);
    }

}
