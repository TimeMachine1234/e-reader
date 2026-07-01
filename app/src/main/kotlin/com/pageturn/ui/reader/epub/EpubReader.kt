// Copyright 2024 PageTurn Contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.pageturn.ui.reader.epub

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import com.pageturn.domain.model.Book
import com.pageturn.domain.model.Highlight
import com.pageturn.domain.model.ReaderSettings
import com.pageturn.ui.reader.ReaderUiState
import com.pageturn.ui.reader.ReaderViewModel
import com.pageturn.ui.theme.readerThemeByName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Book as EpubBook
import nl.siegmann.epublib.epub.EpubReader as EpublibReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

// Mutable holder so the WebViewClient (created in factory) can read state that
// changes across recompositions without capturing Compose values by snapshot.
private class EpubPagerState {
    @Volatile var book: EpubBook? = null
    // When loading a chapter as a result of a "previous page at chapter start",
    // the new chapter should open on its LAST page rather than its first.
    @Volatile var openAtLastPage: Boolean = false
    // Latest settings/highlights, kept current so the WebViewClient's
    // onPageFinished (a long-lived closure) never injects stale config.
    @Volatile var settings: ReaderSettings = ReaderSettings()
    @Volatile var highlights: List<Highlight> = emptyList()
    // Status-bar height (dp) so text can clear the notification icons in the
    // immersive reader. Kept current for the onPageFinished closure.
    @Volatile var topInsetDp: Int = 0
    // Within-chapter position (0..1) to restore on the first chapter load only;
    // -1 means "no restore" (start at page 0 or last page).
    @Volatile var restoreFrac: Float = -1f
}

private const val EPUB_SCHEME = "https://epub.local/"
private const val FONT_PREFIX = "__fonts__/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubReader(
    book: Book,
    uiState: ReaderUiState,
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier
) {
    var epubBook by remember { mutableStateOf<EpubBook?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var currentSpineIndex by remember { mutableStateOf(0) }
    val pagerState = remember { EpubPagerState() }
    var lastAppliedSettings by remember { mutableStateOf<ReaderSettings?>(null) }

    // Status-bar height in dp (≈ CSS px in the WebView) to clear notification icons.
    val density = LocalDensity.current
    val topInsetDp = (WindowInsets.statusBars.getTop(density) / density.density).toInt()

    // Keep the holder in sync so the WebViewClient closures read live config.
    SideEffect {
        pagerState.settings = uiState.readerSettings
        pagerState.highlights = uiState.highlights
        pagerState.topInsetDp = topInsetDp
    }

    // Parse EPUB in background on first load
    LaunchedEffect(book.filePath) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("EpubReader", "Parsing EPUB: ${book.filePath}")
                val file = File(book.filePath)
                if (!file.exists()) {
                    withContext(Dispatchers.Main) {
                        viewModel.onReadError("Book file not found: ${book.filePath}")
                    }
                    return@withContext
                }
                val parsed = EpublibReader().readEpub(FileInputStream(file))
                val spineSize = parsed.spine.size()
                Log.d("EpubReader", "EPUB parsed OK, spine=$spineSize")
                pagerState.book = parsed
                withContext(Dispatchers.Main) {
                    epubBook = parsed
                    viewModel.onTotalPagesResolved(spineSize)
                    val chapters = parsed.tableOfContents.tocReferences.map { it.title ?: "" }
                    if (chapters.isNotEmpty()) viewModel.onChapterChange(chapters.first())
                }
            } catch (e: Exception) {
                Log.e("EpubReader", "EPUB parse failed", e)
                withContext(Dispatchers.Main) {
                    viewModel.onReadError("Failed to open book: ${e.message}")
                }
            }
        }
    }

    // Load initial chapter as soon as the EPUB is parsed
    LaunchedEffect(epubBook) {
        val eb = epubBook ?: return@LaunchedEffect
        val wv = webView ?: return@LaunchedEffect
        val targetIndex = uiState.currentPage.coerceIn(0, eb.spine.size() - 1)
        currentSpineIndex = targetIndex
        // Restore the exact within-chapter position saved last time (stored as
        // "f:<0..1>" in the book's currentCfi).
        pagerState.restoreFrac = book.currentCfi
            ?.removePrefix("f:")?.toFloatOrNull()
            ?.takeIf { book.currentCfi?.startsWith("f:") == true }
            ?: -1f
        loadSpineItem(wv, eb, targetIndex)
    }

    // Navigate to a different chapter when the spine index changes
    LaunchedEffect(uiState.currentPage, epubBook) {
        val eb = epubBook ?: return@LaunchedEffect
        val wv = webView ?: return@LaunchedEffect
        val targetIndex = uiState.currentPage.coerceIn(0, eb.spine.size() - 1)
        if (targetIndex != currentSpineIndex) {
            currentSpineIndex = targetIndex
            loadSpineItem(wv, eb, targetIndex)
        }
    }

    // React to settings changes while reading.
    LaunchedEffect(uiState.readerSettings, epubBook) {
        val wv = webView ?: return@LaunchedEffect
        if (epubBook == null) return@LaunchedEffect
        val settings = uiState.readerSettings
        val prev = lastAppliedSettings
        lastAppliedSettings = settings
        // Initial layout is handled by onPageFinished; only act on real changes.
        if (prev == null) return@LaunchedEffect
        wv.evaluateJavascript(buildCssInjectionJs(settings), null)
        wv.evaluateJavascript(buildPagerJs(settings, startAtLast = false, pagerState.topInsetDp), null)
    }

    // Re-inject highlights whenever the list changes
    LaunchedEffect(uiState.highlights) {
        webView?.evaluateJavascript(buildHighlightInjectionJs(uiState.highlights), null)
    }

    // Re-lay out on rotation so the new viewport size is used (and "two pages in
    // landscape" engages). The WebView's own resize event is unreliable through
    // Compose, so we drive it here after the view has settled into its new size.
    val orientation = LocalConfiguration.current.orientation
    LaunchedEffect(orientation) {
        val wv = webView ?: return@LaunchedEffect
        if (epubBook == null) return@LaunchedEffect
        delay(200)
        wv.evaluateJavascript(buildCssInjectionJs(pagerState.settings), null)
        wv.evaluateJavascript(buildPagerJs(pagerState.settings, startAtLast = false, pagerState.topInsetDp), null)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    builtInZoomControls = false
                    displayZoomControls = false
                    setSupportZoom(false)
                    // Lock the viewport to the actual view width so rotating the
                    // device never auto-scales the text up or down.
                    useWideViewPort = false
                    loadWithOverviewMode = false
                    textZoom = 100
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? {
                        val url = request.url.toString()
                        if (!url.startsWith(EPUB_SCHEME)) return null
                        val href = url.removePrefix(EPUB_SCHEME)

                        // Bundled reader fonts served from app assets
                        if (href.startsWith(FONT_PREFIX)) {
                            val fontFile = href.removePrefix(FONT_PREFIX)
                            return try {
                                WebResourceResponse(
                                    "font/ttf", null,
                                    context.assets.open("fonts/$fontFile")
                                )
                            } catch (e: Exception) {
                                Log.w("EpubReader", "Font asset missing: $fontFile")
                                null
                            }
                        }

                        // EPUB-embedded resources (images, css, fonts) from memory
                        val resource = pagerState.book?.resources?.getByHref(href)
                        if (resource?.data != null) {
                            val mime = resource.mediaType?.name ?: "application/octet-stream"
                            return WebResourceResponse(mime, "UTF-8", ByteArrayInputStream(resource.data))
                        }
                        Log.w("EpubReader", "Resource not found for href=$href")
                        return null
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean = true // keep navigation inside the reader

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        val startAtLast = pagerState.openAtLastPage
                        pagerState.openAtLastPage = false
                        // Restore-fraction applies to the first chapter load only.
                        val restoreFrac = pagerState.restoreFrac
                        pagerState.restoreFrac = -1f
                        val settings = pagerState.settings
                        view.evaluateJavascript(buildCssInjectionJs(settings), null)
                        view.evaluateJavascript(buildHighlightInjectionJs(pagerState.highlights), null)
                        view.evaluateJavascript(
                            buildPagerJs(settings, startAtLast, pagerState.topInsetDp, restoreFrac), null
                        )
                        view.evaluateJavascript(buildTextSelectionJs(), null)
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                        Log.d(
                            "EpubReaderJS",
                            "${msg.message()} @${msg.sourceId()}:${msg.lineNumber()}"
                        )
                        return true
                    }
                }
                addJavascriptInterface(
                    EpubJsBridge(
                        viewModel = viewModel,
                        onRequestNextChapter = { viewModel.nextChapter() },
                        onRequestPrevChapter = {
                            pagerState.openAtLastPage = true
                            viewModel.prevChapter()
                        }
                    ),
                    "Android"
                )
                webView = this
            }
        },
        update = { wv -> webView = wv }
    )
}

// ---------------------------------------------------------------------------
// Spine loading
// ---------------------------------------------------------------------------

private fun loadSpineItem(webView: WebView, epubBook: EpubBook, spineIndex: Int) {
    val spineRef = epubBook.spine.spineReferences.getOrNull(spineIndex) ?: run {
        Log.e("EpubReader", "No spine ref at index $spineIndex (size=${epubBook.spine.size()})")
        return
    }
    val resource = spineRef.resource ?: return
    val data = resource.data ?: return
    val htmlContent = String(data, Charsets.UTF_8)
    val baseUrl = "$EPUB_SCHEME${resource.href}"
    Log.d("EpubReader", "Loading spine[$spineIndex] href=${resource.href} len=${htmlContent.length}")
    webView.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
}

// ---------------------------------------------------------------------------
// Fonts
// ---------------------------------------------------------------------------

/** Maps a font setting key to (asset file or null, @font-face family name, generic fallback). */
private fun fontMapping(key: String): Triple<String?, String, String> = when (key) {
    "georgia"      -> Triple("Georgia.ttf", "PTGeorgia", "serif")
    "palatino"     -> Triple("Palatino.ttf", "PTPalatino", "serif")
    "opendyslexic" -> Triple("OpenDyslexic-Regular.ttf", "PTOpenDyslexic", "sans-serif")
    "lato"         -> Triple("Lato-Regular.ttf", "PTLato", "sans-serif")
    "merriweather" -> Triple("Merriweather-Regular.ttf", "PTMerriweather", "serif")
    "eb_garamond"  -> Triple("EBGaramond-Regular.ttf", "PTGaramond", "serif")
    else           -> Triple(null, "", "sans-serif") // "system"
}

// ---------------------------------------------------------------------------
// JS builders
// ---------------------------------------------------------------------------

private fun buildCssInjectionJs(settings: ReaderSettings): String {
    val theme = readerThemeByName(settings.theme)
    val bg = colorToHex(theme.backgroundColor)
    val fg = colorToHex(theme.textColor)
    val align = if (settings.justifyText) "justify" else "left"
    val weight = if (settings.boldText) "bold" else "normal"

    val (file, family, generic) = fontMapping(settings.fontFamily)
    val fontFace = if (file != null) {
        "@font-face{font-family:'$family';src:url('${EPUB_SCHEME}${FONT_PREFIX}$file') format('truetype');font-display:swap;}"
    } else ""
    val fontFamilyValue = if (file != null) "'$family', $generic" else generic

    val css = """
        $fontFace
        html { background-color: $bg !important; }
        body, #pt-curl-inner {
            font-family: $fontFamilyValue !important;
            font-size: ${settings.fontSizeSp}px !important;
            line-height: ${settings.lineSpacing} !important;
            letter-spacing: ${settings.letterSpacing}px !important;
            text-align: $align !important;
            font-weight: $weight !important;
            color: $fg !important;
            background-color: $bg !important;
            word-spacing: normal !important;
        }
        p { margin-top: 0 !important; margin-bottom: ${settings.paragraphSpacing}px !important; }
        h1,h2,h3,h4,h5,h6 { color: $fg !important; }
        a { color: inherit !important; text-decoration: underline; }
    """.trimIndent()

    return wrapStyle("pageturn-style", css)
}

private fun wrapStyle(id: String, css: String): String = """
    (function() {
        var style = document.getElementById('$id');
        if (!style) {
            style = document.createElement('style');
            style.id = '$id';
            document.head.appendChild(style);
        }
        style.textContent = ${jsString(css)};
    })();
""".trimIndent()

/** Encodes a Kotlin string as a safe JS string literal. */
private fun jsString(s: String): String =
    "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "") + "\""

/**
 * Self-contained reading engine injected into each chapter document.
 *
 * Supports two modes:
 *  - "page": CSS-column pagination, one screen per page. Native scrolling is
 *    disabled and page transitions are driven by JS so the animation (slide /
 *    fade / curl / none) is fully under our control.
 *  - "scroll": continuous vertical scrolling of the whole chapter.
 *
 * Exposes `window.__ptPager`; reads its live config so re-injecting on a
 * settings change (e.g. switching mode or animation) takes effect immediately.
 */
private fun buildPagerJs(
    settings: ReaderSettings,
    startAtLast: Boolean,
    topInsetDp: Int = 0,
    startFraction: Float = -1f
): String {
    val h = settings.horizontalMarginDp.coerceAtLeast(0)
    val v = settings.verticalPaddingDp.coerceAtLeast(0)
    val top = topInsetDp.coerceAtLeast(0)
    val startFrac = startFraction
    val anim = when (settings.pageTurnAnimation) {
        "fade", "none", "curl" -> settings.pageTurnAnimation
        else -> "slide"
    }
    val mode = if (settings.paginateMode) "page" else "scroll"
    val startLast = if (startAtLast) "true" else "false"
    val bg = colorToHex(readerThemeByName(settings.theme).backgroundColor)
    val cols = settings.columns.coerceIn(1, 2)
    val tap = if (settings.tapZoneLayout == "thirds") "thirds" else "sides"
    val dual = if (settings.dualPageLandscape) "true" else "false"

    return """
(function(){
 try {
  var pager = window.__ptPager || {};
  window.__ptPager = pager;
  pager.H = $h; pager.V = $v; pager.ANIM = '$anim'; pager.MODE = '$mode'; pager.BG = '$bg';
  pager.COLS = $cols; pager.TAP = '$tap'; pager.DUAL = $dual; pager.TOP = $top;
  if(typeof pager.current !== 'number') pager.current = 0;
  var firstInit = !pager.__init;
  var startLast = $startLast;
  var startFraction = $startFrac;

  function vw(){ return window.innerWidth; }
  function vh(){ return window.innerHeight; }
  function B(){ return document.body; }
  function SC(){ return document.scrollingElement || document.documentElement; }
  function styleEl(id){ var s=document.getElementById(id); if(!s){s=document.createElement('style');s.id=id;document.head.appendChild(s);} return s; }

  pager.applyLayout = function(){
    var H=pager.H, V=pager.V, TOP=(pager.TOP||0)+V;
    if(pager.MODE==='scroll'){
      styleEl('pt-pager-style').textContent =
        'html{margin:0!important;padding:0!important;height:auto!important;overflow-x:hidden!important;overflow-y:auto!important;perspective:none!important;}'+
        'body{margin:0!important;box-sizing:border-box!important;padding:'+TOP+'px '+H+'px '+V+'px '+H+'px!important;'+
        'height:auto!important;width:auto!important;column-width:auto!important;-webkit-column-width:auto!important;columns:auto!important;'+
        'overflow:visible!important;transform:none!important;touch-action:auto!important;}'+
        'img,svg,table{max-width:100%!important;height:auto!important;}pre{white-space:pre-wrap!important;}';
    } else {
      // column-count = COLS text columns per screen. With a uniform gap this
      // keeps the page pitch exactly one viewport wide (scrollLeft = page*vw),
      // so 1-column and 2-column layouts both paginate cleanly.
      // "Two pages in landscape" forces a 2-up spread when the screen is wider
      // than it is tall (re-evaluated on rotation via relayout).
      var C = (pager.DUAL && vw()>vh()) ? 2 : (pager.COLS||1);
      var maxImg=Math.max(1, vh()-2*V);
      // width:auto keeps the body equal to the live viewport, so rotating never
      // leaves a stale wide layout that the WebView would scale down to fit.
      styleEl('pt-pager-style').textContent =
        'html{margin:0!important;padding:0!important;height:'+vh()+'px!important;overflow:hidden!important;perspective:1600px!important;}'+
        'body{margin:0!important;box-sizing:border-box!important;padding:'+TOP+'px '+H+'px '+V+'px '+H+'px!important;'+
        'height:'+vh()+'px!important;width:auto!important;'+
        'column-count:'+C+'!important;column-gap:'+(2*H)+'px!important;column-fill:auto!important;'+
        '-webkit-column-count:'+C+'!important;-webkit-column-gap:'+(2*H)+'px!important;'+
        'overflow-x:auto!important;overflow-y:hidden!important;touch-action:none!important;backface-visibility:hidden;}'+
        'body::-webkit-scrollbar{display:none!important;width:0!important;height:0!important;}'+
        'img,svg,table{max-width:100%!important;max-height:'+maxImg+'px!important;height:auto!important;}pre{white-space:pre-wrap!important;}';
    }
  };

  pager.total = function(){
    if(pager.MODE==='scroll') return Math.max(1, Math.round(SC().scrollHeight / vh()));
    return Math.max(1, Math.round(B().scrollWidth / vw()));
  };
  pager.clampP = function(p){ var t=pager.total(); if(p<0)p=0; if(p>t-1)p=t-1; return p; };
  pager.report = function(){ if(window.Android && Android.onChapterPage) Android.onChapterPage(pager.current, pager.total()); };

  // Page mode positions pages via the body's horizontal scrollLeft (which
  // correctly reveals the CSS columns); animations are layered on top.
  pager.renderPage = function(p, animate){
    p = pager.clampP(p);
    var prev = pager.current; pager.current = p;
    var b = B();
    var target = p*vw();
    b.style.transform='none';
    if(!animate || pager.ANIM==='none'){
      b.style.transition='none'; b.style.opacity='1'; b.scrollLeft=target; pager.report(); return;
    }
    if(pager.ANIM==='fade'){
      b.style.transition='opacity 0.15s ease'; b.style.opacity='0';
      setTimeout(function(){ b.scrollLeft=target;
        requestAnimationFrame(function(){ b.style.transition='opacity 0.2s ease'; b.style.opacity='1'; });
        pager.report(); }, 150);
      return;
    }
    if(pager.ANIM==='curl'){
      var dir=(p>=prev)?1:-1;
      b.style.transformOrigin=(dir>0?'left':'right')+' center';
      b.style.transition='transform 0.2s ease, opacity 0.2s ease';
      b.style.transform='rotateY('+(dir*-16)+'deg) scale(0.93)'; b.style.opacity='0.5';
      setTimeout(function(){
        b.scrollLeft=target;
        b.style.transition='none'; b.style.transform='rotateY('+(dir*10)+'deg) scale(0.95)';
        requestAnimationFrame(function(){
          b.style.transition='transform 0.2s ease, opacity 0.2s ease';
          b.style.transform='none'; b.style.opacity='1';
        });
        pager.report();
      }, 200);
      return;
    }
    // slide
    b.style.transition='none'; b.style.opacity='1';
    b.scrollTo({ left: target, behavior: 'smooth' });
    pager.report();
  };

  pager.goTo = function(p, animate){
    if(pager.MODE==='scroll'){ window.scrollTo({ top: p*vh(), behavior: animate?'smooth':'auto' }); pager.current=p; pager.report(); }
    else { pager.renderPage(p, animate); }
  };
  pager.next = function(){ var t=pager.total();
    if(pager.current < t-1) pager.goTo(pager.current+1, true);
    else if(window.Android && Android.requestNextChapter) Android.requestNextChapter(); };
  pager.prev = function(){
    if(pager.current > 0) pager.goTo(pager.current-1, true);
    else if(window.Android && Android.requestPrevChapter) Android.requestPrevChapter(); };

  pager.relayout = function(){
    var keep = pager.current;
    pager.applyLayout();
    requestAnimationFrame(function(){ requestAnimationFrame(function(){
      if(pager.MODE==='scroll'){ window.scrollTo({top: keep*vh(), behavior:'auto'}); pager.report(); }
      else { pager.renderPage(Math.min(keep, pager.total()-1), false); }
    }); });
  };

  pager.applyLayout();
  requestAnimationFrame(function(){ requestAnimationFrame(function(){
    if(firstInit){
      var t=pager.total();
      if(startFraction >= 0) pager.current = Math.round(startFraction*(t-1));
      else pager.current = startLast ? (t-1) : 0;
    }
    if(pager.MODE==='scroll'){ window.scrollTo({top: pager.current*vh(), behavior:'auto'}); pager.report(); }
    else { pager.renderPage(pager.current, false); }
  }); });
  pager.__init = true;

  if(!pager.__bound){
    pager.__bound = true;
    var sx=0, sy=0, st=0, mv=false;
    document.addEventListener('touchstart', function(e){ var t=e.touches[0]; sx=t.clientX; sy=t.clientY; st=Date.now(); mv=false; }, {passive:true});
    document.addEventListener('touchmove', function(e){
      var t=e.touches[0]; if(Math.abs(t.clientX-sx)>10||Math.abs(t.clientY-sy)>10) mv=true;
      if(pager.MODE==='page' && e.cancelable) e.preventDefault(); // no native pan in page mode
    }, {passive:false});
    document.addEventListener('touchend', function(e){
      var t=e.changedTouches[0], dx=t.clientX-sx, dy=t.clientY-sy, dt=Date.now()-st;
      var sel = window.getSelection ? String(window.getSelection()) : '';
      if(sel && sel.length>0) return;

      if(pager.MODE==='scroll'){
        var atBottom=(SC().scrollTop+vh())>=(SC().scrollHeight-3);
        var atTop=SC().scrollTop<=2;
        if(Math.abs(dy)>70 && Math.abs(dy)>Math.abs(dx)){
          if(dy<0 && atBottom && window.Android && Android.requestNextChapter){ Android.requestNextChapter(); return; }
          if(dy>0 && atTop && window.Android && Android.requestPrevChapter){ Android.requestPrevChapter(); return; }
        }
        if(!mv && dt<450 && window.Android && Android.onTap) Android.onTap(t.clientX, t.clientY);
        return;
      }

      // page mode
      if(Math.abs(dx)>50 && Math.abs(dx)>Math.abs(dy)*1.3){ if(dx<0) pager.next(); else pager.prev(); return; }
      if(!mv && dt<450){
        var x=t.clientX, y=t.clientY, w=vw(), hh=vh();
        // Bottom-left corner always reveals the bars (progress + settings access).
        if(x < w*0.22 && y > hh*0.78){ if(window.Android && Android.onTap) Android.onTap(x, y); return; }
        // Tap-zone widths depend on the chosen layout.
        var left = (pager.TAP==='thirds') ? 0.3333 : 0.25;
        var right = (pager.TAP==='thirds') ? 0.6667 : 0.75;
        if(x < w*left) pager.prev();
        else if(x > w*right) pager.next();
        else if(window.Android && Android.onTap) Android.onTap(x, y);
      }
    }, {passive:false});

    window.addEventListener('resize', function(){ if(window.__ptPager) window.__ptPager.relayout(); });

    var sct=null;
    window.addEventListener('scroll', function(){
      if(pager.MODE!=='scroll') return;
      if(sct) clearTimeout(sct);
      sct=setTimeout(function(){ pager.current=Math.round(SC().scrollTop/vh()); pager.report(); }, 120);
    }, {passive:true});
  }
 } catch(err) {
  console.log('[PT] ERROR '+(err && err.message ? err.message : err)+' @'+(err && err.stack ? err.stack : ''));
 }
})();
""".trimIndent()
}

private fun buildHighlightInjectionJs(highlights: List<Highlight>): String {
    val validIds = highlights.joinToString(",") { "\"${it.id}\":1" }
    val highlightCalls = highlights.joinToString("\n") { h ->
        val color = highlightColorToHex(h.color)
        val escapedText = h.selectedText.replace("'", "\\'").replace("\n", " ")
        """highlightText('${h.id}', '$escapedText', '$color');"""
    }
    return """
        (function() {
            var valid = {$validIds};
            // Remove marks for highlights that were deleted (unwrap them).
            var marks = document.querySelectorAll('mark[id^="hl-"]');
            for (var i = marks.length - 1; i >= 0; i--) {
                var m = marks[i];
                if (!valid[m.id.substring(3)]) {
                    var p = m.parentNode;
                    while (m.firstChild) p.insertBefore(m.firstChild, m);
                    p.removeChild(m);
                    if (p.normalize) p.normalize();
                }
            }
            function highlightText(id, text, color) {
                if (!text || text.length === 0) return;
                var body = document.body;
                if (document.getElementById('hl-' + id)) return;
                var escaped = text.replace(/[.*+?^${'$'}{}()|[\]\\]/g, '\\${'$'}&');
                var regex = new RegExp(escaped, 'g');
                body.innerHTML = body.innerHTML.replace(regex,
                    '<mark id="hl-' + id + '" style="background-color:' + color + ';opacity:0.4;">' + text + '</mark>');
            }
            $highlightCalls
            if (window.__ptPager) window.__ptPager.relayout();
        })();
    """.trimIndent()
}

private fun buildTextSelectionJs(): String = """
    (function() {
        if (document._ptSelectionBound) return;
        document._ptSelectionBound = true;
        document.addEventListener('selectionchange', function() {
            var selection = window.getSelection();
            if (!selection || selection.isCollapsed) return;
            var text = selection.toString().trim();
            if (text.length === 0) return;
            var range = selection.getRangeAt(0);
            var rect = range.getBoundingClientRect();
            if (window.Android) {
                window.Android.onTextSelected(text, '', rect.x + rect.width / 2, rect.y);
            }
        });
    })();
""".trimIndent()

// ---------------------------------------------------------------------------
// Color helpers
// ---------------------------------------------------------------------------

private fun colorToHex(color: androidx.compose.ui.graphics.Color): String {
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return String.format("#%02X%02X%02X", r, g, b)
}

private fun highlightColorToHex(colorName: String): String = when (colorName) {
    "yellow" -> "#FFEB3B"
    "green"  -> "#4CAF50"
    "pink"   -> "#E91E63"
    "blue"   -> "#2196F3"
    else     -> "#FFEB3B"
}
