package kh.link_up.controller;

import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/err")
@Slf4j
@Tag(name = "Error", description = "에러페이지 관련 API") 
public class ErrorController {

    @Operation(summary = "접근 거부 페이지", description = "접근이 거부되었을 때 보여주는 페이지로 메시지를 전달합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "접근 거부 페이지 정상 반환")
    })
    @GetMapping("/denied-page")
    public String deniedPage(HttpServletRequest request, Model model) {
        String msg = (String) request.getAttribute("msg");
        log.info("error message : {}", msg);
        if (msg == null) {
            msg = (String) request.getSession().getAttribute("msg");
            log.info("anonymous error message : {}", msg);
            if (msg != null) {
                request.getSession().removeAttribute("msg");
            }
        }
        model.addAttribute("msg", msg);
        return "denied-page";
    }
}
