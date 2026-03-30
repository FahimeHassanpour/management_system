package com.management.controllers

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class ManagerController {

    @GetMapping("/manager/dashboard")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    fun managerDashboard(): String = "manager/dashboard"
}
