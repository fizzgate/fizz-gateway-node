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

package we.plugin.jwtAuth;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;
import we.plugin.jwt.JwtAuthPluginFilter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 
 * @author Francis Dong
 *
 */
public class JwtAuthPluginFilterTests {

	private JwtAuthPluginFilter plugin = new JwtAuthPluginFilter();

	@Test
	public void testVerify() {

		String secretKey = "123456";
		
		String rsaPublicKey = "-----BEGIN PUBLIC KEY-----\n"
				+ "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuGbXWiK3dQTyCbX5xdE4\n"
				+ "yCuYp0AF2d15Qq1JSXT/lx8CEcXb9RbDddl8jGDv+spi5qPa8qEHiK7FwV2KpRE9\n"
				+ "83wGPnYsAm9BxLFb4YrLYcDFOIGULuk2FtrPS512Qea1bXASuvYXEpQNpGbnTGVs\n"
				+ "WXI9C+yjHztqyL2h8P6mlThPY9E9ue2fCqdgixfTFIF9Dm4SLHbphUS2iw7w1JgT\n"
				+ "69s7of9+I9l5lsJ9cozf1rxrXX4V1u/SotUuNB3Fp8oB4C1fLBEhSlMcUJirz1E8\n"
				+ "AziMCxS+VrRPDM+zfvpIJg3JljAh3PJHDiLu902v9w+Iplu1WyoB2aPfitxEhRN0\n"
				+ "YwIDAQAB\n"
				+ "-----END PUBLIC KEY-----";
		
		String ecPublicKey = "-----BEGIN PUBLIC KEY-----\n"
				+ "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEEVs/o5+uQbTjL3chynL4wXgUg2R9\n"
				+ "q9UU8I5mEovUf86QZ7kOBIjJwqnzD1omageEHWwHdBO6B+dFabmdT9POxg==\n"
				+ "-----END PUBLIC KEY-----";
		
		// HS256
		String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.keH6T3x1z7mmhKL1T3r9sQdAxxdzB6siemGMr_6ZOwU";
		DecodedJWT jwt = plugin.verify(token, secretKey, null);
		assertNotNull(jwt);

		// RS256
		token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.FsPP7skBe3RgWESameUrpffWSapIIkx4gWAuLPumsFNu4Kqzekt0eyiyGaxHjicBY4UhAlvbSo7GzBANO40x3fkEkpA2YnigOMH9CB4qTzehhg0liMhPuqAmmtKMLpJzT5dboixRjY316KSsrtY6LSJgModG4K21-zufJ-AJZS9bOaBNo_5TKbHRI-vZ0I4QFDjVDZxpsDITe2FOSc4uIdaXns67ZUlvjoDXeAgaMCZtDUxxR2j7s_jefajUQPHt8lc2eecdD8S91RTt3lboFKnWO9r5Ygvl21mZo_WjEyVs01XU4Zd5Pk-B8aH2B8d2MkG7wyPaX-q-cD4mOEsRvA";
		jwt = plugin.verify(token, secretKey, rsaPublicKey);
		assertNotNull(jwt);
		
		// ES256
		token = "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.tyh-VfuzIxCyGYDlkBA7DfyjrqmSHu6pQ2hoZuFqUSLPNY2N0mpHb3nk5K17HWP_3cYHBw7AhHale5wky6-sVA";
		jwt = plugin.verify(token, secretKey, ecPublicKey);
		assertNotNull(jwt);
		
	}

}
