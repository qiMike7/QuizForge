package org.example.quizapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RagService {

    /** 单次调用不足时，最多补齐的重试次数（含首次）。 */
    private static final int MAX_ATTEMPTS = 3;
    /** 合法题目选项数量范围。 */
    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 6;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public RagService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 严格按 numQuestions 生成题目：
     * 1. 每次让模型多产出 ~30% 以对冲非法/重复；
     * 2. 按题干去重后累加；
     * 3. 若不足则重试（最多 MAX_ATTEMPTS 次），若超出则截断到 numQuestions。
     */
    public List<Map<String, Object>> generateQuestionsFromText(String text, int numQuestions) {
        if (text == null || text.trim().isEmpty() || numQuestions <= 0) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> collected = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int attempt = 0; attempt < MAX_ATTEMPTS && collected.size() < numQuestions; attempt++) {
            int need = numQuestions - collected.size();
            int askFor = Math.max(need + 2, (int) Math.ceil(need * 1.3));

            String prompt = buildQuestionGenerationPrompt(text, askFor, collected);

            String response;
            try {
                response = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }

            for (Map<String, Object> q : parseQuestionsFromResponse(response)) {
                if (collected.size() >= numQuestions) break;
                String key = normalizeKey(q);
                if (key == null || seen.add(key)) {
                    collected.add(q);
                }
            }
        }

        if (collected.size() > numQuestions) {
            return new ArrayList<>(collected.subList(0, numQuestions));
        }
        return collected;
    }

    private String buildQuestionGenerationPrompt(String text, int numQuestions,
                                                 List<Map<String, Object>> alreadyGenerated) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一名严格的考试出题器。请根据下面的学习内容，生成 ")
          .append(numQuestions)
          .append(" 道独立、互不重复的单项选择题。\n\n");

        sb.append("【硬性数量要求】\n");
        sb.append("- 必须恰好返回 ").append(numQuestions).append(" 个 JSON 对象，多一道或少一道都视为失败；\n");
        sb.append("- 若知识点较少，请从概念辨析、细节记忆、应用推理等不同角度拆分出题以凑足数量；\n");
        sb.append("- 只输出一个合法的 JSON 数组，不要任何解释文字、不要 Markdown 代码块围栏。\n\n");

        sb.append("【格式要求】\n");
        sb.append("1. 每题 4 个选项，按 A/B/C/D 顺序放入 options 数组；\n");
        sb.append("2. correctAnswer 为正确选项的下标（0=A、1=B、2=C、3=D），必须是数字；\n");
        sb.append("3. 每题必须包含 question / options / correctAnswer / explanation 四个字段；\n");
        sb.append("4. 字符串中出现双引号需按 JSON 规范转义。\n\n");

        if (alreadyGenerated != null && !alreadyGenerated.isEmpty()) {
            sb.append("【以下题目已生成，本次严禁重复或高度近似】\n");
            for (Map<String, Object> q : alreadyGenerated) {
                Object t = q.get("question");
                if (t != null) sb.append("- ").append(t).append("\n");
            }
            sb.append("\n");
        }

        sb.append("【学习内容】\n").append(text).append("\n\n");

        sb.append("【输出格式示例】\n");
        sb.append("[\n");
        sb.append("  {\"question\":\"...\",\"options\":[\"...\",\"...\",\"...\",\"...\"],\"correctAnswer\":0,\"explanation\":\"...\"}\n");
        sb.append("]\n");

        return sb.toString();
    }

    private List<Map<String, Object>> parseQuestionsFromResponse(String response) {
        if (response == null || response.isBlank()) return Collections.emptyList();

        String cleaned = cleanJsonResponse(response);
        try {
            List<Map<String, Object>> parsed = objectMapper.readValue(
                    cleaned, new TypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> valid = new ArrayList<>();
            for (Map<String, Object> q : parsed) {
                Map<String, Object> normalized = normalizeQuestion(q);
                if (normalized != null) valid.add(normalized);
            }
            if (!valid.isEmpty()) return valid;
        } catch (Exception ignore) {
            // 落到正则兜底
        }
        return parseQuestionsManually(response);
    }

    /**
     * 剥离 Markdown 代码块围栏，并截取首个 JSON 数组片段。
     * 兼容模型偶尔在 JSON 前后夹带的解释性文字。
     */
    private String cleanJsonResponse(String response) {
        String s = response.trim();

        // 去掉开头的 ```xxx 围栏行
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            s = (firstNl > 0) ? s.substring(firstNl + 1) : s.substring(3);
        }
        // 去掉结尾的 ``` 围栏
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        s = s.trim();

        // 定位 JSON 数组
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start >= 0 && end > start) {
            s = s.substring(start, end + 1);
        }
        return s;
    }

    /**
     * 校验单题字段并规范化：correctAnswer 转 Integer，options 转 List&lt;String&gt;。
     * 不合法返回 null 由调用方丢弃。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeQuestion(Map<String, Object> q) {
        if (q == null) return null;

        Object question = q.get("question");
        Object explanation = q.get("explanation");
        Object optionsRaw = q.get("options");
        Object correctRaw = q.get("correctAnswer");

        if (!(question instanceof String) || ((String) question).isBlank()) return null;
        if (!(optionsRaw instanceof List)) return null;

        List<String> options = new ArrayList<>();
        for (Object o : (List<Object>) optionsRaw) {
            options.add(o == null ? "" : String.valueOf(o));
        }
        if (options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) return null;

        int correct;
        if (correctRaw instanceof Number) {
            correct = ((Number) correctRaw).intValue();
        } else if (correctRaw instanceof String) {
            try { correct = Integer.parseInt(((String) correctRaw).trim()); }
            catch (NumberFormatException e) { return null; }
        } else {
            return null;
        }
        if (correct < 0 || correct >= options.size()) return null;

        Map<String, Object> out = new HashMap<>();
        out.put("question", ((String) question).trim());
        out.put("options", options);
        out.put("correctAnswer", correct);
        out.put("explanation", explanation == null ? "" : String.valueOf(explanation).trim());
        return out;
    }

    /** 用于去重的题干 key：小写 + 去空白。 */
    private String normalizeKey(Map<String, Object> q) {
        Object t = q.get("question");
        if (!(t instanceof String)) return null;
        return ((String) t).toLowerCase().replaceAll("\\s+", "");
    }

    /** JSON 解析失败时的正则兜底，支持字符串内的转义双引号。 */
    private List<Map<String, Object>> parseQuestionsManually(String response) {
        List<Map<String, Object>> questions = new ArrayList<>();

        Pattern questionPattern = Pattern.compile(
                "\"question\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*" +
                        "\"options\"\\s*:\\s*\\[((?:[^\\]]|\\\\.)*)\\]\\s*,\\s*" +
                        "\"correctAnswer\"\\s*:\\s*(\\d+)\\s*,\\s*" +
                        "\"explanation\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
                Pattern.DOTALL);
        Pattern optionPattern = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

        Matcher matcher = questionPattern.matcher(response);
        while (matcher.find()) {
            try {
                Map<String, Object> raw = new HashMap<>();
                raw.put("question", unescapeJson(matcher.group(1)));

                List<String> options = new ArrayList<>();
                Matcher om = optionPattern.matcher(matcher.group(2));
                while (om.find()) options.add(unescapeJson(om.group(1)));
                raw.put("options", options);

                raw.put("correctAnswer", Integer.parseInt(matcher.group(3)));
                raw.put("explanation", unescapeJson(matcher.group(4)));

                Map<String, Object> normalized = normalizeQuestion(raw);
                if (normalized != null) questions.add(normalized);
            } catch (Exception ignore) { }
        }
        return questions;
    }

    private String unescapeJson(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    default -> sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }
}
