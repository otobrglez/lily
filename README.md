# The Lily Project

Let there be lilies.

Lily is a [LiveView] implementation on top of [ZIO-HTTP] and will make your experience of writing live/real-time
applications a breeze without or minimal usage of JavaScript.

\- Oto

## Development

- Use `devenv` to get Java JDK. Check [devenv.nix](./devenv.nix) for more information.
- Lily can be interactively developed with the help of `sbt-revolver` with:

 ```bash
 sbt ~backend/reStart
 sbt ~core/test
 ```

- The server boots on http://localhost:3334

[LiveView]: https://hexdocs.pm/phoenix_live_view/Phoenix.LiveView.html
[ZIO-HTTP]: https://zio.dev/zio-http/