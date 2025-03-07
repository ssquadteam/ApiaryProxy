# Apiary AntiBot Integration

Apiary AntiBot is a powerful anti-bot system integrated directly into the Velocity proxy. It provides comprehensive protection against bot attacks, connection spam, and automated login attempts.

## Features

- **Bot Attack Detection**: Automatically detects and mitigates bot attacks
- **Connection Queuing**: Limits the rate of new connections during attacks
- **Advanced Verification System**: Multiple verification methods to ensure players are legitimate
  - **Gravity Check**: Verifies that players follow Minecraft's gravity physics
  - **Collision Check**: Ensures players collide with blocks correctly
  - **Vehicle Check**: Verifies proper interaction with vehicles
  - **CAPTCHA System**: Map-based CAPTCHA for additional verification
  - **Client Brand Validation**: Checks for valid client brands
- **IP-based Protection**: Limits connections per IP and blacklists IPs that fail verification
- **Customizable Messages**: All messages can be customized in the language file

## Configuration

All configuration files are located in the `/antibot` directory:

- `config.yml`: Main configuration file
- `language.yml`: Customizable messages

### Verification Methods

Apiary AntiBot includes several verification methods:

1. **Gravity Check**: Verifies that players fall according to Minecraft's gravity physics
2. **Collision Check**: Ensures players collide with blocks correctly
3. **Vehicle Check**: Verifies proper interaction with vehicles like boats
4. **CAPTCHA**: Map-based CAPTCHA system for additional verification
5. **Client Brand Validation**: Checks for valid client brands

## Architecture

The AntiBot system consists of several key components:

- **ApiaryAntibot**: Main class that coordinates all anti-bot functionality
- **ConnectionQueue**: Manages the queue of incoming connections
- **VerificationManager**: Handles player verification
- **AttackTracker**: Detects and tracks bot attacks

## How It Works

1. When a player connects, their connection is added to the queue
2. During an attack, connections are processed at a limited rate
3. New players are sent to a verification server where they must pass various checks
4. Players who fail verification are disconnected
5. Players who pass verification are allowed to join the server

## Integration

The AntiBot system is fully integrated with the Velocity proxy and requires no additional plugins. It is disabled by default and can be enabled in the `velocity.toml` file:

```toml
[antibot]
enabled = true
```

## Performance

The AntiBot system is designed to be lightweight and efficient, with minimal impact on server performance. The verification process is optimized to use minimal resources while providing maximum protection.

## Credits

Apiary AntiBot is based on the Sonar AntiBot system by jonesdevelopment, with enhancements and integration for the Velocity proxy. 