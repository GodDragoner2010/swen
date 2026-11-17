package at.technikum.mrp.service;

import at.technikum.mrp.domain.AuthToken;
import at.technikum.mrp.domain.User;

public record LoginResult(User user, AuthToken token) {
}
