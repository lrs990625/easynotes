package com.lrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Test {

    public static void main(String[] args) {
        String jsonString = "[{\"user\": \"lrs\"},[{\"id\":\"95\",\"word\":\"行く\",\"kana\":\"ゆく\",\"type\":\"自动词・五段/一类\",\"meanings\":\"1.出嫁。（嫁に行く。とつぐ。）2.（事物）进行；进展。（物事が進展、実現する。）3.上年纪。（ある年齢に達する。）4.做满足；满意。（気持ちが、満足した状態になる。）5.去,走。（人、動物、乗り物が、遠くへ移動する。）6.到……去，往……去，赴。（人、動物、乗り物が目的地に向かって進む。また至りつく。）7.走过，经过，通过；离去。通り過ぎる\",\"date\":\"2024/5/12\"},{\"id\":\"96\",\"word\":\"行く\",\"kana\":\"いく\",\"type\":\"自动词・五段/一类\",\"meanings\":\"1.行进，去。2.到，往，前往。3.进展，进行。4.满足，满意。5.上年纪。6.逝去，流逝。7.移至他处。8.到达。9.通往，通向。\",\"date\":\"2024/5/12\"},{\"id\":\"102\",\"word\":\"旨味\",\"kana\":\"うまみ\",\"type\":\"名词\",\"meanings\":\"1.美味，好吃。（食物のうまい味。また、うまい度合い。おいしさ。）2.有利的，满意的。（仕事・商売などで利益やもうけが多いというおもしろみ。）\",\"date\":\"2024/5/12\"},{\"id\":\"113\",\"word\":\"言う\",\"kana\":\"ゆう\",\"type\":\"自他・五段/一类\",\"meanings\":\"1.同：言（いう）。\",\"date\":\"2024/5/12\"},{\"id\":\"114\",\"word\":\"言う\",\"kana\":\"いう\",\"type\":\"自他・五段/一类\",\"meanings\":\"1.响，做响，吱声，发出声音或响动。2.言，云，说，讲，道。用语言将事实、想法表达出来。3.说，命令，指示。4.称为，叫做。\",\"date\":\"2024/5/12\"}]]";

        try {
            // 创建 ObjectMapper 对象
            ObjectMapper objectMapper = new ObjectMapper();

            // 解析 JSON 字符串
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // 获取第一个元素，即包含 "user" 的对象
            JsonNode userNode = jsonNode.get(0);

            // 提取 "user" 值
            String user = userNode.get("user").asText();

            System.out.println("User: " + user);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
