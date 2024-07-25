package org.example;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MainTest {

	@Test
	void singleEntry() {
		String result = Main.replace(
				"""
						[[servlet-authentication-userdetails]]
						= UserDetails

						{security-api-url}org/springframework/security/core/userdetails/UserDetails.html[`UserDetails`] is returned by the xref:servlet/authentication/passwords/user-details-service.adoc#servlet-authentication-userdetailsservice[`UserDetailsService`].
						The xref:servlet/authentication/passwords/dao-authentication-provider.adoc#servlet-authentication-daoauthenticationprovider[`DaoAuthenticationProvider`] validates the `UserDetails` and then returns an xref:servlet/authentication/architecture.adoc#servlet-authentication-authentication[`Authentication`] that has a principal that is the `UserDetails` returned by the configured `UserDetailsService`.""")
			.replacement();

		assertThat(result).isEqualTo(
				"""
						[[servlet-authentication-userdetails]]
						= UserDetails

						javadoc:org.springframework.security.core.userdetails.UserDetails[] is returned by the xref:servlet/authentication/passwords/user-details-service.adoc#servlet-authentication-userdetailsservice[`UserDetailsService`].
						The xref:servlet/authentication/passwords/dao-authentication-provider.adoc#servlet-authentication-daoauthenticationprovider[`DaoAuthenticationProvider`] validates the `UserDetails` and then returns an xref:servlet/authentication/architecture.adoc#servlet-authentication-authentication[`Authentication`] that has a principal that is the `UserDetails` returned by the configured `UserDetailsService`.""");
	}

	@Test
	void multipleMatches() {
		String result = Main
			.replace(
					"""
							[[servlet-authentication-form]]
							= Form Login
							:figures: servlet/authentication/unpwd

							This is something {security-api-url}org/springframework/security/core/userdetails/UserDetails.html[`UserDetails`].

							This is another entry {security-api-url}org/springframework/security/core/Authentication.html[`Authentication`].

							Something at the end.""")
			.replacement();

		assertThat(result).isEqualTo("""
				[[servlet-authentication-form]]
				= Form Login
				:figures: servlet/authentication/unpwd

				This is something javadoc:org.springframework.security.core.userdetails.UserDetails[].

				This is another entry javadoc:org.springframework.security.core.Authentication[].

				Something at the end.""");
	}

	@Test
	void multipleMatchSingleLine() {
		String result = Main
			.replace(
					"""
							[[servlet-authentication-form]]
							= Form Login
							:figures: servlet/authentication/unpwd

							This is something {security-api-url}org/springframework/security/core/userdetails/UserDetails.html[`UserDetails`]. This is another entry {security-api-url}org/springframework/security/core/Authentication.html[`Authentication`].

							Something at the end.""")
			.replacement();

		assertThat(result).isEqualTo(
				"""
						[[servlet-authentication-form]]
						= Form Login
						:figures: servlet/authentication/unpwd

						This is something javadoc:org.springframework.security.core.userdetails.UserDetails[]. This is another entry javadoc:org.springframework.security.core.Authentication[].

						Something at the end.""");
	}

	@Test
	void uniqueText() {
		String result = Main
			.replace(
					"""
							[[servlet-authentication-form]]
							= Form Login
							:figures: servlet/authentication/unpwd

							This is another entry {security-api-url}org/springframework/security/core/Authentication.html[Click `Authentication`].

							Something at the end.""")
			.replacement();

		assertThat(result).isEqualTo("""
				[[servlet-authentication-form]]
				= Form Login
				:figures: servlet/authentication/unpwd

				This is another entry javadoc:org.springframework.security.core.Authentication[Click `Authentication`].

				Something at the end.""");
	}

	@Test
	void anchor() {
		String content = """
				Calling `{security-api-url}org/springframework/security/oauth2/jwt/ReactiveJwtDecoders.html#fromIssuerLocation-java.lang.String-[ReactiveJwtDecoders#fromIssuerLocation]` invokes the Provider Configuration or Authorization Server Metadata endpoint to derive the JWK Set URI.
				If the application does not expose a `ReactiveJwtDecoder` bean, Spring Boot exposes the above default one.""";

		String result = Main.replace(content).replacement();
		assertThat(result).isEqualTo(
				"""
						Calling `javadoc:org.springframework.security.oauth2.jwt.ReactiveJwtDecoders#fromIssuerLocation-java.lang.String-[ReactiveJwtDecoders#fromIssuerLocation]` invokes the Provider Configuration or Authorization Server Metadata endpoint to derive the JWK Set URI.
						If the application does not expose a `ReactiveJwtDecoder` bean, Spring Boot exposes the above default one.""");
	}

	@Test
	void annotation() {
		String result = Main.replace(
				"""
						[[servlet-authentication-userdetails]]
						= UserDetails

						{security-api-url}org/springframework/security/core/userdetails/UserDetails.html[`@UserDetails`] is returned by the xref:servlet/authentication/passwords/user-details-service.adoc#servlet-authentication-userdetailsservice[`UserDetailsService`].
						The xref:servlet/authentication/passwords/dao-authentication-provider.adoc#servlet-authentication-daoauthenticationprovider[`DaoAuthenticationProvider`] validates the `UserDetails` and then returns an xref:servlet/authentication/architecture.adoc#servlet-authentication-authentication[`Authentication`] that has a principal that is the `UserDetails` returned by the configured `UserDetailsService`.""")
			.replacement();

		assertThat(result).isEqualTo(
				"""
						[[servlet-authentication-userdetails]]
						= UserDetails

						javadoc:org.springframework.security.core.userdetails.UserDetails[format=annotation] is returned by the xref:servlet/authentication/passwords/user-details-service.adoc#servlet-authentication-userdetailsservice[`UserDetailsService`].
						The xref:servlet/authentication/passwords/dao-authentication-provider.adoc#servlet-authentication-daoauthenticationprovider[`DaoAuthenticationProvider`] validates the `UserDetails` and then returns an xref:servlet/authentication/architecture.adoc#servlet-authentication-authentication[`Authentication`] that has a principal that is the `UserDetails` returned by the configured `UserDetailsService`.""");
	}

}
