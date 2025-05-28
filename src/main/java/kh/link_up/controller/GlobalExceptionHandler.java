package kh.link_up.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleAllExceptions(Exception ex, HttpServletRequest request) {
        request.setAttribute("msg", "서버에 문제가 발생했습니다. 다시 시도해 주세요.");
        request.setAttribute("action", "home");
        return new ModelAndView("redirect:/err/denied-page");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        request.setAttribute("msg", "잘못된 요청입니다. 요청을 다시 확인해 주세요.");
        request.setAttribute("action", "back");
        return new ModelAndView("redirect:/err/denied-page");
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleSecurityException(SecurityException ex, HttpServletRequest request) {
        request.setAttribute("msg", "접근 권한이 없습니다. 로그인 후 이용해 주세요.");
        request.setAttribute("action", "login");
        return new ModelAndView("redirect:/err/denied-page");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(HttpServletRequest request) {
        request.setAttribute("msg", "페이지를 찾을 수 없습니다.");
        request.setAttribute("action", "home");
        return new ModelAndView("redirect:/err/denied-page");
    }
}