package org.example.shop.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.UUID;

@Component
@ConfigurationProperties(prefix = "aliyun.oss")
@Data
public class OssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String domain;

    /**
     * 上传文件到OSS
     * @param inputStream 文件输入流
     * @param originalFileName 原始文件名
     * @return 文件访问URL
     */
    public String uploadFile(InputStream inputStream, String originalFileName) {
        OSS ossClient = null;
        try {
            // 创建OSS客户端
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

            // 生成文件路径：使用时间戳 + UUID + 文件后缀
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = "shop/" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "") + fileExtension;

            // 上传文件
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream);
            ossClient.putObject(putObjectRequest);

            // 构建文件访问URL
            // 如果配置了domain则使用domain，否则自动构建
            String fileUrl;
            if (domain != null && !domain.trim().isEmpty()) {
                // 使用配置的domain（可以是自定义域名）
                fileUrl = domain.endsWith("/") ? domain + fileName : domain + "/" + fileName;
            } else {
                // 自动构建：https://{bucketName}.{endpoint}/{fileName}
                fileUrl = "https://" + bucketName + "." + endpoint + "/" + fileName;
            }
            
            return fileUrl;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
}

