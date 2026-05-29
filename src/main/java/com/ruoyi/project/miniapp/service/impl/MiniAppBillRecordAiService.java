package com.ruoyi.project.miniapp.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.project.ai.domain.AiModelConfig;
import com.ruoyi.project.ai.domain.AiModelRoute;
import com.ruoyi.project.ai.service.IAiModelConfigService;
import com.ruoyi.project.ai.service.IAiModelRouteService;
import com.ruoyi.project.ai.service.impl.LangChain4jAgentService;
import com.ruoyi.project.bill.domain.BillAccount;
import com.ruoyi.project.bill.domain.BillCategory;
import com.ruoyi.project.bill.domain.BillRecord;
import com.ruoyi.project.bill.domain.BillUserProfile;
import com.ruoyi.project.bill.service.IBillAccountService;
import com.ruoyi.project.bill.service.IBillCategoryService;
import com.ruoyi.project.bill.service.IBillRecordService;
import com.ruoyi.project.bill.service.IBillUserProfileService;
import com.ruoyi.project.miniapp.domain.dto.BillRecordAnalyzeRequest;
import com.ruoyi.project.miniapp.domain.vo.BillRecordAnalyzeBatchResult;
import com.ruoyi.project.miniapp.domain.vo.BillRecordAnalyzeResult;
import com.ruoyi.project.miniapp.util.BillImageOptimizer;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MiniAppBillRecordAiService {

    private static final String PRODUCT_TYPE = "miniapp";
    private static final String SCENE_CODE = "bill_record_analyze";
    private static final String IMAGE_SCENE_CODE = "bill_record_image_analyze";
    private static final String[] QWEN_VISION_MODEL_CANDIDATES = {
            "qwen3.6-plus",
            "qwen3.5-plus",
            "qwen-vl-plus",
            "qwen3-vl-plus",
            "qwen-vl-max"
    };
    private static final String[] BILL_KEYWORDS = {
            "早饭", "早餐", "午饭", "午餐", "晚饭", "晚餐", "夜宵", "加班餐",
            "咖啡", "奶茶", "外卖", "打车", "滴滴", "地铁", "公交", "通勤",
            "工资", "奖金", "房租", "水电", "话费", "电影", "购物", "加油"
    };
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LangChain4jAgentService langChain4jAgentService;
    private final IAiModelRouteService modelRouteService;
    private final IAiModelConfigService modelConfigService;
    private final IBillCategoryService billCategoryService;
    private final IBillAccountService billAccountService;
    private final IBillUserProfileService billUserProfileService;
    private final IBillRecordService billRecordService;

    public MiniAppBillRecordAiService(LangChain4jAgentService langChain4jAgentService,
            IAiModelRouteService modelRouteService,
            IAiModelConfigService modelConfigService,
            IBillCategoryService billCategoryService,
            IBillAccountService billAccountService,
            IBillUserProfileService billUserProfileService,
            IBillRecordService billRecordService) {
        this.langChain4jAgentService = langChain4jAgentService;
        this.modelRouteService = modelRouteService;
        this.modelConfigService = modelConfigService;
        this.billCategoryService = billCategoryService;
        this.billAccountService = billAccountService;
        this.billUserProfileService = billUserProfileService;
        this.billRecordService = billRecordService;
    }

    public BillRecordAnalyzeBatchResult analyze(Long userId, BillRecordAnalyzeRequest request) {
        String text = StrUtil.trim(request.getText());
        if (StrUtil.isBlank(text)) {
            throw new ServiceException("记账描述不能为空");
        }

        AnalyzeContext context = prepareAnalyzeContext(userId);
        AiModelConfig modelConfig = resolveTextModelConfig();
        if (modelConfig == null) {
            throw new ServiceException("未配置可用的 AI 模型，请联系管理员");
        }

        String systemPrompt = buildSystemPrompt(context.today(), context.expenseCategories(),
                context.incomeCategories(), context.accounts(), false);
        String aiResponse = langChain4jAgentService.chatWithSystem(modelConfig.getId(), systemPrompt, text);
        return buildBatchResult(userId, buildAnalyzeResults(context, parseAiJson(aiResponse), text), text);
    }

    public BillRecordAnalyzeBatchResult analyzeImage(Long userId, MultipartFile file, String remark) {
        long startMs = System.currentTimeMillis();
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请上传图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ServiceException("图片大小不能超过5MB");
        }

        AnalyzeContext context = prepareAnalyzeContext(userId);
        AiModelConfig modelConfig = resolveVisionModelConfig();
        if (modelConfig == null) {
            throw new ServiceException("未配置可用的图像识别模型（千问 qwen3.5-plus / qwen3.6-plus），请联系管理员");
        }

        String mimeType = resolveImageMimeType(file);
        BillImageOptimizer.OptimizedImage optimizedImage;
        try {
            optimizedImage = BillImageOptimizer.optimize(file.getBytes(), mimeType);
        } catch (Exception e) {
            log.warn("图片预处理失败，回退原图: {}", e.getMessage());
            try {
                optimizedImage = new BillImageOptimizer.OptimizedImage(
                        file.getBytes(), mimeType, 0, 0, false);
            } catch (Exception ex) {
                throw new ServiceException("读取图片失败");
            }
        }

        String imageBase64 = Base64.getEncoder().encodeToString(optimizedImage.getBytes());
        log.info("账单图片预处理: originalSize={}B, optimizedSize={}B, mimeType={}, resized={}, costMs={}",
                file.getSize(),
                optimizedImage.getBytes().length,
                optimizedImage.getMimeType(),
                optimizedImage.isResized(),
                System.currentTimeMillis() - startMs);

        String systemPrompt = buildImageAnalyzePrompt(context.today(), context.expenseCategories(),
                context.incomeCategories(), context.accounts());
        String userText = StrUtil.blankToDefault(remark, "识别支付截图/小票/发票，提取金额、分类、日期");
        String aiResponse = langChain4jAgentService.chatWithSystemAndImageFast(
                modelConfig.getId(), systemPrompt, imageBase64, optimizedImage.getMimeType(), userText);
        String rawText = StrUtil.blankToDefault(remark, "图片识别记账");
        return buildBatchResult(userId, buildAnalyzeResults(context, parseAiJson(aiResponse), rawText), rawText);
    }

    private AnalyzeContext prepareAnalyzeContext(Long userId) {
        BillUserProfile userProfile = billUserProfileService.selectByMiniUserId(userId);
        List<BillCategory> expenseCategories = billCategoryService.selectCategoryTree("0");
        List<BillCategory> incomeCategories = billCategoryService.selectCategoryTree("1");
        List<BillAccount> accounts = billAccountService.selectByUserId(userId);
        if (accounts.isEmpty()) {
            throw new ServiceException("请先创建记账账户");
        }
        return new AnalyzeContext(userProfile, expenseCategories, incomeCategories, accounts,
                LocalDate.now().format(DATE_FORMATTER));
    }

    private BillRecordAnalyzeResult buildAnalyzeResult(AnalyzeContext context, JsonNode parsed, String rawText) {
        String recordType = normalizeRecordType(parsed.path("recordType").asText("0"));
        BigDecimal amount = parseAmount(parsed.path("amount").asText(null));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("未能识别有效金额，请补充金额信息后重试");
        }

        String categoryName = StrUtil.blankToDefault(parsed.path("categoryName").asText(null), "其他");
        BillCategory category = matchCategory(recordType, categoryName, context.expenseCategories(),
                context.incomeCategories());

        String accountHint = parsed.path("accountHint").asText(null);
        BillAccount account = matchAccount(accountHint, context.accounts(), context.userProfile());

        String recordDate = resolveRecordDate(parsed.path("recordDate").asText(null), context.today());
        String remark = StrUtil.blankToDefault(parsed.path("remark").asText(null), rawText);

        BillRecordAnalyzeResult result = new BillRecordAnalyzeResult();
        result.setRecordType(recordType);
        result.setAmount(amount);
        result.setCategoryId(category.getCategoryId());
        result.setCategoryName(category.getCategoryName());
        result.setCategoryIcon(category.getIcon());
        result.setAccountId(account.getAccountId());
        result.setAccountName(account.getAccountName());
        result.setRecordDate(recordDate);
        result.setRemark(remark);
        result.setRawText(rawText);
        return result;
    }

    private List<BillRecordAnalyzeResult> buildAnalyzeResults(AnalyzeContext context, JsonNode root, String rawText) {
        List<JsonNode> nodes = extractRecordNodes(root);
        List<BillRecordAnalyzeResult> results = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            JsonNode node = nodes.get(i);
            try {
                String itemRawText = nodes.size() == 1 ? rawText : rawText + " #" + (i + 1);
                results.add(buildAnalyzeResult(context, node, itemRawText));
            } catch (ServiceException e) {
                log.warn("跳过无效记账项 index={}, reason={}", i, e.getMessage());
            }
        }
        if (results.isEmpty()) {
            throw new ServiceException("未能识别有效账单，请补充信息后重试");
        }
        return results;
    }

    private List<JsonNode> extractRecordNodes(JsonNode root) {
        if (root == null || root.isNull()) {
            throw new ServiceException("AI 未返回有效结果");
        }
        if (root.isArray()) {
            List<JsonNode> nodes = new ArrayList<>();
            root.forEach(nodes::add);
            if (nodes.isEmpty()) {
                throw new ServiceException("AI 未返回有效账单");
            }
            return nodes;
        }
        if (root.has("records") && root.get("records").isArray()) {
            List<JsonNode> nodes = new ArrayList<>();
            root.get("records").forEach(nodes::add);
            if (nodes.isEmpty()) {
                throw new ServiceException("AI 未返回有效账单");
            }
            return nodes;
        }
        return List.of(root);
    }

    private BillRecordAnalyzeBatchResult buildBatchResult(Long userId, List<BillRecordAnalyzeResult> results,
            String rawText) {
        for (BillRecordAnalyzeResult result : results) {
            attachDuplicateInfo(userId, result);
        }
        BillRecordAnalyzeBatchResult batch = new BillRecordAnalyzeBatchResult();
        batch.setRecords(results);
        batch.setRawText(rawText);
        batch.setTotal(results.size());
        return batch;
    }

    private BillRecordAnalyzeResult attachDuplicateInfo(Long userId, BillRecordAnalyzeResult result) {
        BillRecord duplicate = findDuplicateRecord(userId, result);
        if (duplicate == null) {
            result.setDuplicateDetected(false);
            return result;
        }

        result.setDuplicateDetected(true);
        result.setDuplicateRecordId(duplicate.getRecordId());
        result.setDuplicateAmount(duplicate.getAmount());
        result.setDuplicateRemark(duplicate.getRemark());
        result.setDuplicateHint(buildDuplicateHint(duplicate, result));
        return result;
    }

    private BillRecord findDuplicateRecord(Long userId, BillRecordAnalyzeResult result) {
        // 仅对比 AI 解析出的单个记账日：未指定日期时为当天，指定了则为那一天，不做跨天范围查询
        LocalDate compareDate = parseRecordDate(result.getRecordDate());
        if (compareDate == null) {
            return null;
        }

        QueryWrapper qw = QueryWrapper.create()
                .from("bill_record")
                .eq("user_id", userId)
                .eq("record_date", compareDate)
                .eq("record_type", result.getRecordType())
                .orderBy("create_time", false);
        List<BillRecord> existingRecords = billRecordService.list(qw);
        if (existingRecords.isEmpty()) {
            return null;
        }

        for (BillRecord existing : existingRecords) {
            if (isDuplicate(existing, result)) {
                return existing;
            }
        }
        return null;
    }

    private boolean isDuplicate(BillRecord existing, BillRecordAnalyzeResult result) {
        boolean sameCategory = Objects.equals(existing.getCategoryId(), result.getCategoryId());
        boolean sameAmount = amountEquals(existing.getAmount(), result.getAmount());
        boolean textSimilar = textSimilar(existing, result);

        if (sameCategory && sameAmount) {
            return true;
        }
        if (sameCategory && textSimilar) {
            return true;
        }
        return sameAmount && textSimilar;
    }

    private boolean amountEquals(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return false;
        }
        return left.subtract(right).abs().compareTo(AMOUNT_TOLERANCE) <= 0;
    }

    private boolean textSimilar(BillRecord existing, BillRecordAnalyzeResult result) {
        String inputText = joinText(result.getRawText(), result.getRemark(), result.getCategoryName());
        String existingText = joinText(existing.getRemark(), result.getCategoryName());
        if (StrUtil.isBlank(inputText) || StrUtil.isBlank(existingText)) {
            return false;
        }

        for (String keyword : BILL_KEYWORDS) {
            if (inputText.contains(keyword) && existingText.contains(keyword)) {
                return true;
            }
        }

        String sharedKeyword = findKeyword(inputText);
        if (StrUtil.isNotBlank(sharedKeyword) && existingText.contains(sharedKeyword)) {
            return true;
        }

        if (StrUtil.length(existing.getRemark()) >= 2
                && (inputText.contains(existing.getRemark()) || existingText.contains(result.getRemark()))) {
            return true;
        }
        return false;
    }

    private String buildDuplicateHint(BillRecord existing, BillRecordAnalyzeResult result) {
        String keyword = findKeyword(joinText(result.getRawText(), result.getRemark()));
        if (StrUtil.isBlank(keyword)) {
            keyword = findKeyword(existing.getRemark());
        }
        if (StrUtil.isNotBlank(keyword)) {
            return keyword + "已记录";
        }
        if (StrUtil.isNotBlank(existing.getRemark())) {
            return "「" + existing.getRemark() + "」已记录";
        }
        return "「" + result.getCategoryName() + " ¥" + formatAmount(existing.getAmount()) + "」已记录";
    }

    private String findKeyword(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        return Arrays.stream(BILL_KEYWORDS)
                .filter(text::contains)
                .findFirst()
                .orElse(null);
    }

    private String joinText(String... parts) {
        return Arrays.stream(parts)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(" "));
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return amount.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private record AnalyzeContext(
            BillUserProfile userProfile,
            List<BillCategory> expenseCategories,
            List<BillCategory> incomeCategories,
            List<BillAccount> accounts,
            String today) {
    }

    private String buildSystemPrompt(String today, List<BillCategory> expenseCategories,
            List<BillCategory> incomeCategories, List<BillAccount> accounts, boolean imageMode) {
        String expenseList = formatCategoryList(expenseCategories);
        String incomeList = formatCategoryList(incomeCategories);
        String accountList = accounts.stream()
                .map(item -> item.getAccountName() + "(" + accountTypeLabel(item.getAccountType()) + ")")
                .collect(Collectors.joining("、"));
        String sourceHint = imageMode
                ? "请仔细识别图片中的支付截图、小票、发票、账单详情、银行流水等内容。"
                : "请根据用户的自然语言描述，解析出记账信息。";
        return "你是个人记账助手。" + sourceHint
                + "今天日期是 " + today + "。"
                + "仅返回 JSON，不要 markdown，不要解释。"
                + "格式：{\"records\":[{\"recordType\",\"amount\",\"categoryName\",\"recordDate\",\"remark\",\"accountHint\"}, ...]}。"
                + "用户描述可能包含多笔账单，每笔对应 records 一项；只有一笔时也放在 records 数组里。"
                + "字段：recordType(0支出/1收入)、amount(数字)、categoryName(分类名)、recordDate(yyyy-MM-dd，单天日期)、remark(简短备注)、accountHint(账户名或支付方式，如微信/支付宝/现金)。"
                + "规则：1. 花了/买了/支付/消费/支出/外卖/打车等判为支出(recordType=0)；"
                + "2. 收到/工资/奖金/退款/入账/收入等判为收入(recordType=1)；"
                + "3. categoryName 必须从给定分类中选择最匹配的一项；"
                + "4. 金额只输出数字，不含货币符号；"
                + "5. recordDate 只能是单个自然日：用户未提日期时必须输出今天 " + today + "；"
                + "用户提到昨天/前天/具体日期时，换算成对应的 yyyy-MM-dd；不要输出日期范围。"
                + "支出分类：" + expenseList + "。"
                + "收入分类：" + incomeList + "。"
                + "可选账户：" + accountList + "。";
    }

    private String buildImageAnalyzePrompt(String today, List<BillCategory> expenseCategories,
            List<BillCategory> incomeCategories, List<BillAccount> accounts) {
        String expenseList = formatCategoryList(expenseCategories);
        String incomeList = formatCategoryList(incomeCategories);
        String accountList = accounts.stream()
                .map(BillAccount::getAccountName)
                .collect(Collectors.joining("、"));
        return "你是账单 OCR 助手。识别图片中的支付截图、小票、发票或流水。"
                + "今天 " + today + "。只返回 JSON，不要 markdown，不要解释，不要思考过程。"
                + "格式：{\"records\":[...]}，图片里有多笔交易时每笔一项，只有一笔也放在 records 里。"
                + "每项字段：recordType(0支出/1收入)、amount(数字)、categoryName、recordDate(yyyy-MM-dd)、remark、accountHint。"
                + "未看到日期时用 " + today + "；看到相对日期要换算成单天 yyyy-MM-dd。"
                + "categoryName 只能从以下选择：支出[" + expenseList + "]；收入[" + incomeList + "]。"
                + "账户参考：" + accountList + "。";
    }

    private String formatCategoryList(List<BillCategory> categories) {
        return categories.stream()
                .map(BillCategory::getCategoryName)
                .distinct()
                .collect(Collectors.joining("、"));
    }

    private JsonNode parseAiJson(String aiResponse) {
        try {
            String json = extractJson(aiResponse);
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            log.warn("AI 记账解析失败, response={}", aiResponse, e);
            throw new ServiceException("AI 解析失败，请换个说法再试");
        }
    }

    private String extractJson(String text) {
        if (StrUtil.isBlank(text)) {
            throw new ServiceException("AI 未返回有效结果");
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int arrayStart = trimmed.indexOf('[');
            int arrayEnd = trimmed.lastIndexOf(']');
            int objectStart = trimmed.indexOf('{');
            int objectEnd = trimmed.lastIndexOf('}');
            if (arrayStart >= 0 && arrayEnd > arrayStart
                    && (objectStart < 0 || arrayStart < objectStart)) {
                return trimmed.substring(arrayStart, arrayEnd + 1);
            }
            if (objectStart >= 0 && objectEnd > objectStart) {
                return trimmed.substring(objectStart, objectEnd + 1);
            }
        }
        int arrayStart = trimmed.indexOf('[');
        int arrayEnd = trimmed.lastIndexOf(']');
        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (arrayStart >= 0 && arrayEnd > arrayStart
                && (objectStart < 0 || arrayStart < objectStart)) {
            return trimmed.substring(arrayStart, arrayEnd + 1);
        }
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        return trimmed;
    }

    private String normalizeRecordType(String recordType) {
        return "1".equals(recordType) ? "1" : "0";
    }

    private BigDecimal parseAmount(String amountText) {
        if (StrUtil.isBlank(amountText)) {
            return null;
        }
        String normalized = amountText.replaceAll("[^0-9.]", "");
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BillCategory matchCategory(String recordType, String categoryName,
            List<BillCategory> expenseCategories, List<BillCategory> incomeCategories) {
        List<BillCategory> candidates = "1".equals(recordType) ? incomeCategories : expenseCategories;
        if (candidates.isEmpty()) {
            throw new ServiceException("暂无可用分类，请联系管理员");
        }

        String target = StrUtil.blankToDefault(categoryName, "其他");
        BillCategory exact = candidates.stream()
                .filter(item -> target.equals(item.getCategoryName()))
                .findFirst()
                .orElse(null);
        if (exact != null) {
            return exact;
        }

        BillCategory contains = candidates.stream()
                .filter(item -> item.getCategoryName().contains(target) || target.contains(item.getCategoryName()))
                .findFirst()
                .orElse(null);
        if (contains != null) {
            return contains;
        }

        BillCategory fallback = candidates.stream()
                .filter(item -> "其他".equals(item.getCategoryName()))
                .findFirst()
                .orElse(candidates.get(0));
        return fallback;
    }

    private BillAccount matchAccount(String accountHint, List<BillAccount> accounts, BillUserProfile userProfile) {
        if (StrUtil.isNotBlank(accountHint)) {
            String hint = accountHint.toLowerCase(Locale.ROOT);
            BillAccount byName = accounts.stream()
                    .filter(item -> StrUtil.containsIgnoreCase(item.getAccountName(), accountHint))
                    .findFirst()
                    .orElse(null);
            if (byName != null) {
                return byName;
            }

            String accountType = inferAccountType(hint);
            if (accountType != null) {
                BillAccount byType = accounts.stream()
                        .filter(item -> accountType.equals(item.getAccountType()))
                        .findFirst()
                        .orElse(null);
                if (byType != null) {
                    return byType;
                }
            }
        }

        if (userProfile != null && userProfile.getDefaultAccountId() != null) {
            BillAccount defaultAccount = accounts.stream()
                    .filter(item -> userProfile.getDefaultAccountId().equals(item.getAccountId()))
                    .findFirst()
                    .orElse(null);
            if (defaultAccount != null) {
                return defaultAccount;
            }
        }
        return accounts.get(0);
    }

    private String inferAccountType(String hint) {
        if (hint.contains("微信")) {
            return "1";
        }
        if (hint.contains("支付宝")) {
            return "2";
        }
        if (hint.contains("信用卡")) {
            return "4";
        }
        if (hint.contains("银行卡") || hint.contains("储蓄")) {
            return "3";
        }
        if (hint.contains("现金")) {
            return "0";
        }
        return null;
    }

    private String accountTypeLabel(String accountType) {
        return switch (StrUtil.blankToDefault(accountType, "5")) {
            case "0" -> "现金";
            case "1" -> "微信";
            case "2" -> "支付宝";
            case "3" -> "银行卡";
            case "4" -> "信用卡";
            default -> "其他";
        };
    }

    private String resolveRecordDate(String aiDate, String today) {
        if (StrUtil.isBlank(aiDate)) {
            return today;
        }
        try {
            return LocalDate.parse(aiDate.trim(), DATE_FORMATTER).format(DATE_FORMATTER);
        } catch (Exception e) {
            return today;
        }
    }

    private LocalDate parseRecordDate(String recordDate) {
        if (StrUtil.isBlank(recordDate)) {
            return null;
        }
        try {
            return LocalDate.parse(recordDate.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }

    private AiModelConfig resolveTextModelConfig() {
        AiModelRoute route = findRoute(SCENE_CODE);
        if (route != null && route.getPrimaryModelConfigId() != null) {
            AiModelConfig config = modelConfigService.getById(route.getPrimaryModelConfigId());
            if (config != null && "Y".equals(config.getEnabled()) && "0".equals(config.getStatus())) {
                return config;
            }
        }

        AiModelConfig fallback = modelConfigService.getEnabledByModel("deepseek-v4-flash");
        if (fallback != null) {
            return fallback;
        }

        List<AiModelConfig> configs = modelConfigService.listEnabledByProviderAndCapability("deepseek", "chat");
        return configs.isEmpty() ? null : configs.get(0);
    }

    private AiModelConfig resolveVisionModelConfig() {
        AiModelRoute route = findRoute(IMAGE_SCENE_CODE);
        if (route != null && route.getPrimaryModelConfigId() != null) {
            AiModelConfig config = modelConfigService.getById(route.getPrimaryModelConfigId());
            if (config != null && "Y".equals(config.getEnabled()) && "0".equals(config.getStatus())) {
                return config;
            }
        }

        for (String modelName : QWEN_VISION_MODEL_CANDIDATES) {
            AiModelConfig config = modelConfigService.getEnabledByModel(modelName);
            if (config != null) {
                return config;
            }
        }

        AiModelConfig defaultQwen = modelConfigService.getDefaultByProviderAndCapability("qianwen", "chat");
        if (defaultQwen != null) {
            return defaultQwen;
        }

        List<AiModelConfig> visionConfigs = modelConfigService.listEnabledByProviderAndCapability("qianwen", "vision");
        if (!visionConfigs.isEmpty()) {
            return visionConfigs.get(0);
        }

        List<AiModelConfig> qwenChatConfigs = modelConfigService.listEnabledByProviderAndCapability("qianwen", "chat");
        return qwenChatConfigs.isEmpty() ? null : qwenChatConfigs.get(0);
    }

    private String resolveImageMimeType(MultipartFile file) {
        String contentType = StrUtil.blankToDefault(file.getContentType(), "");
        if (contentType.startsWith("image/")) {
            return contentType;
        }
        String filename = StrUtil.blankToDefault(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        if (filename.endsWith(".png")) {
            return "image/png";
        }
        if (filename.endsWith(".webp")) {
            return "image/webp";
        }
        if (filename.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }

    private AiModelRoute findRoute(String sceneCode) {
        QueryWrapper qw = QueryWrapper.create()
                .from("ai_model_route")
                .where("product_type = ?", PRODUCT_TYPE)
                .and("scene_code = ?", sceneCode)
                .and("enabled = 'Y'")
                .and("del_flag = '0'")
                .limit(1);
        return modelRouteService.getOne(qw);
    }
}
