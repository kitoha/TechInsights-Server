package com.techinsights.api.props

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.Base64

class AuthPropertiesTest {

    private val validBase64Key = Base64.getEncoder()
        .encodeToString("this-is-a-very-secure-secret-key!!".toByteArray())

    @Test
    fun `should bind valid properties`() {
        val jwt = AuthProperties.Jwt(
            secretKey = validBase64Key,
            accessTokenExpiration = Duration.ofMinutes(15),
            refreshTokenExpiration = Duration.ofDays(7),
            cookieSecure = true
        )
        val props = AuthProperties(jwt = jwt)

        assertThat(props.jwt.secretKey).isEqualTo(validBase64Key)
        assertThat(props.jwt.accessTokenExpiration).isEqualTo(Duration.ofMinutes(15))
        assertThat(props.jwt.cookieSecure).isTrue()
    }

    @Test
    fun `should fail when secret key is empty`() {
        assertThatThrownBy {
            AuthProperties.Jwt(secretKey = "")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Secret key must not be empty")
    }

    @Test
    fun `should fail when secret key is too short`() {
        assertThatThrownBy {
            AuthProperties.Jwt(secretKey = "short-key")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Secret key must be at least 32 characters")
    }

    @Test
    fun `should have safe defaults`() {
        val jwt = AuthProperties.Jwt(secretKey = validBase64Key)

        assertThat(jwt.accessTokenExpiration).isEqualTo(Duration.ofMinutes(30))
        assertThat(jwt.cookieSecure).isTrue()
    }

    @Test
    fun `should fail when secret key is not valid Base64`() {
        assertThatThrownBy {
            AuthProperties.Jwt(secretKey = "this-is-not-base64-!@#\$%^&*()_+this-is-not-base64")
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Secret key must be valid Base64")
    }

    @Test
    fun `should pass when secret key is valid Base64 encoded`() {
        val jwt = AuthProperties.Jwt(secretKey = validBase64Key)
        assertThat(jwt.secretKey).isEqualTo(validBase64Key)
    }
}
