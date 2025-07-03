package pl.cuyer.thedome.exceptions

open class AuthException(message: String) : RuntimeException(message)

class UserAlreadyExistsException : AuthException("User already exists")
class InvalidCredentialsException : AuthException("Invalid credentials")
class InvalidRefreshTokenException : AuthException("Invalid refresh token")
class AnonymousUpgradeException : AuthException("Unable to upgrade anonymous user")
