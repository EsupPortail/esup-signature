package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Message;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
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

    public Message createMessage(String endDate, String text) throws ParseException {
        Message message = new Message();
        Date date = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);
        message.setEndDate(date);
        message.setText(text);
        messageRepository.save(message);
        return message;
    }

    public void deleteMessage(Long id) {
        messageRepository.deleteById(id);
    }

    public Page<Message> getAll(Pageable pageable) {
        return messageRepository.findAll(pageable);
    }

    public List<Message> getByUser(User user) {
        return messageRepository.findByUsersNotContainsAndEndDateAfter(user, new Date());
    }

    @Transactional
    public void disableMessageForUser(String authUserEppn, long id) {
        User authUser = userService.getByEppn(authUserEppn);
        Message message = messageRepository.findById(id).get();
        message.getUsers().add(authUser);
    }

}
