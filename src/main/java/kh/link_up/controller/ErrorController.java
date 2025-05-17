package kh.link_up.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/err")
public class ErrorController {

    @GetMapping("/denied-page")
    public String deniedPage(HttpServletRequest request, Model model) {
        System.out.println("denied-page들어옴");
        // 메시지와 리다이렉트할 페이지를 모델에 담아서 전달
        String msg = (String) request.getAttribute("msg");
        model.addAttribute("msg", msg);

        return "denied-page";  // denied-page.html을 반환
    }
}
