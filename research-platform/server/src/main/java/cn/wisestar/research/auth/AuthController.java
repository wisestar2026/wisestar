package cn.wisestar.research.auth;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.wisestar.research.auth.dto.LoginRequest;
import cn.wisestar.research.auth.dto.LoginResponse;
import cn.wisestar.research.common.R;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@Value("${research.auth.cookie-name:research-token}")
	private String cookieName;

	@Value("${research.auth.cookie-path:/}")
	private String cookiePath;

	@PostMapping("/login")
	public ResponseEntity<R<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
		LoginResponse response = authService.login(request);
		ResponseCookie cookie = ResponseCookie.from(cookieName, response.token()).path(cookiePath).httpOnly(true)
				.sameSite("Lax").maxAge(authService.cookieMaxAgeSeconds()).build();
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(R.ok(response));
	}

	@GetMapping("/me")
	public R<LoginResponse.UserView> me() {
		return R.ok(authService.view(UserContext.require()));
	}

	@PostMapping("/logout")
	public ResponseEntity<R<Void>> logout() {
		ResponseCookie cookie = ResponseCookie.from(cookieName, "").path(cookiePath).httpOnly(true).maxAge(0).build();
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(R.ok());
	}

}
