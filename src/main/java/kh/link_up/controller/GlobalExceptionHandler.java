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

    // 모든 예외를 처리하는 핸들러
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleAllExceptions(Exception ex, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView();

        // 에러 메시지를 리퀘스트 속성에 저장
        request.setAttribute("msg", "서버에 문제가 발생했습니다. 다시 시도해 주세요.");

        // denied-page로 리다이렉트
        mav.setViewName("redirect:/err/denied-page");
        return mav;
    }

    // 400 Bad Request 처리
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView();
        request.setAttribute("msg", "잘못된 요청입니다. 요청을 다시 확인해 주세요.");
        mav.setViewName("redirect:/err/denied-page");
        return mav;
    }

    // 403 Forbidden 처리
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleSecurityException(SecurityException ex, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView();
        request.setAttribute("msg", "접근 권한이 없습니다.");
        mav.setViewName("redirect:/err/denied-page");
        return mav;
    }

    // 404 Not Found 처리
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(HttpServletRequest request) {
        ModelAndView mav = new ModelAndView();
        request.setAttribute("msg", "페이지를 찾을 수 없습니다.");
        mav.setViewName("redirect:/err/denied-page");
        return mav;
    }

}