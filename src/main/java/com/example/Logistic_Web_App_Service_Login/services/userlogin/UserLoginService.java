package com.example.Logistic_Web_App_Service_Login.services.userlogin;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.Logistic_Web_App_Service_Login.components.JwtTokenUtil;
import com.example.Logistic_Web_App_Service_Login.dtos.UserLoginDTO;
import com.example.Logistic_Web_App_Service_Login.enums.StatusRole;
import com.example.Logistic_Web_App_Service_Login.exceptions.DataNotFoundException;
import com.example.Logistic_Web_App_Service_Login.exceptions.ExpiredTokenException;
import com.example.Logistic_Web_App_Service_Login.exceptions.InvalidPasswordException;
import com.example.Logistic_Web_App_Service_Login.mappers.UserLoginMapper;
import com.example.Logistic_Web_App_Service_Login.models.Role;
import com.example.Logistic_Web_App_Service_Login.models.Token;
import com.example.Logistic_Web_App_Service_Login.models.User;
import com.example.Logistic_Web_App_Service_Login.models.UserLogin;
import com.example.Logistic_Web_App_Service_Login.repositories.TokenRepository;
import com.example.Logistic_Web_App_Service_Login.repositories.UserLoginRepository;
import com.example.Logistic_Web_App_Service_Login.services.role.RoleService;
import com.example.Logistic_Web_App_Service_Login.services.user.UserService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserLoginService implements IUserLoginService {
	private final UserLoginRepository userLoginRepository;
	private final AuthenticationManager authenticationManager;

	@Autowired
	UserLoginMapper userLoginMapper;

	private final UserService userService;
	private final JwtTokenUtil jwtTokenUtil;
	private final TokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;

	private final RoleService roleService;

	@Override
	@Transactional
	public UserLogin createUserLogin(UserLoginDTO userLoginDTO) throws Exception {
		String username = userLoginDTO.getUserName();

		if (userLoginRepository.existsByUsername(username)) {
			throw new DataIntegrityViolationException("Username exist");
		}

		User user = userService.getUserById(userLoginDTO.getUserId());

		Role role = roleService.getRoleById(userLoginDTO.getRoleId());

		if (role.getName().toUpperCase().equals(StatusRole.ADMIN)) {
			throw new Exception("Not create ADMIN");
		}

		String password = userLoginDTO.getPassword();
		String encodedPassword = passwordEncoder.encode(password);

		UserLogin newUserLogin = userLoginMapper.mapToUserLoginEntity(userLoginDTO);
		newUserLogin.setPassword(encodedPassword);
		newUserLogin.setUser(user);
		newUserLogin.setRole(role);

		return userLoginRepository.save(newUserLogin);
	}

	@Override
	public UserLogin getUserLoginById(Long userLoginId) {
		return userLoginRepository.findById(userLoginId)
				.orElseThrow(() -> new RuntimeException(String.format("User with id = %d not found", userLoginId)));
	}

	@Override
	public String login(String userName, String password, String loginType) throws Exception {
		Optional<UserLogin> optionalUserLogin = userLoginRepository.findByUsernameAndLoginType(userName, loginType);

		if (optionalUserLogin.isEmpty()) {
			throw new DataNotFoundException("Invalid phone number or password");
		}

		UserLogin existingUserLogin = optionalUserLogin.get();

		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userName,
				password);
		
		authenticationManager.authenticate(authenticationToken);

		return jwtTokenUtil.generateToken(existingUserLogin);
	}

	@Override
	public void resetPassword(Long userLoginId, String newPassword)
			throws InvalidPasswordException, DataNotFoundException {
		UserLogin existingUserLogin = getUserLoginById(userLoginId);

		String encodedPassword = passwordEncoder.encode(newPassword);
		existingUserLogin.setPassword(encodedPassword);
		userLoginRepository.save(existingUserLogin);
		// reset password => clear token
		List<Token> tokens = tokenRepository.findByUserLogin(existingUserLogin);
		for (Token token : tokens) {
			tokenRepository.delete(token);
		}
	}

	@Override
	public UserLogin getUserLoginDetailsFromToken(String token) throws Exception {
		if(jwtTokenUtil.isTokenExpired(token)) {
            throw new ExpiredTokenException("Token is expired");
        }
        String username = jwtTokenUtil.extractUsername(token);
        Optional<UserLogin> userLogin = userLoginRepository.findByUsername(username);

        if (userLogin.isPresent()) {
            return userLogin.get();
        } else {
            throw new Exception("User not found");
        }
	}

	@Override
	public UserLogin getUserLoginDetailsFromRefreshToken(String refreshToken) throws Exception {
		Token existingToken = tokenRepository.findByRefreshToken(refreshToken);
		
        return getUserLoginDetailsFromToken(existingToken.getToken());
	}

}
