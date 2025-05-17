package kh.link_up.config;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebListener
public class SessionListner implements HttpSessionListener {

    public SessionListner() {
        log.info("SessionListner initialized");
    }

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        log.info("Session created: {}", se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        log.info("Session destroyed: {}", se.getSession().getId());
    }
}


