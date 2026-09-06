package io.meyer1994.example;

import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class Examples {

    private final ProducerTemplate producerTemplate;
    private final JdbcTemplate jdbcTemplate;

    public Examples(ProducerTemplate producerTemplate, JdbcTemplate jdbcTemplate) {
        this.producerTemplate = producerTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void createExampleEmbeddings() {
        jdbcTemplate.update("DELETE FROM text_examples");

        List<Map<String, String>> examples = List.of(
                // GOOD — appreciation & praise (40)
                Map.of("text", "love this stream, best content ever", "label", "good"),
                Map.of("text", "amazing gameplay, so entertaining", "label", "good"),
                Map.of("text", "you're doing great, keep it up", "label", "good"),
                Map.of("text", "this is awesome, never stop streaming", "label", "good"),
                Map.of("text", "great vibes in here, loving the energy", "label", "good"),
                Map.of("text", "such a fun community, everyone is so nice", "label", "good"),
                Map.of("text", "this is the highlight of my day", "label", "good"),
                Map.of("text", "your skill is incredible to watch", "label", "good"),
                Map.of("text", "thank you for making my night better", "label", "good"),
                Map.of("text", "the chat is so wholesome today", "label", "good"),
                Map.of("text", "you deserve way more viewers", "label", "good"),
                Map.of("text", "this streamer is a hidden gem", "label", "good"),
                Map.of("text", "favourite streamer hands down", "label", "good"),
                Map.of("text", "what a clutch play", "label", "good"),
                Map.of("text", "respect to the mods", "label", "good"),
                Map.of("text", "welcome back, we missed you", "label", "good"),
                Map.of("text", "this music slaps", "label", "good"),
                Map.of("text", "the production quality is top tier", "label", "good"),
                Map.of("text", "GG that was well played", "label", "good"),
                Map.of("text", "I learned so much from this", "label", "good"),
                Map.of("text", "you're so funny I can't breathe", "label", "good"),
                Map.of("text", "the vibes are immaculate", "label", "good"),
                Map.of("text", "just gifted five subs because this rocks", "label", "good"),
                Map.of("text", "been watching for years, never disappoints", "label", "good"),
                Map.of("text", "your positivity is contagious", "label", "good"),
                Map.of("text", "this collab is legendary", "label", "good"),
                Map.of("text", "pure talent on display", "label", "good"),
                Map.of("text", "every stream gets better", "label", "good"),
                Map.of("text", "you genuinely care about chat and it shows", "label", "good"),
                Map.of("text", "the storytelling here is phenomenal", "label", "good"),
                Map.of("text", "never laughed this hard in my life", "label", "good"),
                Map.of("text", "real good advice, thanks for that", "label", "good"),
                Map.of("text", "you made my bad day good", "label", "good"),
                Map.of("text", "big respect for grinding like this", "label", "good"),
                Map.of("text", "the community here is unmatched", "label", "good"),
                Map.of("text", "first time here and I'm already hooked", "label", "good"),
                Map.of("text", "you're cracked at this game", "label", "good"),
                Map.of("text", "absolute legend", "label", "good"),
                Map.of("text", "best streamer on the platform no cap", "label", "good"),
                Map.of("text", "this is quality content right here", "label", "good"),

                // GOOD — encouragement & support (30)
                Map.of("text", "don't let the haters get to you", "label", "good"),
                Map.of("text", "you got this, believe in yourself", "label", "good"),
                Map.of("text", "take your time, we'll wait", "label", "good"),
                Map.of("text", "rest is important, don't burn out", "label", "good"),
                Map.of("text", "your mental health matters most", "label", "good"),
                Map.of("text", "proud of how far you've come", "label", "good"),
                Map.of("text", "you inspire me to be better", "label", "good"),
                Map.of("text", "keep being yourself, that's why we watch", "label", "good"),
                Map.of("text", "your hard work is paying off", "label", "good"),
                Map.of("text", "we believe in you", "label", "good"),
                Map.of("text", "you're allowed to have bad days", "label", "good"),
                Map.of("text", "sending positive vibes your way", "label", "good"),
                Map.of("text", "you're stronger than you know", "label", "good"),
                Map.of("text", "your content helps me through tough times", "label", "good"),
                Map.of("text", "don't give up, you're almost there", "label", "good"),
                Map.of("text", "you're doing better than you think", "label", "good"),
                Map.of("text", "we love you just the way you are", "label", "good"),
                Map.of("text", "your creativity is refreshing", "label", "good"),
                Map.of("text", "thanks for being so genuine", "label", "good"),
                Map.of("text", "you make everyone feel welcome", "label", "good"),
                Map.of("text", "your perseverance is admirable", "label", "good"),
                Map.of("text", "you're building something special here", "label", "good"),
                Map.of("text", "keep pushing boundaries", "label", "good"),
                Map.of("text", "your authenticity is rare", "label", "good"),
                Map.of("text", "you handle pressure so well", "label", "good"),
                Map.of("text", "your growth is inspiring to see", "label", "good"),
                Map.of("text", "never doubt your impact", "label", "good"),
                Map.of("text", "you bring out the best in chat", "label", "good"),
                Map.of("text", "your dedication doesn't go unnoticed", "label", "good"),
                Map.of("text", "you're a role model for many", "label", "good"),

                // GOOD — casual & social (20)
                Map.of("text", "hello from Australia", "label", "good"),
                Map.of("text", "just got home from work, excited to catch the stream", "label", "good"),
                Map.of("text", "anyone else watching while eating dinner", "label", "good"),
                Map.of("text", "the emotes are perfect for this moment", "label", "good"),
                Map.of("text", "chat is moving so fast", "label", "good"),
                Map.of("text", "who else is here for the first time", "label", "good"),
                Map.of("text", "shoutout to the regulars in chat", "label", "good"),
                Map.of("text", "this is my comfort stream", "label", "good"),
                Map.of("text", "can't believe I caught this live", "label", "good"),
                Map.of("text", "the notifications finally worked", "label", "good"),
                Map.of("text", "who else is watching on mobile", "label", "good"),
                Map.of("text", "just subbed with prime, let's go", "label", "good"),
                Map.of("text", "the chat memes are elite", "label", "good"),
                Map.of("text", "happy to be part of this community", "label", "good"),
                Map.of("text", "this stream is my daily routine now", "label", "good"),
                Map.of("text", "the raid was insane last time", "label", "good"),
                Map.of("text", "love how interactive you are with chat", "label", "good"),
                Map.of("text", "the overlays look clean today", "label", "good"),
                Map.of("text", "best way to end my day", "label", "good"),
                Map.of("text", "who else has been here since the beginning", "label", "good"),

                // GOOD — gaming specific (20)
                Map.of("text", "that headshot was clean", "label", "good"),
                Map.of("text", "nice flick", "label", "good"),
                Map.of("text", "the mechanics are insane", "label", "good"),
                Map.of("text", "perfect timing on that ability", "label", "good"),
                Map.of("text", "the macro play is next level", "label", "good"),
                Map.of("text", "that read was absolutely correct", "label", "good"),
                Map.of("text", "the game sense is unreal", "label", "good"),
                Map.of("text", "how do you even see that", "label", "good"),
                Map.of("text", "that combo was perfectly executed", "label", "good"),
                Map.of("text", "the positioning here is textbook", "label", "good"),
                Map.of("text", "incredible reaction time", "label", "good"),
                Map.of("text", "that was a 200 IQ play", "label", "good"),
                Map.of("text", "the callouts are on point", "label", "good"),
                Map.of("text", "clean ace", "label", "good"),
                Map.of("text", "the teamwork with your duo is amazing", "label", "good"),
                Map.of("text", "that outplay was nasty", "label", "good"),
                Map.of("text", "you predicted that perfectly", "label", "good"),
                Map.of("text", "the map awareness is elite", "label", "good"),
                Map.of("text", "that was a pro level play", "label", "good"),
                Map.of("text", "the speedrun strats are wild", "label", "good"),

                // GOOD — emotes & hype (20)
                Map.of("text", "lets goooo", "label", "good"),
                Map.of("text", "poggers", "label", "good"),
                Map.of("text", "monkaS that was intense", "label", "good"),
                Map.of("text", "OMEGALUL", "label", "good"),
                Map.of("text", "pepeD hype", "label", "good"),
                Map.of("text", "widepeepoHappy vibes", "label", "good"),
                Map.of("text", "EZ Clap", "label", "good"),
                Map.of("text", "FeelsGoodMan", "label", "good"),
                Map.of("text", "PagMan finally", "label", "good"),
                Map.of("text", "gachiHYPER", "label", "good"),
                Map.of("text", "peepoCheer", "label", "good"),
                Map.of("text", "TriHard 7", "label", "good"),
                Map.of("text", "KKomrade salute", "label", "good"),
                Map.of("text", "SourPls this banger", "label", "good"),
                Map.of("text", "BibleThump wholesome", "label", "good"),
                Map.of("text", "PogChamp moment", "label", "good"),
                Map.of("text", "LULW", "label", "good"),
                Map.of("text", "pepeJAM", "label", "good"),
                Map.of("text", "YEP", "label", "good"),
                Map.of("text", "BatChest I'm literally shaking", "label", "good"),

                // BAD — direct hostility (35)
                Map.of("text", "hate this, worst stream ever", "label", "bad"),
                Map.of("text", "terrible content, leaving now", "label", "bad"),
                Map.of("text", "you're so bad at this game, uninstall", "label", "bad"),
                Map.of("text", "this is boring garbage, do something else", "label", "bad"),
                Map.of("text", "toxic chat, mods are useless", "label", "bad"),
                Map.of("text", "worst take I've ever heard, clown", "label", "bad"),
                Map.of("text", "why are you even streaming this", "label", "bad"),
                Map.of("text", "nobody asked for your opinion", "label", "bad"),
                Map.of("text", "go touch grass", "label", "bad"),
                Map.of("text", "this stream is a waste of time", "label", "bad"),
                Map.of("text", "you always throw, it's painful", "label", "bad"),
                Map.of("text", "shut up and play the game", "label", "bad"),
                Map.of("text", "mods are asleep, chat is chaos", "label", "bad"),
                Map.of("text", "I hope you lose every match", "label", "bad"),
                Map.of("text", "your setup looks like a dumpster", "label", "bad"),
                Map.of("text", "can't believe people actually watch this", "label", "bad"),
                Map.of("text", "streamer is so cringe I had to leave", "label", "bad"),
                Map.of("text", "this is why nobody likes you", "label", "bad"),
                Map.of("text", "keep whining, it's all you're good at", "label", "bad"),
                Map.of("text", "not funny, never was", "label", "bad"),
                Map.of("text", "your thumbnails are misleading trash", "label", "bad"),
                Map.of("text", "pure skill issue", "label", "bad"),
                Map.of("text", "selling out for sponsors I see", "label", "bad"),
                Map.of("text", "copying other streamers, zero originality", "label", "bad"),
                Map.of("text", "the audio mix is giving me a headache", "label", "bad"),
                Map.of("text", "fake reactions for donations, pathetic", "label", "bad"),
                Map.of("text", "you're only here because of luck", "label", "bad"),
                Map.of("text", "ratio", "label", "bad"),
                Map.of("text", "you look like you've never seen sunlight", "label", "bad"),
                Map.of("text", "who let this person on the internet", "label", "bad"),
                Map.of("text", "can't even afford a decent mic lol", "label", "bad"),
                Map.of("text", "streamer is so washed up", "label", "bad"),
                Map.of("text", "just retire already", "label", "bad"),
                Map.of("text", "your chat is a cult and not in a good way", "label", "bad"),
                Map.of("text", "delusional", "label", "bad"),
                Map.of("text", "you peaked two years ago and it shows", "label", "bad"),

                // BAD — harassment & personal attacks (30)
                Map.of("text", "kill yourself", "label", "bad"),
                Map.of("text", "no one would miss you", "label", "bad"),
                Map.of("text", "you're a failure in life", "label", "bad"),
                Map.of("text", "your family must be disappointed", "label", "bad"),
                Map.of("text", "you're the worst person I've ever seen", "label", "bad"),
                Map.of("text", "do the world a favor and log off", "label", "bad"),
                Map.of("text", "your voice is annoying as hell", "label", "bad"),
                Map.of("text", "get a real job you loser", "label", "bad"),
                Map.of("text", "you're worthless", "label", "bad"),
                Map.of("text", "everyone here hates you", "label", "bad"),
                Map.of("text", "your existence is a mistake", "label", "bad"),
                Map.of("text", "ugly both inside and out", "label", "bad"),
                Map.of("text", "you're a disgrace to humanity", "label", "bad"),
                Map.of("text", "hope you get banned forever", "label", "bad"),
                Map.of("text", "your parents regret having you", "label", "bad"),
                Map.of("text", "die in a fire", "label", "bad"),
                Map.of("text", "you're mentally ill and it shows", "label", "bad"),
                Map.of("text", "kys noob", "label", "bad"),
                Map.of("text", "you're a waste of oxygen", "label", "bad"),
                Map.of("text", "nobody loves you", "label", "bad"),
                Map.of("text", "you're the reason we can't have nice things", "label", "bad"),
                Map.of("text", "pathetic waste of space", "label", "bad"),
                Map.of("text", "your opinion is irrelevant", "label", "bad"),
                Map.of("text", "kill yourself speedrun any percent", "label", "bad"),
                Map.of("text", "you're a burden to everyone around you", "label", "bad"),
                Map.of("text", "no talent, no personality, nothing", "label", "bad"),
                Map.of("text", "I genuinely despise you", "label", "bad"),
                Map.of("text", "you're a stain on society", "label", "bad"),
                Map.of("text", "end yourself", "label", "bad"),
                Map.of("text", "trash human being", "label", "bad"),

                // BAD — spam & disruption (20)
                Map.of("text", "wwwwwwwwwwwwww", "label", "bad"),
                Map.of("text", "first", "label", "bad"),
                Map.of("text", "second", "label", "bad"),
                Map.of("text", "third lol", "label", "bad"),
                Map.of("text", "spam spam spam spam spam", "label", "bad"),
                Map.of("text", "copy paste copy paste", "label", "bad"),
                Map.of("text", "ascii art walls", "label", "bad"),
                Map.of("text", "AAAAAAAAAAAAAAAAAAAAA", "label", "bad"),
                Map.of("text", "!!!", "label", "bad"),
                Map.of("text", "reeeeeeeeeeeeeeee", "label", "bad"),
                Map.of("text", "same", "label", "bad"),
                Map.of("text", "same", "label", "bad"),
                Map.of("text", "same", "label", "bad"),
                Map.of("text", "same", "label", "bad"),
                Map.of("text", "same", "label", "bad"),
                Map.of("text", "did you just say that out loud", "label", "bad"),
                Map.of("text", "random caps for no reason", "label", "bad"),
                Map.of("text", "wtf is this", "label", "bad"),
                Map.of("text", "????????????????", "label", "bad"),
                Map.of("text", "lolololololololol", "label", "bad"),

                // BAD — gatekeeping & negativity (20)
                Map.of("text", "this used to be better", "label", "bad"),
                Map.of("text", "the old streams were way better", "label", "bad"),
                Map.of("text", "you've changed and not in a good way", "label", "bad"),
                Map.of("text", "fake fan detected", "label", "bad"),
                Map.of("text", "new viewers don't understand the lore", "label", "bad"),
                Map.of("text", "you're not a real gamer", "label", "bad"),
                Map.of("text", "bandwagon fans are the worst", "label", "bad"),
                Map.of("text", "you only watch because it's trending", "label", "bad"),
                Map.of("text", "OG fans remember when this was good", "label", "bad"),
                Map.of("text", "this community went downhill", "label", "bad"),
                Map.of("text", "casuals ruin everything", "label", "bad"),
                Map.of("text", "you don't even play this game", "label", "bad"),
                Map.of("text", "fake reactions for donations pathetic", "label", "bad"),
                Map.of("text", "sellout", "label", "bad"),
                Map.of("text", "just here for drops", "label", "bad"),
                Map.of("text", "your takes are mid", "label", "bad"),
                Map.of("text", "overrated streamer", "label", "bad"),
                Map.of("text", "main character syndrome", "label", "bad"),
                Map.of("text", "pick me energy", "label", "bad"),
                Map.of("text", "virtue signaling hard", "label", "bad"),

                // BAD — trolling & bait (15)
                Map.of(
                        "text",
                        "did you know you can double your fps by deleting system32",
                        "label",
                        "bad"),
                Map.of("text", "source trust me bro", "label", "bad"),
                Map.of("text", "not reading that essay", "label", "bad"),
                Map.of("text", "counter ratio plus you're wrong", "label", "bad"),
                Map.of("text", "cope harder", "label", "bad"),
                Map.of("text", "seethe", "label", "bad"),
                Map.of("text", "mald", "label", "bad"),
                Map.of("text", "rent free", "label", "bad"),
                Map.of("text", "didn't ask plus you're white", "label", "bad"),
                Map.of("text", "stay mad", "label", "bad"),
                Map.of("text", "crying on the internet", "label", "bad"),
                Map.of("text", "who hurt you", "label", "bad"),
                Map.of("text", "touch grass", "label", "bad"),
                Map.of("text", "skill issue plus ratio plus you're maidenless", "label", "bad"),
                Map.of("text", "based on what", "label", "bad"));

        producerTemplate.asyncSendBody("direct:example-embeddings", examples);
    }

    /**
     * Queries the 20 nearest labeled examples and returns a weighted good score in
     * the range [0.0,
     * 100.0]. Returns 50.0 when no examples exist.
     *
     * @param embedding comma-separated vector string, e.g. "[0.012, -0.034, ...]"
     */
    public Double getScore(String embedding) {
        String sql = """
                SELECT
                    COALESCE(
                        SUM(similarity * CASE WHEN label = 'good' THEN 1.0 ELSE 0.0 END)
                        / NULLIF(SUM(similarity), 0),
                        0.5
                    ) * 100.0 AS good_score
                FROM (
                    SELECT
                        label,
                        GREATEST(0, 1 - (embedding <=> ?::vector)) AS similarity
                    FROM text_examples
                    ORDER BY embedding <=> ?::vector
                    LIMIT 20
                ) neighbors
                """;

        return jdbcTemplate.queryForObject(sql, Double.class, embedding, embedding);
    }

    /**
     * Camel-friendly overload. Reads {@code variable.embedding}, computes the
     * score, and writes
     * {@code variable.goodScore}, {@code variable.badScore}, and
     * {@code variable.score} (all 0-100)
     * back into the exchange.
     */
    public void score(Exchange exchange) {
        String embedding = exchange.getVariable("embedding", String.class);
        if (embedding == null) {
            exchange.setVariable("goodScore", 50.0);
            exchange.setVariable("badScore", 50.0);
            exchange.setVariable("score", 50.0);
            return;
        }

        Double good = getScore(embedding);
        Double bad = 100.0 - good;

        exchange.setVariable("goodScore", good);
        exchange.setVariable("badScore", bad);
        exchange.setVariable("score", good);
    }
}
