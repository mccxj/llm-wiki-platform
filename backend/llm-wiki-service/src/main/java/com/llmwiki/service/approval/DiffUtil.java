package com.llmwiki.service.approval;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * 文本差异对比工具
 * 依赖: com.github.java-diff-utils:diffutils (需在pom.xml中添加)
 */
@Slf4j
public class DiffUtil {

    /**
     * 生成行级diff
     */
    public static String generateDiff(String before, String after) {
        if (before == null) before = "";
        if (after == null) after = "";

        List<String> beforeLines = Arrays.asList(before.split("\n"));
        List<String> afterLines = Arrays.asList(after.split("\n"));

        Patch<String> patch = DiffUtils.diff(beforeLines, afterLines);

        StringBuilder sb = new StringBuilder();
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            switch (delta.getType()) {
                case INSERT:
                    sb.append("+ ").append(delta.getTarget().getLines()).append("\n");
                    break;
                case DELETE:
                    sb.append("- ").append(delta.getSource().getLines()).append("\n");
                    break;
                case CHANGE:
                    sb.append("- ").append(delta.getSource().getLines()).append("\n");
                    sb.append("+ ").append(delta.getTarget().getLines()).append("\n");
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * 生成unified diff格式
     */
    public static String generateUnifiedDiff(String before, String after) {
        if (before == null) before = "";
        if (after == null) after = "";

        List<String> beforeLines = Arrays.asList(before.split("\n"));
        List<String> afterLines = Arrays.asList(after.split("\n"));

        Patch<String> patch = DiffUtils.diff(beforeLines, afterLines);
        List<String> unifiedDiff = com.github.difflib.UnifiedDiffUtils.generateUnifiedDiff(
                "before", "after", beforeLines, patch, 3);

        return String.join("\n", unifiedDiff);
    }

    /**
     * 生成HTML格式的diff（用于前端展示）
     */
    public static String generateHtmlDiff(String before, String after) {
        if (before == null) before = "";
        if (after == null) after = "";

        List<String> beforeLines = Arrays.asList(before.split("\n"));
        List<String> afterLines = Arrays.asList(after.split("\n"));

        Patch<String> patch = DiffUtils.diff(beforeLines, afterLines);

        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"diff\">");
        for (AbstractDelta<String> delta : patch.getDeltas()) {
            switch (delta.getType()) {
                case INSERT:
                    for (String line : delta.getTarget().getLines()) {
                        sb.append("<ins>").append(escapeHtml(line)).append("</ins><br>");
                    }
                    break;
                case DELETE:
                    for (String line : delta.getSource().getLines()) {
                        sb.append("<del>").append(escapeHtml(line)).append("</del><br>");
                    }
                    break;
                case CHANGE:
                    for (String line : delta.getSource().getLines()) {
                        sb.append("<del>").append(escapeHtml(line)).append("</del><br>");
                    }
                    for (String line : delta.getTarget().getLines()) {
                        sb.append("<ins>").append(escapeHtml(line)).append("</ins><br>");
                    }
                    break;
            }
        }
        sb.append("</div>");
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
