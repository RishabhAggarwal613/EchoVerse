@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication Management", description = "Endpoints for user authentication and authorization")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final CustomeUserDetailsServiceImplementation customUserDetails;

    private static final String GOOGLE_CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID";

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            CustomeUserDetailsServiceImplementation customUserDetails
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.customUserDetails = customUserDetails;
    }

    @PostMapping("/signin/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody LoginWithGooleRequest req)
            throws GeneralSecurityException, IOException {

        User user = validateGoogleIdToken(req);
        String email = user.getEmail();

        User existingUser = userRepository.findByEmail(email);
        if (existingUser == null) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setImage(user.getImage());
            newUser.setFullName(user.getFullName());
            newUser.setLogin_with_google(true);
            newUser.setPassword(user.getPassword());
            newUser.setVerification(new Varification());

            userRepository.save(newUser);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(email, user.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtProvider.generateToken(authentication);

        AuthResponse authResponse = new AuthResponse(token, true);
        return ResponseEntity.ok(authResponse);
    }

    private User validateGoogleIdToken(LoginWithGooleRequest req) throws GeneralSecurityException, IOException {
        HttpTransport transport = new NetHttpTransport();
        JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                .setAudience(Collections.singletonList(GOOGLE_CLIENT_ID)) // Should match with client
                .build();

        GoogleIdToken token = verifier.verify(req.getCredential());

        if (token == null) {
            throw new CredentialException("Invalid ID token");
        }

        Payload payload = token.getPayload();

        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");
        String userId = payload.getSubject();

        User user = new User();
        user.setEmail(email);
        user.setFullName(name);
        user.setImage(pictureUrl);
        user.setPassword(userId); 
        return user;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> createUserHandler(@Valid @RequestBody User user) throws UserException {
        String email = user.getEmail();

        if (userRepository.findByEmail(email) != null) {
            throw new UserException("Email is already used with another account.");
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFullName(user.getFullName());
        newUser.setBirthDate(user.getBirthDate());
        newUser.setPassword(passwordEncoder.encode(user.getPassword()));
        newUser.setVerification(new Varification());

        userRepository.save(newUser);

        Authentication authentication = new UsernamePasswordAuthenticationToken(email, user.getPassword());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtProvider.generateToken(authentication);
        return ResponseEntity.ok(new AuthResponse(token, true));
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signin(@RequestBody LoginRequest loginRequest) {
        String username = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        Authentication authentication = authenticate(username, password);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtProvider.generateToken(authentication);
        return ResponseEntity.ok(new AuthResponse(token, true));
    }

    private Authentication authenticate(String username, String password) {
        UserDetails userDetails = customUserDetails.loadUserByUsername(username);

        if (userDetails == null || !passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}

