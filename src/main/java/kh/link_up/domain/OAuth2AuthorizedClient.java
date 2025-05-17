package kh.link_up.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "oauth2_authorized_client")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2AuthorizedClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String clientRegistrationId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String principalName;

    @Column(nullable = false, length = 100)
    private String accessTokenType;

    @Lob
    @Column(nullable = false)
    private byte[] accessTokenValue;

    @Column(nullable = false)
    private Timestamp accessTokenIssuedAt;

    @Column(nullable = false)
    private Timestamp accessTokenExpiresAt;

    @Column(length = 1000)
    private String accessTokenScopes;

    @Lob
    private byte[] refreshTokenValue;

    private Timestamp refreshTokenIssuedAt;

    @CreationTimestamp
    private Timestamp createdAt;

    // getters and setters
}