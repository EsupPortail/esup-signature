package org.esupportail.esupsignature.service;

import jakarta.annotation.Resource;
import org.esupportail.esupsignature.entity.Message;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.MessageRepository;
import org.esupportail.esupsignature.service.security.HtmlSanitizerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class MessageService {

    @Resource
    private MessageRepository messageRepository;

    @Resource
    private UserService userService;

    @Resource
    private HtmlSanitizerService htmlSanitizerService;

    public void createMessage(String endDate, String text) throws ParseException {
        Message message = new Message();
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);
        message.setEndDate(date);
        message.setText(htmlSanitizerService.sanitize(text));
        messageRepository.save(message);
    }

    @Transactional
    public void deleteMessage(Long id) {
        messageRepository.deleteById(id);
    }

    public Page<Message> getAll(Pageable pageable) {
        Page<Message> messages = messageRepository.findAll(pageable);
        messages.forEach(this::sanitizeMessage);
        return messages;
    }

    public List<Message> getByUserNeverRead(User user) {
        return sanitizeMessages(messageRepository.findByUsersNotContainsAndEndDateAfter(user, new Date()));
    }

    public List<Message> getByUserAlreadyRead(User user) {
        return sanitizeMessages(messageRepository.findByUsersContains(user));
    }

    @Transactional
    public void disableMessageForUser(String authUserEppn, long id) {
        User authUser = userService.getByEppn(authUserEppn);
        Message message = messageRepository.findById(id).get();
        message.getUsers().add(authUser);
    }

    @Transactional
    public void anonymize(User user) {
        for(Message message : getByUserAlreadyRead(user)) {
            message.getUsers().remove(user);
        }
    }

    private List<Message> sanitizeMessages(List<Message> messages) {
        messages.forEach(this::sanitizeMessage);
        return messages;
    }

    private void sanitizeMessage(Message message) {
        if(message != null) {
            message.setText(htmlSanitizerService.sanitize(message.getText()));
        }
    }

}
