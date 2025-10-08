package kh.link_up.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

        @Value("${file.upload-dir}")
        private String uploadDir; // 예: D:/LinkUpFileFolder

        @Override
        public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
                // Windows의 경우 "\" 대신 "/" 사용
                String boardPath = "file:" + uploadDir.replace("\\", "/") + "/(게시판)/";
                String linkUpPath = "file:" + uploadDir.replace("\\", "/") + "/(LinkUp)/";

                // 기존 매핑
                registry.addResourceHandler("/files/**")
                                .addResourceLocations(boardPath);

                // ✅ 추가: /file/** 요청도 매핑
                registry.addResourceHandler("/file/**")
                                .addResourceLocations(boardPath);

                // 추가: /notion/images/** 매핑
                registry.addResourceHandler("/notion/images/**")
                                .addResourceLocations(linkUpPath);
        }
}
