package com.example.todo.controller;

import com.example.todo.domain.Todo;
import com.example.todo.service.Todoservice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
public class TodoController {

    private final Todoservice todoService;

    public TodoController(Todoservice todoService) {
        this.todoService = todoService;
    }

    // 목록 + 검색
    @GetMapping("/todo")
    public String list(@RequestParam(required = false) String q, Model model) {

        List<Todo> todos;

        if (q == null || q.isBlank()) {
            todos = todoService.findAll();
        } else {
            todos = todoService.search(q);
        }

        model.addAttribute("todos", todos);
        return "todo";
    }
    @PostMapping("/todo/reorder")
    @ResponseBody
    public void reorder(@RequestBody List<Long> ids){

        todoService.reorder(ids);

    }
    @GetMapping("/todo/today")
    public String today(Model model){

        model.addAttribute("todos", todoService.todayTasks());

        return "todo";

    }
    @GetMapping("/todo/week")
    public String week(Model model){

        model.addAttribute("todos", todoService.weekTasks());

        return "todo";

    }
    @GetMapping("/todo/project/{name}")
    public String project(@PathVariable String name, Model model){

        model.addAttribute("todos", todoService.findByProject(name));

        return "todo";

    }
    // 추가
    @PostMapping("/todo/add")
    public String add(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String repeatType,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String tags
    ) {

        if (deadline == null) deadline = LocalDate.now();

        todoService.add(
                title,
                description,
                deadline,
                priority,
                color,
                repeatType,
                project,
                parentId,
                tags
        );

        return "redirect:/todo";
    }

    // 5) 일정 수정
    @PostMapping("/todo/update/{id}")
    public String update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description
    ) {

        Todo todo = todoService.findById(id);
        todo.setTitle(title);
        todo.setDescription(description);

        todoService.save(todo);

        return "redirect:/todo";
    }

    @PostMapping("/todo/delete/{id}")
    public String delete(@PathVariable Long id) {
        todoService.delete(id);
        return "redirect:/todo";
    }

    @PostMapping("/todo/toggle/{id}")
    public String toggle(@PathVariable Long id) {
        todoService.toggle(id);
        return "redirect:/todo";
    }
    @PostMapping("/todo/deleteRepeat")
    public String deleteRepeat(

            @RequestParam String title,
            @RequestParam String repeatType

    ) {

        todoService.deleteRepeat(title, repeatType);

        return "redirect:/todo";
    }
    @PostMapping("/todoNew")
    public String addFromCalendar(

            @RequestParam String title,
            @RequestParam(required = false) String description,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dueDate,

            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String repeatType,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String tags
    ) {

        if (repeatType == null || repeatType.equals("NONE")) {

            todoService.add(
                    title,
                    description,
                    dueDate,
                    priority,
                    color,
                    "NONE",
                    project,
                    parentId,
                    tags
            );

        }

        else if (repeatType.equals("DAILY")) {

            LocalDate endDate = dueDate.plusYears(1);

            LocalDate current = dueDate;

            while (!current.isAfter(endDate)) {

                todoService.add(
                        title,
                        description,
                        current,
                        priority,
                        color,
                        "NONE",
                        project,
                        parentId,
                        tags
                );

                current = current.plusDays(1);
            }

        }

        else if (repeatType.equals("WEEKLY")) {

            LocalDate endDate = dueDate.plusYears(1);

            LocalDate current = dueDate;

            while (!current.isAfter(endDate)) {

                todoService.add(
                        title,
                        description,
                        current,
                        priority,
                        color,
                        "NONE",
                        project,
                        parentId,
                        tags
                );

                current = current.plusWeeks(1);
            }

        }

        else if (repeatType.equals("MONTHLY")) {

            LocalDate endDate = dueDate.plusYears(1);

            LocalDate current = dueDate;

            while (!current.isAfter(endDate)) {

                todoService.add(
                        title,
                        description,
                        current,
                        priority,
                        color,
                        "NONE",
                        project,
                        parentId,
                        tags
                );

                current = current.plusMonths(1);
            }

        }

        return "redirect:/calendar";
    }
}