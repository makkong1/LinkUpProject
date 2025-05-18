package kh.link_up.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/err")
@Slf4j
public class ErrorController {

    @GetMapping("/denied-page")
    public String deniedPage(HttpServletRequest request, Model model) {
        // 1. 우선 request attribute에서 메시지를 가져온다
        String msg = (String) request.getAttribute("msg");
        log.info("error message : {}", msg);
        // 2. request에 없으면 session attribute에서 메시지를 가져온다
        if (msg == null) {
            msg = (String) request.getSession().getAttribute("msg");
            log.info("anonymous error message : {}", msg);
            if (msg != null) {
                // 세션에 있으면 가져오고 나서 지운다
                request.getSession().removeAttribute("msg");
            }
        }

        // 3. 모델에 메시지 전달
        model.addAttribute("msg", msg);

        return "denied-page";
    }

}
