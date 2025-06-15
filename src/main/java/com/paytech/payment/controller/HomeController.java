package com.paytech.payment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.extern.slf4j.Slf4j;

import static com.paytech.payment.constant.Constants.REDIRECT_PAYMENT;

@Controller
@Slf4j
public class HomeController {

    @GetMapping("/")
    public String home() {
        return REDIRECT_PAYMENT;
    }
}