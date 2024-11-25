package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.utils.OssUtil.OssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/admin/common")
public class CommonController {

    @Autowired
    private OssUtil ossUtil;

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) throws Exception {
        String url = ossUtil.upload(file);
        return Result.success(url);
    }

}
