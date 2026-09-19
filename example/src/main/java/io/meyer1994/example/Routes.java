package io.meyer1994.example;

import java.util.Collections;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;

@Controller
public class Routes {
  @Autowired private Twitch twitch;

  @Autowired private Kick kick;

  @Autowired private Chart chart;

  @Value("${app.twitch.channels}")
  private Set<String> channels = Collections.emptySet();

  @GetMapping(path = "/", produces = MediaType.TEXT_HTML_VALUE)
  public String index(Model model) {
    model.addAttribute("channels", channels);
    model.addAttribute("feed", false);
    return "index";
  }

  @GetMapping(path = "/c", produces = MediaType.TEXT_HTML_VALUE)
  public String selectChannel(@RequestParam("channel") String channel) {
    if (channel == null || channel.isBlank()) {
      return "redirect:/";
    }
    return String.format("redirect:/c/%s", channel.trim().toLowerCase());
  }

  @GetMapping(path = "/c/{channel}", produces = MediaType.TEXT_HTML_VALUE)
  public String channel(@PathVariable("channel") String channel, Model model) {
    model.addAttribute("feed", true);
    model.addAttribute("channel", channel.trim().toLowerCase());
    return "index";
  }

  @ResponseBody
  @GetMapping(path = "/c/{channel}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> events(@PathVariable("channel") String channel) {
    String key = channel.trim().toLowerCase();
    return Flux.merge(kick.stream(key), twitch.stream(key));
  }

  @ResponseBody
  @GetMapping(path = "/chart/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> chartData() {
    return chart.stream();
  }
}
