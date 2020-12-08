package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.Message;
import org.esupportail.esupsignature.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.springframework.data.domain.Pageable;

@Service
public class MessageService {

    @Resource
    private MessageRepository messageRepository;

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

}
