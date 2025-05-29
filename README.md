# The Lily Framework

Lily is a tiny framework for rapid and easy development of live/real-time web applications in Scala.

Lily is built around and integrates flawlessly with [ZIO HTTP][ZIO-HTTP] and [ZIO Streams][ZIO-STREAMS]. 

The framework offloads the rendering heavy lifting to the server side ([DOM]) and communicates with the frontend via DOM diffing and binary WebSockets (CBOR).

It's a work in progress, so expect things to change. Please see [example apps][examples] for some inspiration.


## Example app

```scala 3
object CounterExample extends LiveView[Any, Int]:
  def state = ZStream.fromZIO(ZIO.succeed(0))

  def on(s: Int) =
    case on("increment" -> _) => ZIO.succeed(s + 1)
    case on("decrement" -> _) => ZIO.succeed(s - 1)
    case _                    => ZIO.succeed(s)

  def render(n: Int, path: Path): Task[Html] = ZIO.succeed:
    Examples.layout(Some("Simple Counter Example"), Some(path))(
      h1("Lily - Counter example"),
      div(p(s"Counter is now: $n")),
      div(p(s"Plus two is ${n + 2}")),
      div(p(s"Some math: ${n * 1.2 * Math.PI}")),
      div(
        button("Increment").on("click" -> "increment"),
        button("Decrement").on("click" -> "decrement")
      )
    )


// Add to your ZIO HTTP routes via:

private val routes =
  Routes(
    Method.GET / Root -> handler(Response.text("Hello, World!"))
  )
    ++ LiveView.route(Path.empty / "counter-v2", examples.CounterExample)
```

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
[ZIO-STREAMS]: https://zio.dev/reference/stream/
[examples]: https://github.com/otobrglez/lily/tree/master/backend/src/main/scala/dev/lily/examples
[DOM]: https://developer.mozilla.org/en-US/docs/Web/API/Document_Object_Model
