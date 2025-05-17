package kh.link_up.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import kh.link_up.service.CommentNotificationSubscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Slf4j
public class RedisConfig {
/*
* 기본으로 redis에 cacheName::key 형태로 구분되어 저장됨. key를 생략할 경우, "SimpleKey []" 로 저장됨.
1. @Cacheable: 읽을때.
	-예시: @Cacheput 참고.
2. @CachePut: 갱신
	-예시: @Cacheable(value="photo", key="#file.fileID", condition="#file.fileName='test'", unless="#result == null", cacheManager="gsonCacheManager")
3. @CacheEvict: 삭제(cacheManager 지정하면 안됨!)
	-예시: @CacheEvict(value="photo", key="#file.fileID")
4. @Caching: 한 메소드에 여러 어노테이션이 필요할때 그룹화 해줌.
	-예시: @Caching( evict= { @CacheEvict(...), @CacheEvict(...) }, ... )
5. 어노테이션 외에 직접 캐시매니저를 통해 캐시 접근이 필요한 경우
	-서비스 class에서 @Autowired private CacheManager cacheManager; 선언
    -함수 안에서 cacheManager.getCache("cacheName").evict("key") 처럼 처리하면 됨.
*/

    @Bean
    RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
                                                 MessageListenerAdapter listenerAdapter,
                                                 CommentNotificationSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapter, new PatternTopic("comment_notifications"));
        log.debug("Redis listener container initialized");
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(CommentNotificationSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleMessage");
    }

    // Redis 연결 팩토리 설정 (기본 설정으로도 가능)
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 연결 방식 설정 (예: Lettuce 사용)
        return new LettuceConnectionFactory("localhost", 6379);
    }

    // RedisTemplate 설정: Redis와의 통신을 위한 설정
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // Jackson의 ObjectMapper를 사용하여 직렬화 및 역직렬화 기능을 제공
        ObjectMapper objectMapper = new ObjectMapper();

        // LocalDateTime을 직렬화할 수 있도록 JavaTimeModule 등록
        objectMapper.registerModule(new JavaTimeModule());

        // LocalDateTime을 Timestamp로 저장하지 않고 ISO-8601 형식으로 저장하도록 설정
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 객체의 타입 정보를 JSON에 포함시켜서 역직렬화 시에 정확한 타입을 파악하도록 설정
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // GenericJackson2JsonRedisSerializer는 Jackson 라이브러리로
        // 직렬화된 데이터를 Redis에 저장할 수 있게 해주는 직렬화기
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // RedisTemplate은 Redis와 데이터를 주고받기 위한 핵심 클래스
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);  // Redis 연결 팩토리 설정
        redisTemplate.setKeySerializer(new StringRedisSerializer());  // 키는 문자열 형태로 직렬화
        redisTemplate.setValueSerializer(serializer);  // 값은 JSON 형식으로 직렬화
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());  // 해시의 키도 문자열로 직렬화
        redisTemplate.setHashValueSerializer(serializer);  // 해시의 값도 JSON 형식으로 직렬화

        // RedisTemplate 초기화
        redisTemplate.afterPropertiesSet();  // RedisTemplate 초기화 후 사용할 준비 완료

        return redisTemplate;  // 설정된 RedisTemplate 반환
    }

    // RedisCacheManager 설정: 캐시를 Redis에 저장하기 위한 설정
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // Jackson의 ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();

        // LocalDateTime 등의 Java 8 날짜 타입을 직렬화/역직렬화할 수 있도록 JavaTimeModule 등록
        objectMapper.registerModule(new JavaTimeModule());

        // LocalDateTime을 Timestamp로 저장하지 않고 ISO-8601 형식으로 저장하도록 설정
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 타입 정보를 JSON에 포함시켜서 역직렬화 시 정확한 타입 매핑을 위해 설정
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // GenericJackson2JsonRedisSerializer를 사용하여 JSON 형식으로 직렬화
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // Redis 캐시 구성 설정
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))  // 캐시 키를 문자열로 직렬화
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))  // 캐시 값은 JSON 형식으로 직렬화
                .entryTtl(Duration.ofMinutes(10));  // 캐시의 TTL(Time-to-live)을 10분으로 설정

        // RedisCacheManager는 Redis를 캐시 저장소로 사용하기 위한 매니저 클래스
        return RedisCacheManager
                .builder(factory)  // Redis 연결 팩토리 설정
                .cacheDefaults(cacheConfig)  // 기본 캐시 설정 적용
                .build();  // 캐시 매니저 생성
    }
}


