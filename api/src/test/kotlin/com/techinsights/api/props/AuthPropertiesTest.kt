package com.techinsights.api.props

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration

class AuthPropertiesTest {

    @Test
    fun `should bind valid properties`() {
        val jwt = AuthProperties.Jwt(
            secretKey = "this-is-a-very-secure-secret-key-for-testing-purposes-only",
            accessTokenExpiration = Duration.ofMinutes(15),
            refreshTokenExpiration = Duration.ofDays(7),
            cookieSecure = true
        )
        val props = AuthProperties(jwt = jwt)

        assertThat(props.jwt.secretKey).isEqualTo("this-is-a-very-secure-secret-key-for-testing-purposes-only")
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
        // We cannot test default constructor directly for failure if we enforce it in init, 
        // so we provide a valid key to test other defaults
        val jwt = AuthProperties.Jwt(secretKey = "this-is-a-very-secure-secret-key-for-testing-purposes-only")
        
        assertThat(jwt.accessTokenExpiration).isEqualTo(Duration.ofMinutes(30))
        assertThat(jwt.cookieSecure).isTrue()
    }
}
