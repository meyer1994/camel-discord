package io.meyer1994.example;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component("emojiCounter")
public class EmojiCounter {
    public Map<String, Long> count(List<Map<String, Object>> rows) {
        Map<String, Long> counts = rows.stream()
                .map(row -> (String) row.get("MESSAGE"))
                .flatMapToInt(String::codePoints)
                .filter(this::isEmoji)
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .collect(Collectors.groupingBy(
                        emoji -> emoji,
                        LinkedHashMap::new,
                        Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private boolean isEmoji(int codePoint) {
        return codePoint >= 0x1F000 && codePoint <= 0x1FAFF
                || codePoint >= 0x2600 && codePoint <= 0x27BF;
    }
}
