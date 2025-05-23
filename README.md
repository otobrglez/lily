# The Lily Project

 Let there be lilies.

 \- Oto

## Development

- Use `devenv` to get Java JDK. Check [devenv.nix](./devenv.nix) for more information.
- Lily can be interactively developed with the help of `sbt-revolver` with:
 ```bash
 sbt ~backend/reStart
 sbt ~core/test
 ```
- The server boots on http://localhost:3334