package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Comment;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class CommentService {

    @Resource
    private CommentRepository commentRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private UserService userService;

    @Transactional
    public Comment create(Long signRequestId, String text, Integer posX, Integer posY, Integer pageNumer, Integer stepNumber, Boolean spot, String userEppn) {
        User user = userService.getUserByEppn(userEppn);
        SignRequest signRequest = signRequestService.getById(signRequestId);
        Comment comment = new Comment();
        comment.setText(text);
        comment.setCreateBy(user);
        comment.setPosX(posX);
        comment.setPosY(posY);
        comment.setPageNumber(pageNumer);
        comment.setStepNumber(stepNumber);
        comment.setSpot(spot);
        commentRepository.save(comment);
        signRequest.getComments().add(comment);
        return comment;
    }

    @Transactional
    public Long deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).get();
        SignRequest signRequest = signRequestService.getSignRequestByComment(comment);
        if(comment.getSpot()) {
            signRequest.getSignRequestParams().get(comment.getStepNumber() - 1).setxPos(0);
            signRequest.getSignRequestParams().get(comment.getStepNumber() - 1).setyPos(0);
        }
        signRequest.getComments().remove(comment);
        commentRepository.delete(comment);
        return signRequest.getId();
    }

}
