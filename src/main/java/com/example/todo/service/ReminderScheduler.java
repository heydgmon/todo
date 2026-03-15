package com.example.todo.service;

import com.example.todo.domain.Todo;
import com.example.todo.repository.TodoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ReminderScheduler {

    private final TodoRepository repo;

    public ReminderScheduler(TodoRepository repo) {
        this.repo = repo;
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void reminder(){

        LocalDate today = LocalDate.now();

        repo.findAll().stream()
                .filter(t -> t.getRemindDate()!=null)
                .filter(t -> t.getRemindDate().isEqual(today))
                .forEach(t -> {

                    System.out.println("Reminder : "+t.getTitle());

                });

    }

}