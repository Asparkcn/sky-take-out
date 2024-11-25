package com.sky.utils.OssUtil.impl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.sky.properties.AliOssProperties;
import com.sky.utils.OssUtil.OssUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sky.oss", name = "type", havingValue = "aliyun")
public class AliOssUtil implements OssUtil {

    private final AliOssProperties aliOssProperties;

    @Override
    public String upload(MultipartFile multipartFile) throws Exception {
        InputStream inputStream = multipartFile.getInputStream();

        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }
        String extFilename = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFilename = UUID.randomUUID() + extFilename;

        OSS ossClient = new OSSClientBuilder()
                .build(aliOssProperties.getEndpoint(), aliOssProperties.getAccessKeyId(), aliOssProperties.getAccessKeySecret());
        String url = null;
        try {
            ossClient.putObject(aliOssProperties.getBucketName(), "spark/" + newFilename, inputStream);
            url = aliOssProperties.getEndpoint().split("//")[0] + aliOssProperties.getBucketName() + "." + aliOssProperties.getEndpoint().split("//")[1] + "/spark/" + newFilename;
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        return url;
    }

}