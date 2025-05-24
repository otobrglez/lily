package dev.lily.lhtml

import io.circe.*, io.circe.generic.semiauto.*, io.circe.syntax.*

object HtmlJsonCodecs:
  given encodeText: Encoder[Text]             = deriveEncoder
  given decodeText: Decoder[Text]             = deriveDecoder
  given encodeElement: Encoder[Element[Html]] = deriveEncoder
  given decodeElement: Decoder[Element[Html]] = deriveDecoder

  given encodeHtmlF: Encoder[HtmlF[Html]] = Encoder.instance {
    case t @ Text(_)          => Json.obj("type" -> Json.fromString("Text"), "value" -> t.asJson)
    case e @ Element(_, _, _) => Json.obj("type" -> Json.fromString("Element"), "value" -> e.asJson)
  }

  given decodeHtmlF: Decoder[HtmlF[Html]] = Decoder.instance { cursor =>
    cursor.get[String]("type").flatMap {
      case "Text"    => cursor.get[Text]("value")
      case "Element" => cursor.get[Element[Html]]("value")
      case other     => Left(DecodingFailure(s"Unknown HtmlF type: $other", cursor.history))
    }
  }

  given encodeHtml: Encoder[Html] = Encoder.instance(html => encodeHtmlF.apply(html.unfix))
  given decodeHtml: Decoder[Html] = decodeHtmlF.map(Fix(_))

  // HtmlDiff
  given encodeAttrChanged: Encoder[HtmlDiff.AttrChanged]       = deriveEncoder
  given decodeAttrChanged: Decoder[HtmlDiff.AttrChanged]       = deriveDecoder
  given encodeChangeChildren: Encoder[HtmlDiff.ChangeChildren] = deriveEncoder
  given decodeChangeChildren: Decoder[HtmlDiff.ChangeChildren] = deriveDecoder
  given encodeChangeAttrs: Encoder[HtmlDiff.ChangeAttrs]       = deriveEncoder
  given decodeChangeAttrs: Decoder[HtmlDiff.ChangeAttrs]       = deriveDecoder
  given encodeReplace: Encoder[HtmlDiff.Replace]               = deriveEncoder
  given decodeReplace: Decoder[HtmlDiff.Replace]               = deriveDecoder

  given encodeHtmlDiff: Encoder[HtmlDiff] = Encoder.instance {
    case HtmlDiff.NoChange               => Json.obj("type" -> Json.fromString("NoChange"))
    case r @ HtmlDiff.Replace(_)         =>
      Json.obj("type" -> Json.fromString("Replace"), "value" -> r.asJson)
    case ca @ HtmlDiff.ChangeAttrs(_, _) =>
      Json.obj("type" -> Json.fromString("ChangeAttrs"), "value" -> ca.asJson)
    case cc @ HtmlDiff.ChangeChildren(_) =>
      Json.obj("type" -> Json.fromString("ChangeChildren"), "value" -> cc.asJson)
    case x                               =>
      println(s"Unknown HtmlDiff thing wot encode: $x")
      Json.Null
  }

  given decodeHtmlDiff: Decoder[HtmlDiff] = Decoder.instance { cursor =>
    cursor.get[String]("type").flatMap {
      case "NoChange"       => Right(HtmlDiff.NoChange)
      case "Replace"        => cursor.get[HtmlDiff.Replace]("value")
      case "ChangeAttrs"    => cursor.get[HtmlDiff.ChangeAttrs]("value")
      case "ChangeChildren" => cursor.get[HtmlDiff.ChangeChildren]("value")
      case other            => Left(DecodingFailure(s"Unknown HtmlDiff type: $other", cursor.history))
    }
  }
