package com.ruoyi.project.github.task;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import com.ruoyi.common.storage.FileStorageService;
import com.ruoyi.project.github.domain.GithubTrending;
import com.ruoyi.project.github.service.IGithubTrendingService;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * GitHub README文件上传OSS定时任务
 * 功能：每天上午9点执行，查询readme文件为空的上榜仓库，获取README并上传到OSS
 * 
 * @author ruoyi-magic
 * @date 2025-12-01
 */
@Component
public class GithubReadmeUploadTask {

    private static final Logger log = LoggerFactory.getLogger(GithubReadmeUploadTask.class);

    /** GitHub API基础地址 */
    private static final String GITHUB_API_BASE = "https://api.github.com";
    
    /** 每批处理的最大数量 */
    private static final int BATCH_SIZE = 200;
    
    /** GitHub API请求间隔(毫秒) */
    private static final long API_INTERVAL = 1000;

    @Autowired
    private IGithubTrendingService githubTrendingService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Value("${github.token:}")
    private String githubToken;

    /**
     * 执行README上传任务（每天上午9点执行）
     * cron表达式：0 0 9 * * ?
     * 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void execute() {
        log.info("========== 开始执行GitHub README上传OSS定时任务 ==========");
        long startTime = System.currentTimeMillis();
        
        int successCount = 0;
        int failCount = 0;
        int totalCount = 0;

        try {
            // 查询readme_path为空的上榜仓库
            QueryWrapper queryWrapper = QueryWrapper.create()
                .where(new QueryColumn("readme_path").isNull()
                    .or(new QueryColumn("readme_path").eq("")))
                .orderBy("stars_count", false)  // 按star数降序
                .limit(BATCH_SIZE);
            
            List<GithubTrending> emptyReadmeRepos = githubTrendingService.list(queryWrapper);
            totalCount = emptyReadmeRepos.size();
            
            log.info("查询到 {} 个README文件为空的仓库", totalCount);
            
            if (totalCount == 0) {
                log.info("没有需要处理的仓库，任务结束");
                return;
            }
            
            // 遍历处理每个仓库
            for (int i = 0; i < emptyReadmeRepos.size(); i++) {
                GithubTrending repo = emptyReadmeRepos.get(i);
                
                try {
                    log.info("处理仓库 [{}/{}]: {}/{}", 
                        i + 1, totalCount, repo.getOwner(), repo.getTitle());
                    
                    // 获取并上传README
                    boolean result = fetchAndUploadReadme(repo);
                    
                    if (result) {
                        successCount++;
                        log.info("仓库 {}/{} README上传成功", repo.getOwner(), repo.getTitle());
                    } else {
                        failCount++;
                        log.warn("仓库 {}/{} README上传失败", repo.getOwner(), repo.getTitle());
                    }
                    
                    // API调用间隔，避免触发GitHub限流
                    if (i < emptyReadmeRepos.size() - 1) {
                        Thread.sleep(API_INTERVAL);
                    }
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("处理仓库 {}/{} 时发生异常: {}", 
                        repo.getOwner(), repo.getTitle(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("执行GitHub README上传任务时发生异常", e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("========== GitHub README上传OSS定时任务执行完成 ==========");
            log.info("总数: {}, 成功: {}, 失败: {}, 耗时: {}ms", 
                totalCount, successCount, failCount, duration);
        }
    }

    /**
     * 获取README文件并上传到OSS
     * 
     * @param repo 仓库信息
     * @return 是否成功
     */
    private boolean fetchAndUploadReadme(GithubTrending repo) {
        String owner = repo.getOwner();
        String repoName = repo.getTitle();
        
        if (StrUtil.isBlank(owner) || StrUtil.isBlank(repoName)) {
            log.warn("仓库所有者或名称为空，跳过处理");
            return false;
        }
        
        // 1. 调用GitHub API获取README信息
        String apiUrl = String.format("%s/repos/%s/%s/readme", GITHUB_API_BASE, owner, repoName);
        
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "RuoyiMagic/1.0")
            .GET();
        
        // 如果配置了GitHub Token，添加认证头
        if (StrUtil.isNotBlank(githubToken)) {
            requestBuilder.header("Authorization", "token " + githubToken);
        }
        
        HttpRequest request = requestBuilder.build();
        HttpResponse<String> response;
        
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            log.error("调用GitHub API失败: {}", e.getMessage());
            return false;
        }
        
        if (response.statusCode() == 200) {
            // 解析响应
            JSONObject jsonResponse = JSONUtil.parseObj(response.body());
            String content = jsonResponse.getStr("content");
            String encoding = jsonResponse.getStr("encoding");
            String fileName = jsonResponse.getStr("name", "README.md");
            
            if (StrUtil.isBlank(content)) {
                log.warn("README内容为空");
                return false;
            }
            
            // 2. 解码README内容
            byte[] decodedContent;
            if ("base64".equals(encoding)) {
                decodedContent = Base64.getDecoder().decode(content.replaceAll("\\s", ""));
            } else {
                log.warn("不支持的编码格式: {}", encoding);
                return false;
            }
            
            // 3. 上传到OSS
            String ossPath = uploadToOss(owner, repoName, fileName, decodedContent);
            
            if (StrUtil.isNotBlank(ossPath)) {
                // 4. 更新数据库
                repo.setReadmePath(ossPath);
                repo.setReadmeUpdatedAt(new Date());
                repo.setUpdateAt(new Date());
                boolean updated = githubTrendingService.updateById(repo);
                
                if (updated) {
                    log.info("README文件上传成功，OSS路径: {}", ossPath);
                    return true;
                } else {
                    log.error("更新数据库失败");
                    return false;
                }
            } else {
                log.error("上传OSS失败");
                return false;
            }
            
        } else if (response.statusCode() == 404) {
            log.warn("仓库 {}/{} 未找到README文件", owner, repoName);
            return false;
        } else if (response.statusCode() == 403) {
            log.error("GitHub API访问被拒绝，可能是限流或权限不足，状态码: {}", response.statusCode());
            return false;
        } else {
            log.error("GitHub API请求失败，状态码: {}, 响应: {}", 
                response.statusCode(), response.body());
            return false;
        }
    }

    /**
     * 上传文件到OSS
     * 
     * @param owner 仓库所有者
     * @param repoName 仓库名称
     * @param fileName 文件名
     * @param content 文件内容
     * @return OSS文件路径
     */
    private String uploadToOss(String owner, String repoName, String fileName, byte[] content) {
        // 构建OSS存储路径: github-readme/owner/repo/README.md
        String basePath = String.format("github-readme/%s/%s", owner, repoName);
        
        // 获取文件扩展名
        String extension = FileNameUtil.extName(fileName);
        if (StrUtil.isBlank(extension)) {
            extension = "md";
        }
        
        // 构建完整文件名
        String fullFileName = String.format("%s/%s", basePath, fileName);
        
        try {
            // 创建自定义MultipartFile对象
            ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                fileName,
                fileName,
                "text/markdown",
                content
            );
            
            // 使用FileStorageService上传
            String ossUrl = fileStorageService.upload(multipartFile, fullFileName);
            
            if (StrUtil.isNotBlank(ossUrl)) {
                log.info("文件上传成功: {}", ossUrl);
            } else {
                log.error("文件上传失败");
            }
            
            return ossUrl;
        } catch (Exception e) {
            log.error("上传文件到OSS时发生异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 自定义MultipartFile实现，用于将字节数组转换为MultipartFile
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            throw new UnsupportedOperationException("transferTo not supported");
        }
    }
}
