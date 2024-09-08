package com.example.EmailSender1.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class EmailService {

    private static final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public static void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
    }

    public static void removeEmitter(SseEmitter emitter) {
        emitters.remove(emitter);
    }

    public static void sendEmailProgress(String message) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("email-progress").data(message));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}