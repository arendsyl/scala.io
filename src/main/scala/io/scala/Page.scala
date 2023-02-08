package io.scala

import io.scala.views.*

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}
import com.raquo.waypoint.*
import org.scalajs.dom.html
import org.scalajs.dom.idb.Index
import upickle.default.*

enum Page {
  case IndexPage
  case SpeakersPage
  case SponsorsPage
}

object Page {
  implicit val pageCodec: ReadWriter[Page] = macroRW

  val indexRoute    = Route.static(Page.IndexPage, root / endOfSegments)
  val speakersRoute = Route.static(Page.SpeakersPage, root / "speakers" / endOfSegments)
  val sponsorsRoute = Route.static(Page.SponsorsPage, root / "sponsors" / endOfSegments)

  val router = new Router[Page](
    routes = List(indexRoute, speakersRoute, sponsorsRoute),
    getPageTitle = _ => "ScalaIO",
    serializePage = page => write(page)(pageCodec),
    deserializePage = pageStr => read(pageStr)(pageCodec)
  )(
    $popStateEvent = L.windowEvents.onPopState,
    owner = L.unsafeWindowOwner
  )

  val splitter = SplitRender[Page, HtmlElement](router.$currentPage)
    .collect[Page] {
      case Page.IndexPage    => Index.render
      case Page.SpeakersPage => Speakers.render
      case Page.SponsorsPage => Sponsors.render
    }

  def navigateTo(page: Page): Binder[HtmlElement] = Binder { el =>

    val isLinkElement = el.ref.isInstanceOf[html.Anchor]

    if (isLinkElement) {
      el.amend(href(router.absoluteUrlForPage(page)))
    }

    // If element is a link and user is holding a modifier while clicking:
    //  - Do nothing, browser will open the URL in new tab / window / etc. depending on the modifier key
    // Otherwise:
    //  - Perform regular pushState transition
    (onClick
      .filter(ev => !(isLinkElement && (ev.ctrlKey || ev.metaKey || ev.shiftKey || ev.altKey)))
      .preventDefault
      --> (_ => router.pushState(page))).bind(el)
  }
}
