/*
 *  Copyright (C) 2021 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fizzgate.plugin.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fizzgate.plugin.PluginFilter;
import com.fizzgate.util.JacksonUtils;
import com.fizzgate.util.PemUtils;
import com.fizzgate.util.WebUtils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Francis Dong
 *
 */
@Component(JwtAuthPluginFilter.JWT_AUTH_PLUGIN_FILTER)
public class JwtAuthPluginFilter extends PluginFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthPluginFilter.class);

	public static final String JWT_AUTH_PLUGIN_FILTER = "jwtAuthPlugin";

	public static final String RSA = "RSA";

	public static final String EC = "EC";

	public static final String KEY = "key";
	public static final String PASS_HEADER = "passHeader";
	public static final String EXTRACT_CLAIMS = "extractClaims";
	public static final String STATUS_CODE = "statusCode";
	public static final String CONTENT_TYPE = "contentType";
	public static final String RESP_BODY = "respBody";

	public static final String JWT_CLAIMS = "jwt.claims";

	/**
	 * Plugin global custom config, example: <br/>
	 * <br/>
	 * {<br/>
	 * "secretKey": "secret key for HS256/HS384/HS512 Algorithm", <br/>
	 * "publicKey": "public key for RSA or ECDSA Algorithm" <br/>
	 * }<br/>
	 * <br/>
	 */
	private GlobalConfig globalConfig = null;

	private String fixedConfigCache = null;

	@SuppressWarnings("unchecked")
	@Override
	public Mono<Void> doFilter(ServerWebExchange exchange, Map<String, Object> config, String fixedConfig) {

		try {
			if (globalConfig == null || fixedConfigCache == null
					|| (fixedConfigCache != null && !fixedConfigCache.equals(fixedConfig))) {
				if (StringUtils.isNotBlank(fixedConfig)) {
					globalConfig = JacksonUtils.readValue(fixedConfig, GlobalConfig.class);
				} else {
					globalConfig = null;
				}
				fixedConfigCache = fixedConfig;
			}

			String secretKey = (String) config.get(KEY);
			String publicKey = (String) config.get(KEY);
			secretKey = StringUtils.isBlank(secretKey) ? globalConfig.getSecretKey() : secretKey;
			publicKey = StringUtils.isBlank(publicKey) ? globalConfig.getPublicKey() : publicKey;

			Boolean passHeader = null;
			List<Object> passHeaderList = (List<Object>) config.get(PASS_HEADER);
			if(passHeaderList != null && passHeaderList.size() > 0) {
				passHeader = (Boolean) passHeaderList.get(0);
			}
			Boolean extractClaims = null;
			List<Object> extractClaimsList = (List<Object>) config.get(EXTRACT_CLAIMS);
			if(extractClaimsList != null && extractClaimsList.size() > 0) {
				extractClaims = (Boolean) extractClaimsList.get(0);
			}
			Integer statusCode = (Integer) config.get(STATUS_CODE);
			String contentType = (String) config.get(CONTENT_TYPE);
			String respBody = (String) config.get(RESP_BODY);

			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", contentType);

			// JSON Web Token from header
			HttpHeaders reqHeaders = exchange.getRequest().getHeaders();
			// Auth header format: Bearer eyJhbG...
			String token = reqHeaders.getFirst(HttpHeaders.AUTHORIZATION);
			if (StringUtils.isNotBlank(token) && token.length() > 7
					&& token.substring(0, 7).equalsIgnoreCase("Bearer ")) {
				token = token.substring(7);
			} else {
				if (StringUtils.isBlank(token)) {
					log.warn("JWT Auth plugin - Token is missing");
					return WebUtils.responseErrorAndBindContext(exchange, JWT_AUTH_PLUGIN_FILTER,
							HttpStatus.valueOf(statusCode), headers, respBody);
				} else {
					log.warn("JWT Auth plugin - invalid token");
					return WebUtils.responseErrorAndBindContext(exchange, JWT_AUTH_PLUGIN_FILTER,
							HttpStatus.valueOf(statusCode), headers, respBody);
				}
			}

			DecodedJWT jwt = this.verify(token, secretKey, publicKey);
			if (jwt == null) {
				// failed
				return WebUtils.responseErrorAndBindContext(exchange, JWT_AUTH_PLUGIN_FILTER,
						HttpStatus.valueOf(statusCode), headers, respBody);
			} else {
				// passed
				// remove jwt header
				if (passHeader != null && !passHeader) {
					reqHeaders.remove(HttpHeaders.AUTHORIZATION);
				}

				if (extractClaims != null && extractClaims) {
					exchange.getAttributes().put(JWT_CLAIMS, jwt.getClaims());
				}
			}
			return WebUtils.transmitSuccessFilterResultAndEmptyMono(exchange, JWT_AUTH_PLUGIN_FILTER, null);
		} catch (Exception e) {
			log.error("JWT Auth plugin Exception", e);
			return WebUtils.responseErrorAndBindContext(exchange, JWT_AUTH_PLUGIN_FILTER,
					HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	/**
	 * Verify JWT
	 * 
	 * @param token
	 * @param secretKey key for HS256/HS384/HS512
	 * @param publicKey pub key for RSA or ECDSA
	 * @return
	 * @throws Exception
	 */
	public DecodedJWT verify(String token, String secretKey, String publicKey) {
		try {
			DecodedJWT jwt = JWT.decode(token);
			String alg = jwt.getAlgorithm();
			Algorithm algorithm = null;

			switch (alg) {
			case "HS256":
				algorithm = Algorithm.HMAC256(secretKey);
				break;
			case "HS384":
				algorithm = Algorithm.HMAC384(secretKey);
				break;
			case "HS512":
				algorithm = Algorithm.HMAC512(secretKey);
				break;
			case "RS256":
				algorithm = Algorithm.RSA256((RSAPublicKey) PemUtils.readPublicKeyFromString(publicKey, RSA), null);
				break;
			case "RS384":
				algorithm = Algorithm.RSA384((RSAPublicKey) PemUtils.readPublicKeyFromString(publicKey, RSA), null);
				break;
			case "RS512":
				algorithm = Algorithm.RSA512((RSAPublicKey) PemUtils.readPublicKeyFromString(publicKey, RSA), null);
				break;
			case "ES256":
				algorithm = Algorithm.ECDSA256((ECPublicKey) PemUtils.readPublicKeyFromString(publicKey, EC), null);
				break;
			case "ES256K":
				algorithm = Algorithm.ECDSA256K((ECPublicKey) PemUtils.readPublicKeyFromString(publicKey, EC), null);
				break;
			case "ES384":
				algorithm = Algorithm.ECDSA384((ECPublicKey) PemUtils.readPublicKeyFromString(publicKey, EC), null);
				break;
			case "ES512":
				algorithm = Algorithm.ECDSA512((ECPublicKey) PemUtils.readPublicKeyFromString(publicKey, EC), null);
				break;
			}

			if (algorithm == null) {
				// Algorithm NOT Supported
				log.warn("{} Algorithm NOT Supported", alg);
			} else {
				JWTVerifier verifier = JWT.require(algorithm).build();
				try {
					return verifier.verify(token);
				} catch (JWTVerificationException e) {
					// Verification failed
					log.warn("JWT verification failed: {}", e.getMessage());
				}
			}
		} catch (Exception e) {
			log.warn("JWT verification exception", e);
		}
		return null;
	}

}
