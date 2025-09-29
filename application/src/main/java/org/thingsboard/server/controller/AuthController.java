/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.common.data.security.event.UserCredentialsInvalidationEvent;
import org.thingsboard.server.common.data.security.event.UserSessionInvalidationEvent;
import org.thingsboard.server.common.data.security.model.JwtPair;
import org.thingsboard.server.common.data.security.model.SecuritySettings;
import org.thingsboard.server.common.data.security.model.UserPasswordPolicy;
import org.thingsboard.server.config.annotations.ApiOperation;
import org.thingsboard.server.dao.settings.SecuritySettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.customer.TbCustomerService;
import org.thingsboard.server.service.entitiy.tenant.TbTenantService;
import org.thingsboard.server.service.entitiy.tenant.profile.TbTenantProfileService;
import org.thingsboard.server.service.entitiy.user.TbUserService;
import org.thingsboard.server.service.security.auth.rest.RestAuthenticationDetails;
import org.thingsboard.server.service.security.model.*;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.net.URI;
import java.util.UUID;

import static org.thingsboard.server.common.data.StringUtils.generateSafeToken;

@RestController
@TbCoreComponent
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class AuthController extends BaseController {

    @Value("${server.rest.rate_limits.reset_password_per_user:5:3600}")
    private String defaultLimitsConfiguration;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenFactory tokenFactory;
    private final MailService mailService;
    private final SystemSecurityService systemSecurityService;
    private final SecuritySettingsService securitySettingsService;
    private final RateLimitService rateLimitService;
    private final ApplicationEventPublisher eventPublisher;
    private final TbTenantService tbTenantService;
    private  final TbCustomerService tbCustomerService;
    private final TbTenantProfileService tbTenantProfileService;
    private final TbUserService tbUserService;
    private final UserService userService;
    private static final int DEFAULT_TOKEN_LENGTH = 30;


    @ApiOperation(value = "Get current User (getUser)",
            notes = "Get the information about the User which credentials are used to perform this REST API call.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @GetMapping(value = "/auth/user")
    public User getUser() throws ThingsboardException {
        SecurityUser securityUser = getCurrentUser();
        User user = userService.findUserById(securityUser.getTenantId(), securityUser.getId());
        checkDashboardInfo(user.getAdditionalInfo());
        return user;
    }

    @ApiOperation(value = "Logout (logout)",
            notes = "Special API call to record the 'logout' of the user to the Audit Logs. Since platform uses [JWT](https://jwt.io/), the actual logout is the procedure of clearing the [JWT](https://jwt.io/) token on the client side. ")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/auth/logout")
    public void logout(HttpServletRequest request) throws ThingsboardException {
        logLogoutAction(request);
    }

    @ApiOperation(value = "Change password for current User (changePassword)",
            notes = "Change the password for the User which credentials are used to perform this REST API call. Be aware that previously generated [JWT](https://jwt.io/) tokens will be still valid until they expire.")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @PostMapping(value = "/auth/changePassword")
    public JwtPair changePassword(@Parameter(description = "Change Password Request")
                                  @RequestBody ChangePasswordRequest changePasswordRequest) throws ThingsboardException {
        String currentPassword = changePasswordRequest.getCurrentPassword();
        String newPassword = changePasswordRequest.getNewPassword();
        SecurityUser securityUser = getCurrentUser();
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, securityUser.getId());
        if (!passwordEncoder.matches(currentPassword, userCredentials.getPassword())) {
            throw new ThingsboardException("Current password doesn't match!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        systemSecurityService.validatePassword(newPassword, userCredentials);
        if (passwordEncoder.matches(newPassword, userCredentials.getPassword())) {
            throw new ThingsboardException("New password should be different from existing!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        userCredentials.setPassword(passwordEncoder.encode(newPassword));
        userService.replaceUserCredentials(securityUser.getTenantId(), userCredentials);

        eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
        return tokenFactory.createTokenPair(securityUser);
    }

    @ApiOperation(value = "Get the current User password policy (getUserPasswordPolicy)",
            notes = "API call to get the password policy for the password validation form(s).")
    @GetMapping(value = "/noauth/userPasswordPolicy")
    public UserPasswordPolicy getUserPasswordPolicy() throws ThingsboardException {
        SecuritySettings securitySettings = checkNotNull(securitySettingsService.getSecuritySettings());
        return securitySettings.getPasswordPolicy();
    }

    @ApiOperation(value = "Check Activate User Token (checkActivateToken)",
            notes = "Checks the activation token and forwards user to 'Create Password' page. " +
                    "If token is valid, returns '303 See Other' (redirect) response code with the correct address of 'Create Password' page and same 'activateToken' specified in the URL parameters. " +
                    "If token is not valid, returns '409 Conflict'. " +
                    "If token is expired, redirects to error page.")
    @GetMapping(value = "/noauth/activate", params = {"activateToken"})
    public ResponseEntity<?> checkActivateToken(
            @Parameter(description = "The activate token string.")
            @RequestParam(value = "activateToken") String activateToken) {
        UserCredentials userCredentials = userService.findUserCredentialsByActivateToken(TenantId.SYS_TENANT_ID, activateToken);
        if (userCredentials == null) {
            return response(HttpStatus.CONFLICT);
        } else if (userCredentials.isActivationTokenExpired()) {
            return redirectTo("/activationLinkExpired");
        }
        return redirectTo("/login/createPassword?activateToken=" + activateToken);
    }

    @ApiOperation(value = "Request reset password email (requestResetPasswordByEmail)",
            notes = "Request to send the reset password email if the user with specified email address is present in the database. " +
                    "Always return '200 OK' status for security purposes.")
    @PostMapping(value = "/noauth/resetPasswordByEmail")
    public void requestResetPasswordByEmail(
            @Parameter(description = "The JSON object representing the reset password email request.")
            @RequestBody ResetPasswordEmailRequest resetPasswordByEmailRequest,
            HttpServletRequest request) throws ThingsboardException {
        try {
            String email = resetPasswordByEmailRequest.getEmail();
            UserCredentials userCredentials = userService.requestPasswordReset(TenantId.SYS_TENANT_ID, email);
            User user = userService.findUserById(TenantId.SYS_TENANT_ID, userCredentials.getUserId());
            String baseUrl = systemSecurityService.getBaseUrl(user.getTenantId(), user.getCustomerId(), request);
            String resetUrl = String.format("%s/api/noauth/resetPassword?resetToken=%s", baseUrl,
                    userCredentials.getResetToken());

            mailService.sendResetPasswordEmailAsync(resetUrl, userCredentials.getResetTokenTtl(), email);
        } catch (Exception e) {
            log.warn("Error occurred: {}", e.getMessage());
        }
    }

    @ApiOperation(value = "Check password reset token (checkResetToken)",
            notes = "Checks the password reset token and forwards user to 'Reset Password' page. " +
                    "If token is valid, returns '303 See Other' (redirect) response code with the correct address of 'Reset Password' page and same 'resetToken' specified in the URL parameters. " +
                    "If token is not valid, returns '409 Conflict'. " +
                    "If token is expired, redirects to error page.")
    @GetMapping(value = "/noauth/resetPassword", params = {"resetToken"})
    public ResponseEntity<?> checkResetToken(
            @Parameter(description = "The reset token string.")
            @RequestParam(value = "resetToken") String resetToken) {
        UserCredentials userCredentials = userService.findUserCredentialsByResetToken(TenantId.SYS_TENANT_ID, resetToken);
        if (userCredentials == null) {
            return response(HttpStatus.CONFLICT);
        } else if (userCredentials.isResetTokenExpired()) {
            return redirectTo("/passwordResetLinkExpired");
        }
        if (!rateLimitService.checkRateLimit(LimitedApi.PASSWORD_RESET, userCredentials.getUserId(), defaultLimitsConfiguration)) {
            return response(HttpStatus.TOO_MANY_REQUESTS);
        }
        return redirectTo("/login/resetPassword?resetToken=" + resetToken);
    }

    @ApiOperation(value = "Activate User",
            notes = "Checks the activation token and updates corresponding user password in the database. " +
                    "Now the user may start using his password to login. " +
                    "The response already contains the [JWT](https://jwt.io) activation and refresh tokens, " +
                    "to simplify the user activation flow and avoid asking user to input password again after activation. " +
                    "If token is valid, returns the object that contains [JWT](https://jwt.io/) access and refresh tokens. " +
                    "If token is not valid, returns '400 Bad Request'.")
    @PostMapping(value = "/noauth/activate")
    public JwtPair activateUser(@Parameter(description = "Activate user request.")
                                @RequestBody ActivateUserRequest activateRequest,
                                @RequestParam(required = false, defaultValue = "true") boolean sendActivationMail,
                                HttpServletRequest request) {
        String activateToken = activateRequest.getActivateToken();
        String password = activateRequest.getPassword();
        systemSecurityService.validatePassword(password, null);
        String encodedPassword = passwordEncoder.encode(password);
        UserCredentials credentials = userService.activateUserCredentials(TenantId.SYS_TENANT_ID, activateToken, encodedPassword);
        User user = userService.findUserById(TenantId.SYS_TENANT_ID, credentials.getUserId());
        UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
        SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal);
        userService.setUserCredentialsEnabled(user.getTenantId(), user.getId(), true);
        String baseUrl = systemSecurityService.getBaseUrl(user.getTenantId(), user.getCustomerId(), request);
        String loginUrl = String.format("%s/login", baseUrl);
        String email = user.getEmail();

        if (sendActivationMail) {
            try {
                mailService.sendAccountActivatedEmail(loginUrl, email);
            } catch (Exception e) {
                log.warn("Unable to send account activation email [{}]", e.getMessage());
            }
        }

        var tokenPair = tokenFactory.createTokenPair(securityUser);
        systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(request), ActionType.LOGIN, null);
        return tokenPair;
    }

    @ApiOperation(value = "Reset password (resetPassword)",
            notes = "Checks the password reset token and updates the password. " +
                    "If token is not valid, returns '400 Bad Request'.")
    @PostMapping(value = "/noauth/resetPassword")
    public void resetPassword(@Parameter(description = "Reset password request.")
                                 @RequestBody ResetPasswordRequest resetPasswordRequest,
                                 HttpServletRequest request) throws ThingsboardException {
        String resetToken = resetPasswordRequest.getResetToken();
        String password = resetPasswordRequest.getPassword();
        UserCredentials userCredentials = userService.findUserCredentialsByResetToken(TenantId.SYS_TENANT_ID, resetToken);
        if (userCredentials != null) {
            if (userCredentials.isResetTokenExpired()) {
                throw new ThingsboardException("Password reset token expired", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            systemSecurityService.validatePassword(password, userCredentials);
            if (passwordEncoder.matches(password, userCredentials.getPassword())) {
                throw new ThingsboardException("New password should be different from existing!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            String encodedPassword = passwordEncoder.encode(password);
            userCredentials.setPassword(encodedPassword);
            userCredentials.setResetToken(null);
            userCredentials.setResetTokenExpTime(null);
            userCredentials = userService.replaceUserCredentials(TenantId.SYS_TENANT_ID, userCredentials);
            User user = userService.findUserById(TenantId.SYS_TENANT_ID, userCredentials.getUserId());
            UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
            SecurityUser securityUser = new SecurityUser(user, userCredentials.isEnabled(), principal);
            String baseUrl = systemSecurityService.getBaseUrl(user.getTenantId(), user.getCustomerId(), request);
            String loginUrl = String.format("%s/login", baseUrl);
            String email = user.getEmail();
            try {
                mailService.sendPasswordWasResetEmail(loginUrl, email);
            } catch (Exception e) {
                log.warn("Couldn't send password was reset email: {}", e.getMessage());
            }

            eventPublisher.publishEvent(new UserCredentialsInvalidationEvent(securityUser.getId()));
        } else {
            throw new ThingsboardException("Invalid reset token!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    private void logLogoutAction(HttpServletRequest request) throws ThingsboardException {
        var user = getCurrentUser();
        systemSecurityService.logLoginAction(user, new RestAuthenticationDetails(request), ActionType.LOGOUT, null);
        eventPublisher.publishEvent(new UserSessionInvalidationEvent(user.getSessionId()));
    }


    @ApiOperation(value = "Sign up a new Tenant (signup)",
            notes = "Creates a new tenant with the provided title and returns the details of the newly created tenant.")
    @RequestMapping(value = "/noauth/signup", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Tenant> signUp(
            @Parameter(description = "Sign up request")
            @RequestBody SignUpRequest signUpRequest,
            HttpServletRequest request) throws Exception {

        // Vérifier si un utilisateur avec cet email existe déjà
        if (tbTenantService.findTenantByEmail(signUpRequest.getEmail())) {
            throw new Exception("User with this email already exists.");
        }

        // Valider le mot de passe
        systemSecurityService.validatePassword(signUpRequest.getPassword(), null);

        // Créer un nouveau Tenant
        Tenant tenant = new Tenant();
        tenant.setTitle("New Tenant"); // Définir le titre du tenant
        tenant.setEmail(signUpRequest.getEmail());
        Tenant savedTenant = tbTenantService.save(tenant);

        // Créer un utilisateur tenant associé
        User tenantUser = new User();
        tenantUser.setEmail(signUpRequest.getEmail());
        tenantUser.setTenantId(savedTenant.getTenantId());
        tenantUser.setStatus(true);
        tenantUser.setFirstName(signUpRequest.getFirstName());
        tenantUser.setLastName(signUpRequest.getLastName());
        tenantUser.setAuthority(Authority.TENANT_ADMIN); // Rôle d'administrateur tenant
        tenantUser.setTenantId(savedTenant.getId());
        User savedTenantUser = userService.saveUser(TenantId.SYS_TENANT_ID, tenantUser);

        // Créer un UserCredentials  associé
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, savedTenantUser.getId());
        userCredentials.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        userCredentials.setActivateToken(generateSafeToken(DEFAULT_TOKEN_LENGTH));
        userCredentials.setEnabled(false);
        userService.saveUserCredentials(TenantId.SYS_TENANT_ID, userCredentials);


        String baseUrl = systemSecurityService.getBaseUrl(savedTenantUser.getTenantId(),null, request);
        UserCredentials userCredentialsToken = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, savedTenantUser.getId());

        String activateUrl = String.format("%s/api/noauth/activateUser?activateToken=%s", baseUrl,userCredentialsToken.getActivateToken());
        mailService.sendAccountActivatedEmail(activateUrl,savedTenantUser.getEmail());

        // Retourner une réponse avec le tenant créé
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTenant);
    }
    @ApiOperation(
            value = "Activate User",
            notes = "Activates a user's account using the provided activation token. " +
                    "The token must be valid and associated with a user with a non-null password. " +
                    "Upon successful activation, the user is redirected to the login page."
    )
    @RequestMapping(value = "/noauth/activateUser",params = {"activateToken"},method = RequestMethod.GET)
    public ResponseEntity<?> userActivateToken(
            @Parameter(description = "The activation token string used to activate the user's account.")
            @RequestParam(value = "activateToken") String activateToken) {

        // Find user credentials by activation token
        UserCredentials userCredentials = userService.findUserCredentialsByActivateToken(TenantId.SYS_TENANT_ID, activateToken);

        if (userCredentials == null || userCredentials.getPassword() == null || !userCredentials.getActivateToken().equals(activateToken)) {
            // If the token is invalid, return a conflict response
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Invalid activation token.");
        }

        // Validate password
        String password = userCredentials.getPassword();

        // Encode the password and activate user credentials
        userService.activateUserCredentials(TenantId.SYS_TENANT_ID, activateToken, password);

        // Redirect to the login page with a 303 See Other response
        HttpHeaders headers = new HttpHeaders();
        String loginUrl = String.format("/login");
        headers.setLocation(URI.create(loginUrl));

        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }
    @ApiOperation(
            value = "Renvoyer l'email",
            notes = "Reverifie si l'email est enregistré et renvoie un message en conséquence."
    )
    @RequestMapping(value = "/noauth/renvoyerEmail", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<?> renvoyerEmailSignUp(
            @RequestBody String email, HttpServletRequest request) throws Exception {

        // Vérifiez si l'email est non nul et valide
        if (email == null || email.trim().isEmpty()) {
            throw new Exception("L'email ne peut pas être nul ou vide.");

       }

        // Trouvez l'utilisateur par email
        User user = userService.findUserByEmail(TenantId.SYS_TENANT_ID, email);

        // Si l'utilisateur n'est pas trouvé, retournez un message d'email non existant
        if (user == null) {
            throw new Exception("L'email n'existe pas.");

        }

        // Trouvez les informations de connexion de l'utilisateur
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, user.getId());

        // Si les informations de connexion ne sont pas trouvées, retournez un message indiquant que l'utilisateur n'est pas enregistré
        if (userCredentials == null) {
            throw new Exception("Les informations de connexion de l'utilisateur sont manquantes.");

        }

        // Vérifiez si l'utilisateur est activé ou non
        if (userCredentials.isEnabled()) {
            throw new Exception("Le compte avec cet email est déjà actif.");

        } else {
            // Générer l'URL d'activation
            String baseUrl = systemSecurityService.getBaseUrl(TenantId.SYS_TENANT_ID, null, request);
            String activateUrl = String.format("%s/api/noauth/activateUser?activateToken=%s", baseUrl, userCredentials.getActivateToken());

            // Envoyer l'email avec le lien d'activation
            mailService.sendAccountActivatedEmail(activateUrl, email);

            // Retourner une réponse réussie
            return ResponseEntity.ok("L'email d'activation a été renvoyé.");
        }
    }



    @ApiOperation(value = "Sign up a new Customer (signup)",
            notes = "Creates a new Customer with the provided title and returns the details of the newly created Customer.")
    @PreAuthorize("hasAnyAuthority('TENANT_ADMIN')")
    @RequestMapping(value = "/signup/customer", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Customer> signUpCustomer(
            @Parameter(description = "Sign up request")
            @RequestBody SignUpRequest signUpRequest,
            HttpServletRequest request) throws Exception {

        // Vérifier si un utilisateur avec cet email existe déjà
        if (tbCustomerService.findCustomerByEmail(signUpRequest.getEmail())) {
            throw new Exception("User with this email already exists.");
        }

        // Valider le mot de passe
        systemSecurityService.validatePassword(signUpRequest.getPassword(), null);

        // Créer un nouveau Tenant
        Customer cutomer = new Customer();
        cutomer.setTitle("Customer-" +signUpRequest.getFirstName()  ); // Définir le titre du tenant
        cutomer.setEmail(signUpRequest.getEmail());
        // valider tenantId



        cutomer.setTenantId(getCurrentUser().getTenantId());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode additionalInfo = mapper.readTree("{\"description\":\"\",\"homeDashboardId\":null,\"homeDashboardHideToolbar\":true}");
        cutomer.setAdditionalInfo(additionalInfo);
        Customer savedCustomer = tbCustomerService.save(cutomer , getCurrentUser());

        // Créer un utilisateur tenant associé
        User customerUser = new User();
        customerUser.setEmail(signUpRequest.getEmail());
      //  customerUser.setTenantId(savedCustomer.getTenantId());
        customerUser.setStatus(false);
        customerUser.setFirstName(signUpRequest.getFirstName());
        customerUser.setLastName(signUpRequest.getLastName());
        customerUser.setAuthority(Authority.CUSTOMER_USER); // Rôle d'administrateur tenant
        customerUser.setCustomerId(savedCustomer.getId());
        customerUser.setTenantId(savedCustomer.getTenantId());
        User savedCustomerUser = userService.saveUser(TenantId.SYS_TENANT_ID, customerUser);

        // Créer un UserCredentials  associé
        UserCredentials userCredentials = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, savedCustomerUser.getId());
        userCredentials.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        userCredentials.setActivateToken(generateSafeToken(DEFAULT_TOKEN_LENGTH));
        userCredentials.setEnabled(false);
        userService.saveUserCredentials(TenantId.SYS_TENANT_ID, userCredentials);

        String baseUrl = systemSecurityService.getBaseUrl(savedCustomerUser.getTenantId(),null, request);
        UserCredentials userCredentialsToken = userService.findUserCredentialsByUserId(TenantId.SYS_TENANT_ID, savedCustomerUser.getId());

        String activateUrl = String.format("%s/api/noauth/activateUser?activateToken=%s", baseUrl,userCredentialsToken.getActivateToken());
        mailService.sendAccountActivatedEmail(activateUrl,savedCustomerUser.getEmail());

        // Retourner une réponse avec le tenant créé
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCustomer);
    }
}
