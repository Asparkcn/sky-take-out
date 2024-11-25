package com.sky.utils.OssUtil;

import org.springframework.web.multipart.MultipartFile;

public interface OssUtil {
    String upload(MultipartFile multipartFile) throws Exception;
}
