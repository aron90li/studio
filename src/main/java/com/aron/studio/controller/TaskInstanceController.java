package com.aron.studio.controller;

import com.aron.studio.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/taskInstance")
public class TaskInstanceController {

    @Autowired
    private TaskService taskService;


}
